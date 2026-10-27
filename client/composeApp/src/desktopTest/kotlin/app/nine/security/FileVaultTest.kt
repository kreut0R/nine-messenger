package app.nine.security

import app.nine.data.FileMessengerStore
import app.nine.domain.Chat
import app.nine.domain.ChatId
import app.nine.domain.MessengerData
import app.nine.support.FakeTime
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class FileVaultTest {
    private val directory: File = Files.createTempDirectory("nine-vault-test").toFile()
    private val time = FakeTime(now = 1_000)

    // Cheap Argon2 so the tests run fast. The production cost is checked only by being written next to the data.
    private val cheap = Argon2Cost(memoryKiB = 64, iterations = 1, parallelism = 1)
    private fun vault() = FileVault(directory, time, cheap)

    private val code = "correct code 2026"
    private val secretName = "SecretPeerName"

    private fun chats(name: String = secretName) =
        MessengerData(chats = listOf(Chat(ChatId("c-1"), name, createdAtMillis = 5)))

    private fun unlocked(result: UnlockResult): Session = assertIs<UnlockResult.Unlocked>(result).session

    @Test
    fun newDeviceHasNoAccountUntilOneIsCreated() {
        assertFalse(vault().hasAccount())
        vault().createAccount(code)
        assertTrue(vault().hasAccount())
    }

    @Test
    fun createdAccountOpensWithTheSameCodeAndKeepsItsIdAndChats() {
        val created = vault().createAccount(code)
        created.store.save(chats())
        created.lock()

        val reopened = unlocked(vault().unlock(code))

        assertEquals(created.accountId, reopened.accountId)
        assertEquals(chats(), reopened.store.load())
    }

    @Test
    fun differentAccountsGetDifferentIds() {
        val first = vault().createAccount(code).accountId
        val otherDirectory = Files.createTempDirectory("nine-vault-other").toFile()
        val second = FileVault(otherDirectory, time, cheap).createAccount(code).accountId

        assertNotEquals(first, second)
        assertEquals(64, first.hex.length)
    }

    @Test
    fun filesOnDiskContainNeitherChatsNorTheCode() {
        val session = vault().createAccount(code)
        session.store.save(chats())

        directory.listFiles()!!.forEach { file ->
            val text = file.readBytes().toString(Charsets.ISO_8859_1) + file.readText()
            assertFalse(secretName in file.readText(), "${file.name} contains the peer name")
            assertFalse(code in text, "${file.name} contains the access code")
        }
        assertEquals(setOf("vault.json", "identity.enc", "messenger.enc"), directory.list()!!.toSet())
    }

    @Test
    fun shortCodeIsRefused() {
        assertFailsWith<IllegalArgumentException> { vault().createAccount("1234567") }
        assertFalse(vault().hasAccount())
    }

    @Test
    fun wrongCodeIsRefusedAndCountsDownToAPause() {
        vault().createAccount(code).lock()

        for (left in 4 downTo 1) {
            assertEquals(UnlockResult.WrongCode(attemptsBeforePause = left), vault().unlock("wrong code"))
        }
        val paused = assertIs<UnlockResult.Paused>(vault().unlock("wrong code"))
        assertEquals(time.now + 30_000, paused.untilMillis)
    }

    @Test
    fun correctCodeIsRefusedDuringPauseAndAcceptedAfterIt() {
        vault().createAccount(code).lock()
        repeat(Lockout.FreeAttempts) { vault().unlock("wrong code") }

        assertIs<UnlockResult.Paused>(vault().unlock(code))
        time.now += 30_001

        assertIs<UnlockResult.Unlocked>(vault().unlock(code))
    }

    @Test
    fun failedAttemptsSurviveRestartAndResetAfterSuccess() {
        vault().createAccount(code).lock()
        repeat(3) { vault().unlock("wrong code") }

        // A new vault instance, as after an app restart: the counter does not reset.
        assertEquals(UnlockResult.WrongCode(attemptsBeforePause = 1), vault().unlock("wrong code"))

        unlocked(vault().unlock(code)).lock()
        assertEquals(UnlockResult.WrongCode(attemptsBeforePause = 4), vault().unlock("wrong code"))
    }

    @Test
    fun pauseGrowsWithEveryFailureAfterTheFreeAttempts() {
        vault().createAccount(code).lock()
        repeat(Lockout.FreeAttempts) { vault().unlock("wrong code") }

        time.now += 30_001
        val second = assertIs<UnlockResult.Paused>(vault().unlock("wrong code"))

        assertEquals(time.now + 60_000, second.untilMillis)
    }

    @Test
    fun damagedKeyFileIsReportedInsteadOfCrashing() {
        vault().createAccount(code).lock()
        File(directory, "vault.json").writeText("{ not json")

        assertEquals(UnlockResult.Damaged, vault().unlock(code))
    }

    @Test
    fun tamperedChatFileIsSetAsideAndStateStartsEmpty() {
        val session = vault().createAccount(code)
        session.store.save(chats())
        session.lock()
        val file = File(directory, "messenger.enc")
        file.writeBytes(file.readBytes().also { it[it.size - 1] = (it[it.size - 1] + 1).toByte() })

        val reopened = unlocked(vault().unlock(code))

        assertEquals(MessengerData(), reopened.store.load())
        assertTrue(File(directory, "messenger.enc.broken").exists())
    }

    @Test
    fun openChatsFromThePrototypeMoveInsideAndThePlainFileIsGone() {
        FileMessengerStore(directory).save(chats())

        val session = vault().createAccount(code)

        assertEquals(chats(), session.store.load())
        assertFalse(File(directory, "messenger.json").exists())
        assertFalse(directory.listFiles()!!.any { secretName in it.readText() })
    }

    @Test
    fun keyBackupRestoresTheSameAccountOnAnotherDevice() {
        val original = vault().createAccount(code)
        val backup = original.exportKeyBackup("backup password")
        val otherDirectory = Files.createTempDirectory("nine-vault-restore").toFile()

        val restored = assertIs<RestoreResult.Restored>(
            FileVault(otherDirectory, time, cheap).restoreAccount(backup, "backup password", "new code 2027"),
        ).session

        assertEquals(original.accountId, restored.accountId)
        assertIs<UnlockResult.Unlocked>(FileVault(otherDirectory, time, cheap).unlock("new code 2027"))
    }

    @Test
    fun backupDoesNotContainTheKeyInTheOpen() {
        val session = vault().createAccount(code)
        val backup = session.exportKeyBackup("backup password")
        val identityFile = File(directory, "identity.enc")

        assertFalse(identityFile.readBytes().contentEquals(backup))
        assertFalse(backup.decodeToString().contains(session.accountId.hex))
    }

    @Test
    fun wrongBackupPasswordLeavesTheCurrentAccountUntouched() {
        val session = vault().createAccount(code)
        session.store.save(chats())
        val backup = session.exportKeyBackup("backup password")

        assertEquals(RestoreResult.WrongBackupPassword, vault().restoreAccount(backup, "not the password", "new code 2027"))

        assertEquals(chats(), unlocked(vault().unlock(code)).store.load())
    }

    @Test
    fun randomBytesAreNotABackup() {
        vault().createAccount(code)

        assertEquals(RestoreResult.NotABackup, vault().restoreAccount(byteArrayOf(1, 2, 3), "password", "new code 2027"))
        assertEquals(RestoreResult.NotABackup, vault().restoreAccount("{}".toByteArray(), "password", "new code 2027"))
    }

    @Test
    fun restoringOverAnExistingAccountReplacesItWithTheBackupKey() {
        val other = Files.createTempDirectory("nine-vault-source").toFile()
        val source = FileVault(other, time, cheap).createAccount(code)
        val backup = source.exportKeyBackup("backup password")
        val here = vault().createAccount("another code 2026")
        here.store.save(chats())

        val restored = assertIs<RestoreResult.Restored>(vault().restoreAccount(backup, "backup password", "forgotten old code replaced")).session

        assertEquals(source.accountId, restored.accountId)
        assertEquals(MessengerData(), restored.store.load(), "the conversations on this device are lost on restore")
    }

    @Test
    fun wipeRemovesEverythingFromTheDevice() {
        val session = vault().createAccount(code)
        session.store.save(chats())
        File(directory, "messenger.json").writeText("old junk")

        vault().wipe()

        assertEquals(emptyList(), directory.list()!!.toList())
        assertFalse(vault().hasAccount())
    }

    @Test
    fun lockedSessionHoldsNoKeyAnymore() {
        val session = vault().createAccount(code)
        session.store.save(chats())
        session.lock()

        // The key in memory is zeroed: the conversations can no longer be read through the old session.
        assertEquals(MessengerData(), session.store.load())
    }

    @Test
    fun writeAfterLockDoesNotTouchTheEncryptedFile() {
        val session = vault().createAccount(code)
        session.store.save(chats())
        val before = File(directory, "messenger.enc").readBytes()
        session.lock()

        // The autolock can fire between a key press and its handling: a write with the zero key would corrupt the file.
        session.store.save(chats("Another name"))
        session.store.load()

        assertTrue(before.contentEquals(File(directory, "messenger.enc").readBytes()))
        assertFalse(File(directory, "messenger.enc.broken").exists())
        assertEquals(chats(), unlocked(vault().unlock(code)).store.load())
    }

    @Test
    fun newCodeOpensTheAccountAndTheOldOneNoLonger() {
        val session = vault().createAccount(code)
        session.store.save(chats())

        assertEquals(ChangeCodeResult.Changed, session.changeAccessCode(code, "new code 2027"))
        session.lock()

        assertEquals(UnlockResult.WrongCode(attemptsBeforePause = 4), vault().unlock(code))
        val reopened = unlocked(vault().unlock("new code 2027"))
        assertEquals(session.accountId, reopened.accountId)
        assertEquals(chats(), reopened.store.load())
    }

    @Test
    fun wrongCurrentCodeChangesNothingAndCountsAsAFailedAttempt() {
        val session = vault().createAccount(code)

        assertEquals(ChangeCodeResult.WrongCode(attemptsBeforePause = 4), session.changeAccessCode("not this code", "new code 2027"))
        session.lock()

        assertIs<UnlockResult.Unlocked>(vault().unlock(code))
    }

    @Test
    fun changingTheCodeIsPausedAfterTooManyWrongOnes() {
        val session = vault().createAccount(code)
        repeat(Lockout.FreeAttempts - 1) { session.changeAccessCode("not this code", "new code 2027") }

        val paused = assertIs<ChangeCodeResult.Paused>(session.changeAccessCode("not this code", "new code 2027"))
        assertEquals(time.now + 30_000, paused.untilMillis)
        // While the pause lasts even the correct current code is not accepted.
        assertIs<ChangeCodeResult.Paused>(session.changeAccessCode(code, "new code 2027"))
        assertIs<UnlockResult.Paused>(vault().unlock(code))
    }

    @Test
    fun successfulChangeResetsTheFailedAttempts() {
        val session = vault().createAccount(code)
        repeat(3) { session.changeAccessCode("not this code", "new code 2027") }

        assertEquals(ChangeCodeResult.Changed, session.changeAccessCode(code, "new code 2027"))
        session.lock()

        assertEquals(UnlockResult.WrongCode(attemptsBeforePause = 4), vault().unlock("not this code"))
    }

    @Test
    fun shortNewCodeIsRefusedAndTheOldOneKeepsWorking() {
        val session = vault().createAccount(code)

        assertFailsWith<IllegalArgumentException> { session.changeAccessCode(code, "1234567") }
        session.lock()

        assertIs<UnlockResult.Unlocked>(vault().unlock(code))
    }

    @Test
    fun changingTheCodeKeepsTheOtherFilesAndWritesNoCodeToDisk() {
        val session = vault().createAccount(code)
        session.store.save(chats())
        val identityBefore = File(directory, "identity.enc").readBytes()
        val chatsBefore = File(directory, "messenger.enc").readBytes()

        session.changeAccessCode(code, "new code 2027")

        // Only the wrapper of the master key changes: the other files stay the same.
        assertTrue(identityBefore.contentEquals(File(directory, "identity.enc").readBytes()))
        assertTrue(chatsBefore.contentEquals(File(directory, "messenger.enc").readBytes()))
        directory.listFiles()!!.forEach { file ->
            val text = file.readBytes().toString(Charsets.ISO_8859_1) + file.readText()
            assertFalse("new code 2027" in text, "${file.name} contains the new code")
        }
    }

    @Test
    fun productionCostIsStoredNextToTheData() {
        FileVault(directory, time).createAccount(code).lock()

        val record = File(directory, "vault.json").readText()
        assertTrue("\"memoryKiB\": 65536" in record, record)
    }
}
