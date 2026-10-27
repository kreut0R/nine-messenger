package app.nine.security

import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters
import org.bouncycastle.crypto.signers.Ed25519Signer
import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The test vectors from `docs/server-spec.md`. The Go server checks the same values, so a pass here
 * and a pass there mean the client and the server agree on the account id and on the signatures.
 */
class IdentityVectorTest {
    private val seed = ByteArray(32) { it.toByte() }
    private val publicKey = Base64.getUrlDecoder().decode("A6EHv_POEL4dcN0Y50vAmWfk1jCbpQ1fHdyGZBJVMbg")

    @Test
    fun accountIdMatchesTheSpecVector() {
        assertEquals("56475aa75463474c0285df5dbf2bcab73da651358839e9b77481b2eab107708c", Identity(seed).accountId.hex)
    }

    @Test
    fun registerSignatureFromTheSpecVerifies() {
        val message = "nine/v1/register\n56475aa75463474c0285df5dbf2bcab73da651358839e9b77481b2eab107708c\n7K3M-9QXA-2D5F-8HVN\n1793100000000"
        val signature = "RZUgB7fJXdZzY6qFFxpoF4y-6M34FvQKULhuQNPn23IIT3A3uSIqSmdAoFGvRy86nSwLwKWpa_y5BMjIoLZtAg"

        assertTrue(verify(message, signature))
    }

    @Test
    fun authSignatureFromTheSpecVerifies() {
        val message = "nine/v1/auth\n56475aa75463474c0285df5dbf2bcab73da651358839e9b77481b2eab107708c\noKGio6SlpqeoqaqrrK2ur7CxsrO0tba3uLm6u7y9vr8"
        val signature = "CTLRi7KcywBKwRwLkucdYAjA8s05LS70JrXG63MoVZ98uT4ZpU8_hg7nLqvovvlOJ6Y_2mdJvpWXF8is-h5pCA"

        assertTrue(verify(message, signature))
    }

    private fun verify(message: String, signatureB64u: String): Boolean {
        val verifier = Ed25519Signer().apply { init(false, Ed25519PublicKeyParameters(publicKey, 0)) }
        val bytes = message.toByteArray()
        verifier.update(bytes, 0, bytes.size)
        return verifier.verifySignature(Base64.getUrlDecoder().decode(signatureB64u))
    }
}
