package app.nine

import app.nine.ui.platform.FilePicker
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.io.IOException

/** The system file chooser. Opens over the app and waits for an answer. */
class AwtFilePicker : FilePicker {
    override fun save(suggestedName: String, bytes: ByteArray): Boolean {
        val file = choose(FileDialog.SAVE, "Save file", suggestedName) ?: return false
        return try {
            file.writeBytes(bytes)
            true
        } catch (error: IOException) {
            System.err.println("Nine: could not write $file: ${error.message}")
            false
        }
    }

    override fun open(): ByteArray? {
        val file = choose(FileDialog.LOAD, "Choose a file", null) ?: return null
        return try {
            file.readBytes()
        } catch (error: IOException) {
            System.err.println("Nine: could not read $file: ${error.message}")
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
