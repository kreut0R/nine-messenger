package app.nine.security

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
import kotlin.test.assertTrue

class WipeAfterFailuresTest {
    private val directory: File = Files.createTempDirectory("nine-wipe-test").toFile()
    private val time = FakeTime(now = 1_000)
    private val cheap = Argon2Cost(memoryKiB = 64, iterations = 1, parallelism = 1)
    private fun vault() = FileVault(directory, time, cheap)

    private val code = "correct code 2026"
    private val wrong = "not this code"
    private val chats = MessengerData(chats = listOf(Chat(ChatId("c-1"), "Peer", createdAtMillis = 5)))

    /** An account with the erase turned on, locked, as the person leaves the device. */
    private fun lockedAccount(limit: Int?) {
        val session = vault().createAccount(code)
        session.store.save(chats)
        if (limit != null) session.setWipeAfterFailures(limit)
        session.lock()
    }

    private fun assertEverythingIsGone() {
        assertEquals(emptyList(), directory.list()!!.toList())
        assertFalse(vault().hasAccount())
    }

    @Test
    fun isOffByDefaultSoManyWrongCodesNeverEraseAnything() {
        lockedAccount(limit = null)

        repeat(30) {
            time.now += 2 * 60 * 60 * 1000 // past any pause
            assertFalse(vault().unlock(wrong) is UnlockResult.Wiped)
        }

        time.now += 2 * 60 * 60 * 1000
        assertEquals(chats, assertIs<UnlockResult.Unlocked>(vault().unlock(code)).session.store.load())
    }

    @Test
    fun theLimitIsStoredAndReadBack() {
        val session = vault().createAccount(code)
        assertEquals(WipePolicy.Off, session.wipeAfterFailures)

        session.setWipeAfterFailures(10)
        assertEquals(10, session.wipeAfterFailures)
        // A new vault object, as after a restart.
        session.lock()
        assertEquals(10, assertIs<UnlockResult.Unlocked>(vault().unlock(code)).session.wipeAfterFailures)
    }

    @Test
    fun onlyTheOfferedLimitsAreAccepted() {
        val session = vault().createAccount(code)

        assertFailsWith<IllegalArgumentException> { session.setWipeAfterFailures(1) }
        assertFailsWith<IllegalArgumentException> { session.setWipeAfterFailures(7) }
        assertFailsWith<IllegalArgumentException> { session.setWipeAfterFailures(-5) }
        assertEquals(WipePolicy.Off, session.wipeAfterFailures)
        WipePolicy.Options.forEach { session.setWipeAfterFailures(it) }
        session.setWipeAfterFailures(WipePolicy.Off)
        assertEquals(WipePolicy.Off, session.wipeAfterFailures)
    }

    @Test
    fun theLastAllowedWrongCodeErasesEverything() {
        lockedAccount(limit = 5)

        assertEquals(UnlockResult.WrongCode(attemptsBeforePause = 4, attemptsBeforeWipe = 4), vault().unlock(wrong))
        assertEquals(UnlockResult.WrongCode(attemptsBeforePause = 3, attemptsBeforeWipe = 3), vault().unlock(wrong))
        assertEquals(UnlockResult.WrongCode(attemptsBeforePause = 2, attemptsBeforeWipe = 2), vault().unlock(wrong))
        assertEquals(UnlockResult.WrongCode(attemptsBeforePause = 1, attemptsBeforeWipe = 1), vault().unlock(wrong))
        assertTrue(vault().hasAccount())

        assertEquals(UnlockResult.Wiped, vault().unlock(wrong))

        assertEverythingIsGone()
    }

    @Test
    fun theCorrectCodeOnTheLastAllowedAttemptStillOpensTheApp() {
        lockedAccount(limit = 5)
        repeat(4) { vault().unlock(wrong) }

        assertIs<UnlockResult.Unlocked>(vault().unlock(code))
        assertTrue(vault().hasAccount())
    }

    @Test
    fun aSuccessfulSignInStartsTheCountAgain() {
        lockedAccount(limit = 5)
        repeat(4) { vault().unlock(wrong) }
        assertIs<UnlockResult.Unlocked>(vault().unlock(code)).session.lock()

        assertEquals(UnlockResult.WrongCode(attemptsBeforePause = 4, attemptsBeforeWipe = 4), vault().unlock(wrong))
        assertTrue(vault().hasAccount())
    }

    @Test
    fun theCountSurvivesARestartOfTheApp() {
        lockedAccount(limit = 5)
        repeat(3) { vault().unlock(wrong) }

        // Each call builds a new vault: the failures are read from the file, not kept in memory.
        assertEquals(UnlockResult.WrongCode(attemptsBeforePause = 1, attemptsBeforeWipe = 1), vault().unlock(wrong))
        assertEquals(UnlockResult.Wiped, vault().unlock(wrong))
    }

    @Test
    fun aPauseReportsHowManyAttemptsAreLeftBeforeTheErase() {
        lockedAccount(limit = 10)
        repeat(Lockout.FreeAttempts - 1) { vault().unlock(wrong) }

        val paused = assertIs<UnlockResult.Paused>(vault().unlock(wrong))

        assertEquals(5, paused.attemptsBeforeWipe)
    }

    @Test
    fun attemptsRefusedDuringAPauseAreNotCounted() {
        lockedAccount(limit = 10)
        repeat(Lockout.FreeAttempts) { vault().unlock(wrong) }

        repeat(20) { assertIs<UnlockResult.Paused>(vault().unlock(wrong)) }
        time.now += 30_001

        // The sixth real failure, not the twenty-sixth.
        val next = assertIs<UnlockResult.Paused>(vault().unlock(wrong))
        assertEquals(4, next.attemptsBeforeWipe)
    }

    @Test
    fun wrongCurrentCodesInSettingsCountTowardTheLimitAndCloseTheSession() {
        val session = vault().createAccount(code)
        session.store.save(chats)
        session.setWipeAfterFailures(5)

        repeat(4) { assertIs<ChangeCodeResult.WrongCode>(session.changeAccessCode(wrong, "new code 2027")) }
        assertEquals(ChangeCodeResult.Wiped, session.changeAccessCode(wrong, "new code 2027"))

        assertEverythingIsGone()
        // The session is closed: a late write must not bring the file back.
        session.store.save(chats)
        assertEquals(emptyList(), directory.list()!!.toList())
    }

    @Test
    fun changingTheCodeKeepsTheLimit() {
        val session = vault().createAccount(code)
        session.setWipeAfterFailures(20)

        assertEquals(ChangeCodeResult.Changed, session.changeAccessCode(code, "new code 2027"))

        assertEquals(20, session.wipeAfterFailures)
    }

    @Test
    fun aNewAccountStartsWithTheEraseOffEvenAfterAnErasedOne() {
        lockedAccount(limit = 5)
        repeat(5) { vault().unlock(wrong) }

        val fresh = vault().createAccount(code)

        assertEquals(WipePolicy.Off, fresh.wipeAfterFailures)
    }

    @Test
    fun theLimitIsTheOnlyThingTheErasePutsInThePlainFile() {
        lockedAccount(limit = 10)

        val record = File(directory, "vault.json").readText()

        assertTrue("\"wipeAfter\": 10" in record, record)
        assertFalse("Peer" in record)
    }
}
