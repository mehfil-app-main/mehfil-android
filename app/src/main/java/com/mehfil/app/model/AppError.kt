package com.mehfil.app.model

import java.io.IOException

/**
 * What the user is told when an operation fails.
 *
 * Raw exception text used to be concatenated into the toast ("Couldn't save: ${e.message}"), which leaks
 * Firestore internals and stack detail into the UI and is untranslatable. Failures are mapped to one of these
 * cases instead; [cause] is kept for logging and is never shown.
 */
sealed class AppError(val cause: Throwable? = null) {
    /** No usable connection, or the write could not reach the server. */
    class Offline(cause: Throwable? = null) : AppError(cause)
    /** The signed-in user is not allowed to touch this record — in practice, a security-rules rejection. */
    class NotAllowed(cause: Throwable? = null) : AppError(cause)
    /** Anything we have not classified. */
    class Unexpected(cause: Throwable? = null) : AppError(cause)

    companion object {
        fun from(t: Throwable): AppError = when {
            t is IOException -> Offline(t)
            t.isFirestoreCode("UNAVAILABLE", "DEADLINE_EXCEEDED") -> Offline(t)
            t.isFirestoreCode("PERMISSION_DENIED", "UNAUTHENTICATED") -> NotAllowed(t)
            else -> Unexpected(t)
        }

        /**
         * FirebaseFirestoreException carries its status in `code`; matching on the name keeps this file free of
         * a Firebase import so it stays usable from pure-Kotlin tests.
         */
        private fun Throwable.isFirestoreCode(vararg codes: String): Boolean {
            val text = this::class.simpleName.orEmpty() + " " + message.orEmpty()
            return codes.any { text.contains(it, ignoreCase = true) }
        }
    }
}
