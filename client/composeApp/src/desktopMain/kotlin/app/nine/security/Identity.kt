package app.nine.security

import app.nine.domain.AccountId
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import java.security.MessageDigest

/**
 * The sign-in key: an Ed25519 pair. While there is no server, the key only defines the account.
 * Later it will sign the sign-in to the server instead of a password.
 */
internal class Identity(val seed: ByteArray) {
    init {
        require(seed.size == SeedSize) { "The sign-in key must be exactly $SeedSize bytes" }
    }

    private val publicKey: ByteArray = Ed25519PrivateKeyParameters(seed, 0).generatePublicKey().encoded

    val accountId = AccountId(MessageDigest.getInstance("SHA-256").digest(publicKey).toHex())

    companion object {
        const val SeedSize = 32

        fun generate() = Identity(Crypto.randomBytes(SeedSize))
    }
}

private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
