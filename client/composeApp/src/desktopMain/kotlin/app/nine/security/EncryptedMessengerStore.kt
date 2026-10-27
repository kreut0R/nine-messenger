package app.nine.security

import app.nine.data.MessengerStore
import app.nine.data.writeFileAtomically
import app.nine.domain.MessengerData
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/** The same conversation storage as the file one, but the file holds only ciphertext. The key comes from an open session. */
internal class EncryptedMessengerStore(private val file: File, private val masterKey: ByteArray) : MessengerStore {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /**
     * After a lock the key in memory is overwritten with zeros. If a write still got here (say, a key press at the very moment
     * the autolock fired), the file would be encrypted with a zero key, and the real key could no longer open it.
     */
    @Volatile
    private var closed = false

    fun close() {
        closed = true
    }

    override fun load(): MessengerData {
        if (closed || !file.exists()) return MessengerData()
        return try {
            val plain = Crypto.open(masterKey, file.readBytes(), Purpose)
            if (plain == null) {
                keepBrokenFile()
                MessengerData()
            } else {
                json.decodeFromString(MessengerData.serializer(), plain.decodeToString())
            }
        } catch (error: SerializationException) {
            keepBrokenFile()
            MessengerData()
        } catch (error: IOException) {
            keepBrokenFile()
            MessengerData()
        }
    }

    override fun save(data: MessengerData) {
        if (closed) return
        try {
            val plain = json.encodeToString(MessengerData.serializer(), data).toByteArray()
            writeFileAtomically(file, Crypto.seal(masterKey, plain, Purpose))
        } catch (error: IOException) {
            // As with the file store: the message stays on screen, the error is visible in the console.
            System.err.println("Nine: could not save data to $file: ${error.message}")
        }
    }

    /** A damaged file is not erased but set aside next to it: maybe it can still be recovered. */
    private fun keepBrokenFile() {
        val broken = File(file.parentFile, "${file.name}.broken")
        try {
            Files.move(file.toPath(), broken.toPath(), StandardCopyOption.REPLACE_EXISTING)
            System.err.println("Nine: the encrypted data cannot be read, the file was saved as $broken, starting from an empty state.")
        } catch (error: IOException) {
            System.err.println("Nine: the encrypted data cannot be read and could not be set aside: ${error.message}")
        }
    }

    private companion object {
        const val Purpose = "nine:messenger-data"
    }
}
