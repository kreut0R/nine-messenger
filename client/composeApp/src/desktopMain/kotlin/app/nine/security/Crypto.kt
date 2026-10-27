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

/** The cost of one code check. Saved next to the data, so it can be raised later. */
@Serializable
data class Argon2Cost(val memoryKiB: Int, val iterations: Int, val parallelism: Int) {
    companion object {
        /** 64 MiB and three passes: about a quarter of a second on an ordinary computer; GPU brute force gets pricier because of the memory. */
        val Default = Argon2Cost(memoryKiB = 64 * 1024, iterations = 3, parallelism = 1)
    }
}

/**
 * All encryption primitives in one place. There are no home-made algorithms here: AES-256-GCM from the JDK and Argon2id from BouncyCastle.
 *
 * An encrypted block looks like this: format version (1 byte), random nonce (12 bytes), ciphertext with the authentication tag.
 * A fresh random nonce is taken every time; with one key for all records this is safe up to billions of records.
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
     * Encrypts [plaintext]. The [purpose] string is part of the authentication but not of the ciphertext:
     * a block encrypted for one purpose will not open as a block for another, even with the same key.
     */
    fun seal(key: ByteArray, plaintext: ByteArray, purpose: String): ByteArray {
        val nonce = randomBytes(NonceSize)
        val cipher = cipher(Cipher.ENCRYPT_MODE, key, nonce, purpose)
        return byteArrayOf(FormatVersion) + nonce + cipher.doFinal(plaintext)
    }

    /** Decrypts a block. Returns null if the key is wrong or the data is damaged: the two cannot be told apart. */
    fun open(key: ByteArray, sealed: ByteArray, purpose: String): ByteArray? {
        if (sealed.size < 1 + NonceSize + TagBits / 8 || sealed[0] != FormatVersion) return null
        val nonce = sealed.copyOfRange(1, 1 + NonceSize)
        return try {
            cipher(Cipher.DECRYPT_MODE, key, nonce, purpose).doFinal(sealed, 1 + NonceSize, sealed.size - 1 - NonceSize)
        } catch (error: AEADBadTagException) {
            null
        }
    }

    /** Turns an access code into a key. The code is normalized to one form, so "e" plus a combining accent matches the single character "é". */
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
