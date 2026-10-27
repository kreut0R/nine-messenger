package app.nine.security

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.util.Base64

/**
 * Копия ключа входа в файле. Шифруется отдельным паролем, не кодом доступа:
 * копию кладут вне устройства, и она должна открываться там, где кода уже нет.
 */
@Serializable
internal class KeyBackup(
    val version: Int = 1,
    val cost: Argon2Cost,
    val salt: String,
    val sealedKey: String,
) {
    companion object {
        private const val Purpose = "nine:key-backup"
        private val json = Json { ignoreUnknownKeys = true }

        fun create(identity: Identity, password: String, cost: Argon2Cost): ByteArray {
            val salt = Crypto.randomBytes(Crypto.SaltSize)
            val key = Crypto.deriveKey(password, salt, cost)
            val backup = KeyBackup(
                cost = cost,
                salt = Base64.getEncoder().encodeToString(salt),
                sealedKey = Base64.getEncoder().encodeToString(Crypto.seal(key, identity.seed, Purpose)),
            )
            key.fill(0)
            return json.encodeToString(serializer(), backup).toByteArray()
        }

        /** Читает копию. Если файл не похож на копию, возвращает [NotABackup], если пароль не тот, то [WrongPassword]. */
        fun read(bytes: ByteArray, password: String): ReadResult {
            val backup = try {
                json.decodeFromString(serializer(), bytes.decodeToString())
            } catch (error: SerializationException) {
                return ReadResult.NotABackup
            } catch (error: IllegalArgumentException) {
                return ReadResult.NotABackup
            }
            if (backup.version != 1) return ReadResult.NotABackup
            val sealed = try {
                Base64.getDecoder().decode(backup.sealedKey)
            } catch (error: IllegalArgumentException) {
                return ReadResult.NotABackup
            }
            val key = Crypto.deriveKey(password, Base64.getDecoder().decode(backup.salt), backup.cost)
            val seed = Crypto.open(key, sealed, Purpose)
            key.fill(0)
            return if (seed != null && seed.size == Identity.SeedSize) ReadResult.Opened(Identity(seed)) else ReadResult.WrongPassword
        }
    }

    sealed interface ReadResult {
        class Opened(val identity: Identity) : ReadResult
        data object WrongPassword : ReadResult
        data object NotABackup : ReadResult
    }
}
