package app.nine

import app.nine.ui.platform.SystemClipboard
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.awt.datatransfer.UnsupportedFlavorException
import java.io.IOException

/** Буфер обмена через AWT. Если система его не отдаёт (например, занят другим приложением), молча ничего не делает. */
class AwtSystemClipboard : SystemClipboard {
    private val clipboard get() = Toolkit.getDefaultToolkit().systemClipboard

    override fun copy(text: String) {
        try {
            clipboard.setContents(StringSelection(text), null)
        } catch (error: IllegalStateException) {
            System.err.println("Nine: буфер обмена недоступен: ${error.message}")
        }
    }

    override fun text(): String? = try {
        if (clipboard.isDataFlavorAvailable(DataFlavor.stringFlavor)) clipboard.getData(DataFlavor.stringFlavor) as? String else null
    } catch (error: IllegalStateException) {
        null
    } catch (error: UnsupportedFlavorException) {
        null
    } catch (error: IOException) {
        null
    }
}
