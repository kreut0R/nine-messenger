package app.nine.domain

import kotlin.jvm.JvmInline

/** Account identifier: hash of the sign-in public key in hexadecimal. An account has no phone number or email. */
@JvmInline
value class AccountId(val hex: String)

/** The first 128 bits for showing to a person in groups of 4 characters. Lets two people compare an account by voice or by eye. */
val AccountId.displayCode: String
    get() = hex.uppercase().take(32).chunked(4).joinToString(" ")
