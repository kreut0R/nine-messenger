package app.nine.security

import app.nine.data.MessengerStore
import app.nine.domain.AccountId

/**
 * Всё секретное на устройстве: ключ входа и переписка. Лежит зашифрованным, открывается кодом доступа.
 * Аккаунт создаётся на устройстве без телефона и почты, сервер о нём пока ничего не знает.
 */
interface Vault {
    /** Есть ли на устройстве аккаунт. Если нет, нужно создать новый или восстановить из копии ключа. */
    fun hasAccount(): Boolean

    /** Создаёт новый аккаунт. Старую открытую переписку, если она осталась от прототипа, переносит внутрь. */
    fun createAccount(code: String): Session

    fun unlock(code: String): UnlockResult

    /**
     * Восстанавливает аккаунт из копии ключа и задаёт новый код. Всё, что уже есть на устройстве, стирается:
     * так можно выйти из положения, если код забыт, но переписка на этом устройстве при этом теряется.
     */
    fun restoreAccount(backup: ByteArray, backupPassword: String, code: String): RestoreResult

    /** Стирает с устройства аккаунт и все данные. Вернуться можно только по копии ключа. */
    fun wipe()
}

/** Открытый аккаунт. Пока сессия жива, ключи лежат в памяти. */
interface Session {
    val accountId: AccountId

    /** Зашифрованное хранилище переписки для [app.nine.data.MessengerRepository]. */
    val store: MessengerStore

    /** Копия ключа входа в файле, зашифрованном отдельным паролем. Без неё аккаунт теряется вместе с устройством. */
    fun exportKeyBackup(password: String): ByteArray

    /**
     * Меняет код доступа. Старый код проверяется так же строго, как при входе, и неверные попытки считаются в тот же лимит.
     * Ключи шифрования при этом остаются прежними, меняется только то, чем они закрыты. Поэтому новый код не отзывает
     * старый: если кто-то успел скопировать файлы и узнал старый код, ему по-прежнему открыто всё, что он скопировал,
     * и то, что будет записано позже, если он получит свежие файлы. Полная смена ключей будет отдельным шагом.
     */
    fun changeAccessCode(current: String, newCode: String): ChangeCodeResult

    /** Стирает ключи из памяти. После этого сессией пользоваться нельзя. */
    fun lock()
}

sealed interface UnlockResult {
    data class Unlocked(val session: Session) : UnlockResult

    /** Код неверный. [attemptsBeforePause] показывает, сколько попыток осталось до паузы. */
    data class WrongCode(val attemptsBeforePause: Int) : UnlockResult

    /** Слишком много неудач: до [untilMillis] (миллисекунды от 1970 года) код не принимается, даже верный. */
    data class Paused(val untilMillis: Long) : UnlockResult

    /** Файл с ключами не читается. Остаётся восстановление из копии ключа. */
    data object Damaged : UnlockResult
}

sealed interface ChangeCodeResult {
    data object Changed : ChangeCodeResult

    /** Текущий код неверный. [attemptsBeforePause] показывает, сколько попыток осталось до паузы. */
    data class WrongCode(val attemptsBeforePause: Int) : ChangeCodeResult

    /** Слишком много неудач: до [untilMillis] (миллисекунды от 1970 года) код не принимается, даже верный. */
    data class Paused(val untilMillis: Long) : ChangeCodeResult

    /** Файл с ключами не читается. */
    data object Damaged : ChangeCodeResult
}

sealed interface RestoreResult {
    data class Restored(val session: Session) : RestoreResult
    data object WrongBackupPassword : RestoreResult
    data object NotABackup : RestoreResult
}
