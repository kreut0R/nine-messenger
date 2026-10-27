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
 * Keeps the account in the app folder as three files:
 * - `vault.json`: the master key, encrypted with a key derived from the access code (Argon2id), and the failed-attempt counter;
 * - `identity.enc`: the sign-in key, encrypted with the master key;
 * - `messenger.enc`: the conversations, encrypted with the master key.
 *
 * The access code is stored nowhere. A correct code is recognized by the master key decrypting without an error.
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
        require(isAcceptableAccessCode(code)) { "The access code is shorter than $MinAccessCodeLength characters" }
        check(!hasAccount()) { "The device already has an account" }
        return setUp(Identity.generate(), code)
    }

    override fun unlock(code: String): UnlockResult {
        val passed = when (val check = check(code)) {
            is Check.Refused -> return check.result
            is Check.Passed -> check
        }
        val seed = try {
            Crypto.open(passed.masterKey, identityFile.readBytes(), IdentityPurpose)
        } catch (error: IOException) {
            null
        } ?: return UnlockResult.Damaged
        writeRecord(passed.record.copy(failures = 0, pausedUntilMillis = 0))
        val store = EncryptedMessengerStore(dataFile, passed.masterKey)
        return UnlockResult.Unlocked(DeviceSession(Identity(seed), passed.masterKey, store, passed.record.cost))
    }

    /**
     * Checks the code against the attempt limit and returns the master key or the reason for refusal.
     * The failure is recorded before the check: otherwise the app could be switched off in the middle of a check and the code guessed without a limit.
     */
    private fun check(code: String): Check {
        val record = readRecord() ?: return Check.Refused(UnlockResult.Damaged)
        val now = time.nowMillis()
        if (now < record.pausedUntilMillis) return Check.Refused(UnlockResult.Paused(record.pausedUntilMillis))

        val failures = record.failures + 1
        writeRecord(record.copy(failures = failures, pausedUntilMillis = now + Lockout.pauseAfter(failures)))

        val codeKey = Crypto.deriveKey(code, decode(record.salt), record.cost)
        val masterKey = Crypto.open(codeKey, decode(record.wrappedKey), MasterKeyPurpose)
        codeKey.fill(0)
        if (masterKey != null) return Check.Passed(masterKey, record)

        val pause = Lockout.pauseAfter(failures)
        val refusal = if (pause > 0) UnlockResult.Paused(now + pause) else UnlockResult.WrongCode(Lockout.FreeAttempts - failures)
        return Check.Refused(refusal)
    }

    override fun restoreAccount(backup: ByteArray, backupPassword: String, code: String): RestoreResult {
        require(isAcceptableAccessCode(code)) { "The access code is shorter than $MinAccessCodeLength characters" }
        val identity = when (val read = KeyBackup.read(backup, backupPassword)) {
            is KeyBackup.ReadResult.Opened -> read.identity
            KeyBackup.ReadResult.WrongPassword -> return RestoreResult.WrongBackupPassword
            KeyBackup.ReadResult.NotABackup -> return RestoreResult.NotABackup
        }
        // Erase only after the backup has opened: with a wrong password the current account stays untouched.
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
        // The record with the keys is written last: until it exists there is no account on the device, so an interrupted setup does no harm.
        writeRecord(record)
        return DeviceSession(identity, masterKey, store, cost)
    }

    /** The prototype kept conversations in a plain file. Move them into the encrypted store and erase the plain copy. */
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

    /** Erases the file and its temporary and set-aside copies. */
    private fun eraseFiles(name: String) {
        listOf(name, "$name.tmp", "$name.broken").map { File(directory, it) }.filter { it.exists() }.forEach(::erase)
    }

    /**
     * Before deleting, overwrite the contents with zeros. On SSDs and journaling file systems this does not guarantee
     * that the old data is physically gone, so rely on the encryption, not on the overwrite.
     */
    private fun erase(file: File) {
        try {
            file.writeBytes(ByteArray(file.length().toInt()))
        } catch (error: IOException) {
            // Could not overwrite, delete anyway.
        }
        file.delete()
    }

    private sealed interface Check {
        class Passed(val masterKey: ByteArray, val record: VaultRecord) : Check
        class Refused(val result: UnlockResult) : Check
    }

    private inner class DeviceSession(
        private val identity: Identity,
        private val masterKey: ByteArray,
        override val store: EncryptedMessengerStore,
        private val backupCost: Argon2Cost,
    ) : Session {
        override val accountId: AccountId = identity.accountId

        override fun exportKeyBackup(password: String): ByteArray = KeyBackup.create(identity, password, backupCost)

        override fun changeAccessCode(current: String, newCode: String): ChangeCodeResult {
            require(isAcceptableAccessCode(newCode)) { "The access code is shorter than $MinAccessCodeLength characters" }
            val passed = when (val check = check(current)) {
                is Check.Refused -> return when (val refusal = check.result) {
                    is UnlockResult.WrongCode -> ChangeCodeResult.WrongCode(refusal.attemptsBeforePause)
                    is UnlockResult.Paused -> ChangeCodeResult.Paused(refusal.untilMillis)
                    else -> ChangeCodeResult.Damaged
                }
                is Check.Passed -> check
            }
            val salt = Crypto.randomBytes(Crypto.SaltSize)
            val codeKey = Crypto.deriveKey(newCode, salt, cost)
            // The record is replaced whole in one atomic action: before it the old code applies, after it the new one.
            writeRecord(
                VaultRecord(
                    cost = cost,
                    salt = encode(salt),
                    wrappedKey = encode(Crypto.seal(codeKey, passed.masterKey, MasterKeyPurpose)),
                ),
            )
            codeKey.fill(0)
            passed.masterKey.fill(0)
            return ChangeCodeResult.Changed
        }

        override fun lock() {
            store.close()
            masterKey.fill(0)
            identity.seed.fill(0)
        }
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
