package app.nine.domain

import kotlin.jvm.JvmInline

/** Идентификатор аккаунта: хэш публичного ключа входа в шестнадцатеричной записи. Телефона и почты у аккаунта нет. */
@JvmInline
value class AccountId(val hex: String)

/** Первые 128 бит для показа человеку группами по 4 символа. По ним можно сверить аккаунт голосом или глазами. */
val AccountId.displayCode: String
    get() = hex.uppercase().take(32).chunked(4).joinToString(" ")
