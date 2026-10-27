package app.nine.security

import kotlinx.serialization.Serializable
import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters
import java.security.SecureRandom
import java.text.Normalizer
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/** Во сколько обходится одна проверка кода. Сохраняется рядом с данными, поэтому позже его можно будет повысить. */
@Serializable
data class Argon2Cost(val memoryKiB: Int, val iterations: Int, val parallelism: Int) {
    companion object {
        /** 64 МиБ и три прохода: около четверти секунды на обычном компьютере, перебор на видеокартах дорожает из-за памяти. */
        val Default = Argon2Cost(memoryKiB = 64 * 1024, iterations = 3, parallelism = 1)
    }
}

/**
 * Все примитивы шифрования в одном месте. Своих алгоритмов здесь нет: AES-256-GCM из JDK и Argon2id из BouncyCastle.
 *
 * Зашифрованный блок выглядит так: версия формата (1 байт), случайный nonce (12 байт), шифртекст с тегом подлинности.
 * Каждый раз берётся новый случайный nonce, при ключе на все записи это безопасно до миллиардов записей.
 */
internal object Crypto {
    private const val FormatVersion: Byte = 1
    private const val NonceSize = 12
    private const val TagBits = 128
    const val KeySize = 32
    const val SaltSize = 16

    private val random = SecureRandom()

    fun randomBytes(size: Int): ByteArray = ByteArray(size).also(random::nextBytes)

    /**
     * Шифрует [plaintext]. Строка [purpose] входит в проверку подлинности, но не в шифртекст:
     * блок, зашифрованный для одной цели, не откроется как блок для другой, даже с тем же ключом.
     */
    fun seal(key: ByteArray, plaintext: ByteArray, purpose: String): ByteArray {
        val nonce = randomBytes(NonceSize)
        val cipher = cipher(Cipher.ENCRYPT_MODE, key, nonce, purpose)
        return byteArrayOf(FormatVersion) + nonce + cipher.doFinal(plaintext)
    }

    /** Расшифровывает блок. Возвращает null, если ключ не тот или данные повреждены: отличить одно от другого нельзя. */
    fun open(key: ByteArray, sealed: ByteArray, purpose: String): ByteArray? {
        if (sealed.size < 1 + NonceSize + TagBits / 8 || sealed[0] != FormatVersion) return null
        val nonce = sealed.copyOfRange(1, 1 + NonceSize)
        return try {
            cipher(Cipher.DECRYPT_MODE, key, nonce, purpose).doFinal(sealed, 1 + NonceSize, sealed.size - 1 - NonceSize)
        } catch (error: AEADBadTagException) {
            null
        }
    }

    /** Превращает код доступа в ключ. Код приводится к одной форме записи, чтобы «é» из двух символов совпало с «é» из одного. */
    fun deriveKey(code: String, salt: ByteArray, cost: Argon2Cost): ByteArray {
        val parameters = Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
            .withVersion(Argon2Parameters.ARGON2_VERSION_13)
            .withSalt(salt)
            .withMemoryAsKB(cost.memoryKiB)
            .withIterations(cost.iterations)
            .withParallelism(cost.parallelism)
            .build()
        val key = ByteArray(KeySize)
        Argon2BytesGenerator().apply { init(parameters) }
            .generateBytes(Normalizer.normalize(code, Normalizer.Form.NFKC).toByteArray(), key)
        return key
    }

    private fun cipher(mode: Int, key: ByteArray, nonce: ByteArray, purpose: String): Cipher =
        Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(mode, SecretKeySpec(key, "AES"), GCMParameterSpec(TagBits, nonce))
            updateAAD(purpose.toByteArray())
        }
}
