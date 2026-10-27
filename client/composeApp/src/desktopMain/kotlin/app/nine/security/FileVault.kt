package app.nine.security

import app.nine.data.FileMessengerStore
import app.nine.data.MessengerStore
import app.nine.data.writeFileAtomically
import app.nine.domain.AccountId
import app.nine.domain.TimeSource
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import java.util.Base64

/**
 * Хранит аккаунт в папке приложения тремя файлами:
 * - `vault.json`: главный ключ, зашифрованный ключом из кода доступа (Argon2id), и счётчик неудачных попыток;
 * - `identity.enc`: ключ входа, зашифрованный главным ключом;
 * - `messenger.enc`: переписка, зашифрованная главным ключом.
 *
 * Код доступа не хранится нигде. Верный код проверяется тем, что главный ключ расшифровывается без ошибки.
 */
class FileVault(
    private val directory: File,
    private val time: TimeSource = TimeSource.System,
    private val cost: Argon2Cost = Argon2Cost.Default,
) : Vault {
    private val vaultFile = File(directory, "vault.json")
    private val identityFile = File(directory, "identity.enc")
    private val dataFile = File(directory, "messenger.enc")

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override fun hasAccount(): Boolean = vaultFile.exists()

    override fun createAccount(code: String): Session {
        require(isAcceptableAccessCode(code)) { "Код доступа короче $MinAccessCodeLength символов" }
        check(!hasAccount()) { "На устройстве уже есть аккаунт" }
        return setUp(Identity.generate(), code)
    }

    override fun unlock(code: String): UnlockResult {
        val record = readRecord() ?: return UnlockResult.Damaged
        val now = time.nowMillis()
        if (now < record.pausedUntilMillis) return UnlockResult.Paused(record.pausedUntilMillis)

        // Неудачу записываем до проверки: иначе можно выключить приложение посреди проверки и перебирать код без лимита.
        val failures = record.failures + 1
        writeRecord(record.copy(failures = failures, pausedUntilMillis = now + Lockout.pauseAfter(failures)))

        val codeKey = Crypto.deriveKey(code, decode(record.salt), record.cost)
        val masterKey = Crypto.open(codeKey, decode(record.wrappedKey), MasterKeyPurpose)
        codeKey.fill(0)
        if (masterKey == null) {
            val pause = Lockout.pauseAfter(failures)
            return if (pause > 0) UnlockResult.Paused(now + pause) else UnlockResult.WrongCode(Lockout.FreeAttempts - failures)
        }

        val seed = try {
            Crypto.open(masterKey, identityFile.readBytes(), IdentityPurpose)
        } catch (error: IOException) {
            null
        } ?: return UnlockResult.Damaged
        writeRecord(record.copy(failures = 0, pausedUntilMillis = 0))
        return UnlockResult.Unlocked(DeviceSession(Identity(seed), masterKey, EncryptedMessengerStore(dataFile, masterKey), record.cost))
    }

    override fun restoreAccount(backup: ByteArray, backupPassword: String, code: String): RestoreResult {
        require(isAcceptableAccessCode(code)) { "Код доступа короче $MinAccessCodeLength символов" }
        val identity = when (val read = KeyBackup.read(backup, backupPassword)) {
            is KeyBackup.ReadResult.Opened -> read.identity
            KeyBackup.ReadResult.WrongPassword -> return RestoreResult.WrongBackupPassword
            KeyBackup.ReadResult.NotABackup -> return RestoreResult.NotABackup
        }
        // Стираем только после того, как копия открылась: с неверным паролем текущий аккаунт остаётся нетронутым.
        wipe()
        return RestoreResult.Restored(setUp(identity, code))
    }

    override fun wipe() {
        eraseAccountFiles()
        eraseFiles("messenger.json")
    }

    private fun setUp(identity: Identity, code: String): Session {
        eraseAccountFiles()
        val masterKey = Crypto.randomBytes(Crypto.KeySize)
        val salt = Crypto.randomBytes(Crypto.SaltSize)
        val codeKey = Crypto.deriveKey(code, salt, cost)
        val record = VaultRecord(
            cost = cost,
            salt = encode(salt),
            wrappedKey = encode(Crypto.seal(codeKey, masterKey, MasterKeyPurpose)),
        )
        codeKey.fill(0)

        writeFileAtomically(identityFile, Crypto.seal(masterKey, identity.seed, IdentityPurpose))
        val store = EncryptedMessengerStore(dataFile, masterKey)
        moveOpenDataInside(store)
        // Запись с ключами пишется последней: пока её нет, аккаунта на устройстве нет, и оборванная установка не страшна.
        writeRecord(record)
        return DeviceSession(identity, masterKey, store, cost)
    }

    /** Прототип хранил переписку открытым файлом. Переносим её в шифрованное хранилище и стираем открытую копию. */
    private fun moveOpenDataInside(store: MessengerStore) {
        if (!File(directory, "messenger.json").exists()) return
        store.save(FileMessengerStore(directory).load())
        eraseFiles("messenger.json")
    }

    private fun readRecord(): VaultRecord? = try {
        json.decodeFromString(VaultRecord.serializer(), vaultFile.readText())
    } catch (error: SerializationException) {
        null
    } catch (error: IOException) {
        null
    }

    private fun writeRecord(record: VaultRecord) {
        writeFileAtomically(vaultFile, json.encodeToString(VaultRecord.serializer(), record).toByteArray())
    }

    private fun eraseAccountFiles() {
        eraseFiles("vault.json")
        eraseFiles("identity.enc")
        eraseFiles("messenger.enc")
    }

    /** Стирает файл и его временные и отложенные копии. */
    private fun eraseFiles(name: String) {
        listOf(name, "$name.tmp", "$name.broken").map { File(directory, it) }.filter { it.exists() }.forEach(::erase)
    }

    /**
     * Перед удалением затираем содержимое нулями. На SSD и в журналируемых файловых системах это не гарантирует,
     * что старые данные исчезли физически, поэтому полагаться стоит на шифрование, а не на затирание.
     */
    private fun erase(file: File) {
        try {
            file.writeBytes(ByteArray(file.length().toInt()))
        } catch (error: IOException) {
            // Не получилось затереть, всё равно удаляем.
        }
        file.delete()
    }

    private companion object {
        const val MasterKeyPurpose = "nine:master-key"
        const val IdentityPurpose = "nine:identity"

        fun encode(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)
        fun decode(text: String): ByteArray = Base64.getDecoder().decode(text)
    }
}

@Serializable
private data class VaultRecord(
    val version: Int = 1,
    val cost: Argon2Cost,
    val salt: String,
    val wrappedKey: String,
    val failures: Int = 0,
    val pausedUntilMillis: Long = 0,
)

private class DeviceSession(
    private val identity: Identity,
    private val masterKey: ByteArray,
    override val store: MessengerStore,
    private val cost: Argon2Cost,
) : Session {
    override val accountId: AccountId = identity.accountId

    override fun exportKeyBackup(password: String): ByteArray = KeyBackup.create(identity, password, cost)

    override fun lock() {
        masterKey.fill(0)
        identity.seed.fill(0)
    }
}
