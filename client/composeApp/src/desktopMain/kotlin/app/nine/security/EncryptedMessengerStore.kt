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

/** То же хранилище переписки, что и файловое, но в файле лежит только шифртекст. Ключ даёт открытая сессия. */
internal class EncryptedMessengerStore(private val file: File, private val masterKey: ByteArray) : MessengerStore {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /**
     * После блокировки ключ в памяти затёрт нулями. Если бы запись всё же дошла сюда (скажем, нажатие клавиши в тот же миг,
     * когда сработала автоблокировка), файл оказался бы зашифрован нулевым ключом, а настоящий ключ его уже не открыл бы.
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
            // Как и у файлового хранилища: сообщение остаётся на экране, ошибку видно в консоли.
            System.err.println("Nine: не удалось сохранить данные в $file: ${error.message}")
        }
    }

    /** Повреждённый файл не стираем, а откладываем рядом: вдруг его ещё получится вернуть. */
    private fun keepBrokenFile() {
        val broken = File(file.parentFile, "${file.name}.broken")
        try {
            Files.move(file.toPath(), broken.toPath(), StandardCopyOption.REPLACE_EXISTING)
            System.err.println("Nine: зашифрованные данные не читаются, файл сохранён как $broken, начинаем с пустого состояния.")
        } catch (error: IOException) {
            System.err.println("Nine: зашифрованные данные не читаются и не удалось их отложить: ${error.message}")
        }
    }

    private companion object {
        const val Purpose = "nine:messenger-data"
    }
}
