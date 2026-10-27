package app.nine.security

import app.nine.domain.AccountId
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import java.security.MessageDigest

/**
 * Ключ входа: пара Ed25519. Пока сервера нет, ключ только определяет аккаунт.
 * Позже им будут подписывать вход на сервер вместо пароля.
 */
internal class Identity(val seed: ByteArray) {
    init {
        require(seed.size == SeedSize) { "Ключ входа должен быть ровно $SeedSize байта" }
    }

    private val publicKey: ByteArray = Ed25519PrivateKeyParameters(seed, 0).generatePublicKey().encoded

    val accountId = AccountId(MessageDigest.getInstance("SHA-256").digest(publicKey).toHex())

    companion object {
        const val SeedSize = 32

        fun generate() = Identity(Crypto.randomBytes(SeedSize))
    }
}

private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
