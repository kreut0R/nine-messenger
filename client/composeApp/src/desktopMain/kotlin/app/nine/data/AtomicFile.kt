package app.nine.data

import java.io.File
import java.io.IOException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * First write to a temporary file and only then swap it in for the main one.
 * This way a crash in the middle of a write leaves the old file intact instead of a stump.
 */
@Throws(IOException::class)
fun writeFileAtomically(file: File, bytes: ByteArray) {
    file.parentFile.mkdirs()
    val temp = File(file.parentFile, "${file.name}.tmp")
    temp.writeBytes(bytes)
    try {
        Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
    } catch (error: AtomicMoveNotSupportedException) {
        Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING)
    }
}
