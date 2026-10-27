package app.nine.security

import app.nine.data.MessengerStore
import app.nine.domain.AccountId

/**
 * Everything secret on the device: the sign-in key and the conversations. Stored encrypted, opened with the access code.
 * An account is created on the device without a phone number or email; the server knows nothing about it yet.
 */
interface Vault {
    /** Whether the device has an account. If not, create a new one or restore from a key backup. */
    fun hasAccount(): Boolean

    /** Creates a new account. Moves old plaintext conversations left over from the prototype, if any, inside. */
    fun createAccount(code: String): Session

    fun unlock(code: String): UnlockResult

    /**
     * Restores an account from a key backup and sets a new code. Everything already on the device is erased:
     * this is a way out if the code is forgotten, but the conversations on this device are lost.
     */
    fun restoreAccount(backup: ByteArray, backupPassword: String, code: String): RestoreResult

    /** Erases the account and all data from the device. The only way back is a key backup. */
    fun wipe()
}

/** An open account. While the session is alive, the keys are in memory. */
interface Session {
    val accountId: AccountId

    /** Encrypted conversation storage for [app.nine.data.MessengerRepository]. */
    val store: MessengerStore

    /** A copy of the sign-in key in a file encrypted with a separate password. Without it the account is lost together with the device. */
    fun exportKeyBackup(password: String): ByteArray

    /**
     * Changes the access code. The old code is checked as strictly as at sign-in, and wrong attempts count toward the same limit.
     * The encryption keys stay the same, only what they are wrapped with changes. So the new code does not revoke the
     * old one: if someone managed to copy the files and learned the old code, everything they copied stays open to them,
     * and so does whatever is written later if they get fresh files. A full key rotation will be a separate step.
     */
    fun changeAccessCode(current: String, newCode: String): ChangeCodeResult

    /** Wipes the keys from memory. The session cannot be used afterwards. */
    fun lock()
}

sealed interface UnlockResult {
    data class Unlocked(val session: Session) : UnlockResult

    /** Wrong code. [attemptsBeforePause] shows how many attempts are left before a pause. */
    data class WrongCode(val attemptsBeforePause: Int) : UnlockResult

    /** Too many failures: until [untilMillis] (milliseconds since 1970) no code is accepted, even the right one. */
    data class Paused(val untilMillis: Long) : UnlockResult

    /** The key file cannot be read. Restoring from a key backup remains. */
    data object Damaged : UnlockResult
}

sealed interface ChangeCodeResult {
    data object Changed : ChangeCodeResult

    /** The current code is wrong. [attemptsBeforePause] shows how many attempts are left before a pause. */
    data class WrongCode(val attemptsBeforePause: Int) : ChangeCodeResult

    /** Too many failures: until [untilMillis] (milliseconds since 1970) no code is accepted, even the right one. */
    data class Paused(val untilMillis: Long) : ChangeCodeResult

    /** The key file cannot be read. */
    data object Damaged : ChangeCodeResult
}

sealed interface RestoreResult {
    data class Restored(val session: Session) : RestoreResult
    data object WrongBackupPassword : RestoreResult
    data object NotABackup : RestoreResult
}
