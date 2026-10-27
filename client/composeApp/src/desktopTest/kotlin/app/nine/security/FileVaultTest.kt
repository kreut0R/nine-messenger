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

    // Дешёвый Argon2, чтобы тесты шли быстро. Боевая стоимость проверяется только тем, что она записывается рядом с данными.
    private val cheap = Argon2Cost(memoryKiB = 64, iterations = 1, parallelism = 1)
    private fun vault() = FileVault(directory, time, cheap)

    private val code = "верный код 2026"
    private val secretName = "СекретноеИмяСобеседника"

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
            assertFalse(secretName in file.readText(), "${file.name} содержит имя собеседника")
            assertFalse(code in text, "${file.name} содержит код доступа")
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
            assertEquals(UnlockResult.WrongCode(attemptsBeforePause = left), vault().unlock("неверный код"))
        }
        val paused = assertIs<UnlockResult.Paused>(vault().unlock("неверный код"))
        assertEquals(time.now + 30_000, paused.untilMillis)
    }

    @Test
    fun correctCodeIsRefusedDuringPauseAndAcceptedAfterIt() {
        vault().createAccount(code).lock()
        repeat(Lockout.FreeAttempts) { vault().unlock("неверный код") }

        assertIs<UnlockResult.Paused>(vault().unlock(code))
        time.now += 30_001

        assertIs<UnlockResult.Unlocked>(vault().unlock(code))
    }

    @Test
    fun failedAttemptsSurviveRestartAndResetAfterSuccess() {
        vault().createAccount(code).lock()
        repeat(3) { vault().unlock("неверный код") }

        // Новый экземпляр хранилища, как после перезапуска приложения: счётчик не обнуляется.
        assertEquals(UnlockResult.WrongCode(attemptsBeforePause = 1), vault().unlock("неверный код"))

        unlocked(vault().unlock(code)).lock()
        assertEquals(UnlockResult.WrongCode(attemptsBeforePause = 4), vault().unlock("неверный код"))
    }

    @Test
    fun pauseGrowsWithEveryFailureAfterTheFreeAttempts() {
        vault().createAccount(code).lock()
        repeat(Lockout.FreeAttempts) { vault().unlock("неверный код") }

        time.now += 30_001
        val second = assertIs<UnlockResult.Paused>(vault().unlock("неверный код"))

        assertEquals(time.now + 60_000, second.untilMillis)
    }

    @Test
    fun damagedKeyFileIsReportedInsteadOfCrashing() {
        vault().createAccount(code).lock()
        File(directory, "vault.json").writeText("{ не json")

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
        val backup = original.exportKeyBackup("пароль для копии")
        val otherDirectory = Files.createTempDirectory("nine-vault-restore").toFile()

        val restored = assertIs<RestoreResult.Restored>(
            FileVault(otherDirectory, time, cheap).restoreAccount(backup, "пароль для копии", "новый код 2027"),
        ).session

        assertEquals(original.accountId, restored.accountId)
        assertIs<UnlockResult.Unlocked>(FileVault(otherDirectory, time, cheap).unlock("новый код 2027"))
    }

    @Test
    fun backupDoesNotContainTheKeyInTheOpen() {
        val session = vault().createAccount(code)
        val backup = session.exportKeyBackup("пароль для копии")
        val identityFile = File(directory, "identity.enc")

        assertFalse(identityFile.readBytes().contentEquals(backup))
        assertFalse(backup.decodeToString().contains(session.accountId.hex))
    }

    @Test
    fun wrongBackupPasswordLeavesTheCurrentAccountUntouched() {
        val session = vault().createAccount(code)
        session.store.save(chats())
        val backup = session.exportKeyBackup("пароль для копии")

        assertEquals(RestoreResult.WrongBackupPassword, vault().restoreAccount(backup, "не тот пароль", "новый код 2027"))

        assertEquals(chats(), unlocked(vault().unlock(code)).store.load())
    }

    @Test
    fun randomBytesAreNotABackup() {
        vault().createAccount(code)

        assertEquals(RestoreResult.NotABackup, vault().restoreAccount(byteArrayOf(1, 2, 3), "пароль", "новый код 2027"))
        assertEquals(RestoreResult.NotABackup, vault().restoreAccount("{}".toByteArray(), "пароль", "новый код 2027"))
    }

    @Test
    fun restoringOverAnExistingAccountReplacesItWithTheBackupKey() {
        val other = Files.createTempDirectory("nine-vault-source").toFile()
        val source = FileVault(other, time, cheap).createAccount(code)
        val backup = source.exportKeyBackup("пароль для копии")
        val here = vault().createAccount("другой код 2026")
        here.store.save(chats())

        val restored = assertIs<RestoreResult.Restored>(vault().restoreAccount(backup, "пароль для копии", "забытый старый код заменён")).session

        assertEquals(source.accountId, restored.accountId)
        assertEquals(MessengerData(), restored.store.load(), "переписка с этого устройства при восстановлении теряется")
    }

    @Test
    fun wipeRemovesEverythingFromTheDevice() {
        val session = vault().createAccount(code)
        session.store.save(chats())
        File(directory, "messenger.json").writeText("старьё")

        vault().wipe()

        assertEquals(emptyList(), directory.list()!!.toList())
        assertFalse(vault().hasAccount())
    }

    @Test
    fun lockedSessionHoldsNoKeyAnymore() {
        val session = vault().createAccount(code)
        session.store.save(chats())
        session.lock()

        // Ключ в памяти затёрт нулями: прочитать переписку через старую сессию уже нельзя.
        assertEquals(MessengerData(), session.store.load())
    }

    @Test
    fun writeAfterLockDoesNotTouchTheEncryptedFile() {
        val session = vault().createAccount(code)
        session.store.save(chats())
        val before = File(directory, "messenger.enc").readBytes()
        session.lock()

        // Автоблокировка может сработать между нажатием клавиши и её обработкой: запись нулевым ключом испортила бы файл.
        session.store.save(chats("Другое имя"))
        session.store.load()

        assertTrue(before.contentEquals(File(directory, "messenger.enc").readBytes()))
        assertFalse(File(directory, "messenger.enc.broken").exists())
        assertEquals(chats(), unlocked(vault().unlock(code)).store.load())
    }

    @Test
    fun newCodeOpensTheAccountAndTheOldOneNoLonger() {
        val session = vault().createAccount(code)
        session.store.save(chats())

        assertEquals(ChangeCodeResult.Changed, session.changeAccessCode(code, "новый код 2027"))
        session.lock()

        assertEquals(UnlockResult.WrongCode(attemptsBeforePause = 4), vault().unlock(code))
        val reopened = unlocked(vault().unlock("новый код 2027"))
        assertEquals(session.accountId, reopened.accountId)
        assertEquals(chats(), reopened.store.load())
    }

    @Test
    fun wrongCurrentCodeChangesNothingAndCountsAsAFailedAttempt() {
        val session = vault().createAccount(code)

        assertEquals(ChangeCodeResult.WrongCode(attemptsBeforePause = 4), session.changeAccessCode("не тот код", "новый код 2027"))
        session.lock()

        assertIs<UnlockResult.Unlocked>(vault().unlock(code))
    }

    @Test
    fun changingTheCodeIsPausedAfterTooManyWrongOnes() {
        val session = vault().createAccount(code)
        repeat(Lockout.FreeAttempts - 1) { session.changeAccessCode("не тот код", "новый код 2027") }

        val paused = assertIs<ChangeCodeResult.Paused>(session.changeAccessCode("не тот код", "новый код 2027"))
        assertEquals(time.now + 30_000, paused.untilMillis)
        // Пока идёт пауза, даже верный текущий код не принимается.
        assertIs<ChangeCodeResult.Paused>(session.changeAccessCode(code, "новый код 2027"))
        assertIs<UnlockResult.Paused>(vault().unlock(code))
    }

    @Test
    fun successfulChangeResetsTheFailedAttempts() {
        val session = vault().createAccount(code)
        repeat(3) { session.changeAccessCode("не тот код", "новый код 2027") }

        assertEquals(ChangeCodeResult.Changed, session.changeAccessCode(code, "новый код 2027"))
        session.lock()

        assertEquals(UnlockResult.WrongCode(attemptsBeforePause = 4), vault().unlock("не тот код"))
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

        session.changeAccessCode(code, "новый код 2027")

        // Меняется только обёртка главного ключа: остальные файлы остаются теми же.
        assertTrue(identityBefore.contentEquals(File(directory, "identity.enc").readBytes()))
        assertTrue(chatsBefore.contentEquals(File(directory, "messenger.enc").readBytes()))
        directory.listFiles()!!.forEach { file ->
            val text = file.readBytes().toString(Charsets.ISO_8859_1) + file.readText()
            assertFalse("новый код 2027" in text, "${file.name} содержит новый код")
        }
    }

    @Test
    fun productionCostIsStoredNextToTheData() {
        FileVault(directory, time).createAccount(code).lock()

        val record = File(directory, "vault.json").readText()
        assertTrue("\"memoryKiB\": 65536" in record, record)
    }
}
