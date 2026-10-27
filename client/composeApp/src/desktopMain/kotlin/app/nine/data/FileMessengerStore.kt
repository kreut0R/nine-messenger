package app.nine.data

import app.nine.domain.MessengerData
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Keeps the data in one JSON file. Enough for the prototype: the file can be opened and read by eye.
 * Not designed for several copies of the app running at the same time.
 */
class FileMessengerStore(directory: File) : MessengerStore {
    private val file = File(directory, "messenger.json")

    private val json = Json {
        prettyPrint = true
        // New app versions may add fields. An old version must not crash on meeting an unknown one.
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override fun load(): MessengerData {
        if (!file.exists()) return MessengerData()
        return try {
            json.decodeFromString(MessengerData.serializer(), file.readText())
        } catch (error: SerializationException) {
            keepBrokenFile()
            MessengerData()
        } catch (error: IOException) {
            keepBrokenFile()
            MessengerData()
        }
    }

    override fun save(data: MessengerData) {
        try {
            writeFileAtomically(file, json.encodeToString(MessengerData.serializer(), data).toByteArray())
        } catch (error: IOException) {
            // Crashing because of the disk is worse than not saving: the message stays on screen. The error is visible in the console.
            System.err.println("Nine: could not save data to $file: ${error.message}")
        }
    }

    /** A damaged file is not deleted but set aside next to it: maybe something can still be recovered by hand. */
    private fun keepBrokenFile() {
        val broken = File(file.parentFile, "${file.name}.broken")
        try {
            Files.move(file.toPath(), broken.toPath(), StandardCopyOption.REPLACE_EXISTING)
            System.err.println("Nine: the data file cannot be read, it was saved as $broken, starting from an empty state.")
        } catch (error: IOException) {
            System.err.println("Nine: the data file cannot be read and could not be set aside: ${error.message}")
        }
    }
}
