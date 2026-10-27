package app.nine

import app.nine.ui.platform.FilePicker
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.io.IOException

/** Окно выбора файла из системы. Открывается поверх приложения и ждёт ответа. */
class AwtFilePicker : FilePicker {
    override fun save(suggestedName: String, bytes: ByteArray): Boolean {
        val file = choose(FileDialog.SAVE, "Сохранить файл", suggestedName) ?: return false
        return try {
            file.writeBytes(bytes)
            true
        } catch (error: IOException) {
            System.err.println("Nine: не удалось записать $file: ${error.message}")
            false
        }
    }

    override fun open(): ByteArray? {
        val file = choose(FileDialog.LOAD, "Выберите файл", null) ?: return null
        return try {
            file.readBytes()
        } catch (error: IOException) {
            System.err.println("Nine: не удалось прочитать $file: ${error.message}")
            null
        }
    }

    private fun choose(mode: Int, title: String, suggestedName: String?): File? {
        val dialog = FileDialog(null as Frame?, title, mode)
        suggestedName?.let { dialog.file = it }
        dialog.isVisible = true
        val name = dialog.file ?: return null
        return File(dialog.directory, name)
    }
}
