package app.nine.data

import app.nine.domain.Author
import app.nine.domain.BubbleColor
import app.nine.domain.Chat
import app.nine.domain.ChatId
import app.nine.domain.Delivery
import app.nine.domain.MessengerData
import app.nine.domain.Message
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FileMessengerStoreTest {
    private val directory: File = Files.createTempDirectory("nine-store-test").toFile()
    private val store = FileMessengerStore(directory)

    @Test
    fun missingFileGivesEmptyState() {
        assertEquals(MessengerData(), store.load())
    }

    @Test
    fun savedDataIsReadBackUnchanged() {
        val data = MessengerData(
            chats = listOf(
                Chat(
                    id = ChatId("c-1"),
                    title = "Fox",
                    createdAtMillis = 10,
                    archived = true,
                    messages = listOf(Message("m-1", Author.Me, "Hello\nworld", sentAtMillis = 20, delivery = Delivery.Sent)),
                ),
            ),
            bubbleColor = BubbleColor.Olive,
        )

        store.save(data)

        assertEquals(data, FileMessengerStore(directory).load())
    }

    @Test
    fun saveCreatesMissingDirectoryAndLeavesNoTemporaryFile() {
        val nested = File(directory, "a/b")

        FileMessengerStore(nested).save(MessengerData())

        assertEquals(listOf("messenger.json"), nested.list()!!.toList())
    }

    @Test
    fun brokenFileIsSetAsideAndStateStartsEmpty() {
        File(directory, "messenger.json").writeText("{ this is not json")

        assertEquals(MessengerData(), store.load())
        assertTrue(File(directory, "messenger.json.broken").exists(), "the damaged file must be kept next to it")
        assertFalse(File(directory, "messenger.json").exists())
    }

    @Test
    fun unknownFieldsFromNewerVersionAreIgnored() {
        File(directory, "messenger.json").writeText("""{"version": 1, "chats": [], "futureField": 42}""")

        assertEquals(MessengerData(), store.load())
    }
}
