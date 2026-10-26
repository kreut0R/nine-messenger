package app.nine.data

import app.nine.domain.MessengerData
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Хранит данные в одном JSON-файле. Для прототипа этого достаточно: файл можно открыть и прочитать глазами.
 * Не рассчитан на несколько запущенных копий приложения одновременно.
 */
class FileMessengerStore(directory: File) : MessengerStore {
    private val file = File(directory, "messenger.json")

    private val json = Json {
        prettyPrint = true
        // Новые версии приложения могут дописывать поля. Старая версия не должна падать, встретив незнакомое.
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

    /**
     * Сначала пишем во временный файл и только потом подменяем основной.
     * Так при сбое посреди записи старый файл остаётся целым, а не превращается в обрубок.
     */
    override fun save(data: MessengerData) {
        try {
            file.parentFile.mkdirs()
            val temp = File(file.parentFile, "${file.name}.tmp")
            temp.writeText(json.encodeToString(MessengerData.serializer(), data))
            moveReplacing(temp, file)
        } catch (error: IOException) {
            // Падать из-за диска хуже, чем не сохранить: сообщение остаётся на экране. Ошибку видно в консоли.
            System.err.println("Nine: could not save data to $file: ${error.message}")
        }
    }

    /** Повреждённый файл не удаляем, а откладываем рядом: вдруг из него ещё можно что-то достать вручную. */
    private fun keepBrokenFile() {
        val broken = File(file.parentFile, "${file.name}.broken")
        try {
            Files.move(file.toPath(), broken.toPath(), StandardCopyOption.REPLACE_EXISTING)
            System.err.println("Nine: data file is unreadable, kept as $broken, starting with an empty state.")
        } catch (error: IOException) {
            System.err.println("Nine: data file is unreadable and could not be set aside: ${error.message}")
        }
    }

    private fun moveReplacing(from: File, to: File) {
        try {
            Files.move(from.toPath(), to.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        } catch (error: AtomicMoveNotSupportedException) {
            Files.move(from.toPath(), to.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }
}
