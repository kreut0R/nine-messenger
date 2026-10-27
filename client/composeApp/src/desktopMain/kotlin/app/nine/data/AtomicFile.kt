package app.nine.data

import java.io.File
import java.io.IOException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Сначала пишем во временный файл и только потом подменяем основной.
 * Так при сбое посреди записи старый файл остаётся целым, а не превращается в обрубок.
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
