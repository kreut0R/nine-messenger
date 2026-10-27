package app.nine.ui.platform

/** File choosing by the system. The interface does not know how the chooser looks: that is up to the specific platform. */
interface FilePicker {
    /** Offers to pick a place and writes [bytes] there. Returns false if the person canceled or the write failed. */
    fun save(suggestedName: String, bytes: ByteArray): Boolean

    /** Offers to pick a file and returns its contents. Null if the person canceled or the file could not be read. */
    fun open(): ByteArray?
}
