package com.mehfil.app.model

import android.app.Activity
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.OAuthProvider
import com.google.firebase.auth.userProfileChangeRequest
import com.mehfil.app.BuildConfig
import com.mehfil.app.R
import kotlinx.coroutines.tasks.await

/**
 * @param emailVerified false only for an email/password account whose verification link has not been opened yet.
 */
data class Account(
    val uid: String,
    val name: String?,
    val email: String?,
    val provider: String,          // "apple.com" | "google.com" | "password" | "device"
    val emailVerified: Boolean = true,
) {
    val providerLabel get() = when (provider) {
        "apple.com" -> "Apple"
        "google.com" -> "Google"
        "password" -> "Email"
        else -> "This device"
    }
}

/** Thrown when the user backs out of a provider sheet; callers swallow it rather than showing an error. */
class AuthCancelled : Exception("Sign-in cancelled by user")

/** Firebase Auth when the build has a config; otherwise a single on-device account so the app still works. */
class AuthService(private val context: Context) {
    sealed class State { object Loading : State(); object SignedOut : State(); data class SignedIn(val account: Account) : State() }

    val isCloud: Boolean = BuildConfig.HAS_FIREBASE_CONFIG
    var state by mutableStateOf<State>(State.Loading); private set
    private val prefs = context.getSharedPreferences("mehfil-auth", Context.MODE_PRIVATE)
    private val auth: FirebaseAuth? = if (isCloud) FirebaseAuth.getInstance() else null
    private val localAccount = Account("local-device", null, null, "device")

    init {
        if (auth != null) auth.addAuthStateListener { a -> state = a.currentUser?.let { State.SignedIn(account(it)) } ?: State.SignedOut }
        else state = if (prefs.getBoolean("local", false)) State.SignedIn(localAccount) else State.SignedOut
    }

    val account: Account? get() = (state as? State.SignedIn)?.account

    /** True when the signed-in account still has to open its verification link before using the app. */
    val needsEmailVerification: Boolean
        get() = Features[Feature.EMAIL_VERIFICATION_REQUIRED] && account?.let { it.provider == "password" && !it.emailVerified } == true

    private fun account(u: FirebaseUser): Account {
        val provider = u.providerData.firstOrNull { it.providerId != "firebase" }?.providerId ?: "firebase"
        // Only a password account can be unverified; federated providers vouch for the address.
        return Account(u.uid, u.displayName, u.email, provider, if (provider == "password") u.isEmailVerified else true)
    }

    fun continueOnDevice() { prefs.edit().putBoolean("local", true).apply(); state = State.SignedIn(localAccount) }

    // ---- Email and password ----

    /** Minimum we accept: 8 characters with at least one letter and one digit. */
    fun passwordProblem(password: String): String? = when {
        password.length < MIN_PASSWORD_LENGTH -> context.getString(R.string.password_too_short)
        password.none { it.isLetter() } -> context.getString(R.string.password_needs_letter)
        password.none { it.isDigit() } -> context.getString(R.string.password_needs_number)
        else -> null
    }

    companion object {
        const val MIN_PASSWORD_LENGTH = 8

        fun isValidEmail(email: String): Boolean {
            val t = email.trim()
            val at = t.indexOf('@')
            if (at <= 0 || at != t.lastIndexOf('@') || t.contains(' ')) return false
            val domain = t.substring(at + 1)
            return domain.contains('.') && !domain.startsWith('.') && !domain.endsWith('.')
        }
    }

    /** Creates the account, stores the owner's name on it and sends the verification email. */
    suspend fun signUp(email: String, password: String, name: String) {
        val auth = auth ?: error(context.getString(R.string.auth_not_configured))
        val user = requireNotNull(auth.createUserWithEmailAndPassword(email.trim(), password).await().user) { "Firebase returned no user after sign-up" }
        if (name.isNotBlank()) runCatching { user.updateProfile(userProfileChangeRequest { displayName = name.trim() }).await() }
        runCatching { user.sendEmailVerification().await() }
        state = State.SignedIn(account(user))
    }

    suspend fun signIn(email: String, password: String) {
        val auth = auth ?: error(context.getString(R.string.auth_not_configured))
        val user = requireNotNull(auth.signInWithEmailAndPassword(email.trim(), password).await().user) { "Firebase returned no user after sign-in" }
        state = State.SignedIn(account(user))
    }

    /** Sends (or resends) the verification email for the signed-in account. */
    suspend fun sendVerificationEmail() {
        val user = auth?.currentUser ?: error(context.getString(R.string.auth_not_signed_in))
        user.sendEmailVerification().await()
    }

    /** Pulls the user record again so `emailVerified` reflects a link opened elsewhere. */
    suspend fun refreshVerification(): Boolean {
        val user = auth?.currentUser ?: return false
        runCatching { user.reload().await() }
        val fresh = auth.currentUser ?: return false
        state = State.SignedIn(account(fresh))
        return fresh.isEmailVerified
    }

    suspend fun sendPasswordReset(email: String) {
        val auth = auth ?: error(context.getString(R.string.auth_not_configured))
        auth.sendPasswordResetEmail(email.trim()).await()
    }

    /** Changes the password after confirming the current one. */
    suspend fun changePassword(current: String, new: String) {
        val user = auth?.currentUser ?: error(context.getString(R.string.auth_not_signed_in))
        val email = user.email ?: error(context.getString(R.string.auth_not_signed_in))
        user.reauthenticate(EmailAuthProvider.getCredential(email, current)).await()
        user.updatePassword(new).await()
    }

    /** Firebase's auth errors in plain words. */
    /** Firebase's auth errors in words a vendor can act on. Codes are documented at firebase.google.com/docs/reference/kotlin/com/google/firebase/auth/FirebaseAuthException. */
    fun message(e: Throwable): String {
        val code = (e as? FirebaseAuthException)?.errorCode
        val id = when {
            code == "ERROR_INVALID_EMAIL" -> R.string.auth_email_invalid
            code == "ERROR_EMAIL_ALREADY_IN_USE" || e is FirebaseAuthUserCollisionException -> R.string.auth_email_in_use
            code == "ERROR_WEAK_PASSWORD" || e is FirebaseAuthWeakPasswordException -> R.string.auth_password_weak
            code == "ERROR_WRONG_PASSWORD" || code == "ERROR_INVALID_CREDENTIAL" || e is FirebaseAuthInvalidCredentialsException -> R.string.auth_credentials_wrong
            code == "ERROR_USER_NOT_FOUND" || e is FirebaseAuthInvalidUserException -> R.string.auth_user_not_found
            code == "ERROR_USER_DISABLED" -> R.string.auth_user_disabled
            code == "ERROR_TOO_MANY_REQUESTS" -> R.string.auth_too_many_requests
            code == "ERROR_REQUIRES_RECENT_LOGIN" -> R.string.auth_requires_recent_login
            code == "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL" -> R.string.auth_other_provider
            (e.message ?: "").contains("network", ignoreCase = true) -> R.string.auth_no_connection
            else -> R.string.error_sign_in_failed
        }
        return context.getString(id)
    }

    // ---- Google ----

    /** Google via Credential Manager: the Web client id comes from google-services.json (default_web_client_id). */
    suspend fun signInWithGoogle(activity: Activity) {
        val auth = auth ?: error(context.getString(R.string.auth_not_configured))
        val credential = googleCredential(activity)
        auth.signInWithCredential(GoogleAuthProvider.getCredential(credential.idToken, null)).await()
    }

    /** Sign in with Apple on Android uses Firebase's web OAuth flow (Services ID configured in the console). */
    suspend fun signInWithApple(activity: Activity) {
        val auth = auth ?: error(context.getString(R.string.auth_not_configured))
        val provider = OAuthProvider.newBuilder("apple.com").setScopes(listOf("email", "name")).build()
        val pending = auth.pendingAuthResult
        if (pending != null) { pending.await(); return }
        auth.startActivityForSignInWithProvider(activity, provider).await()
    }

    suspend fun signOut() {
        if (auth != null) {
            auth.signOut()
            runCatching { CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest()) }
        } else { prefs.edit().putBoolean("local", false).apply(); state = State.SignedOut }
    }

    /**
     * Re-authenticates (Firebase requires a recent sign-in) and deletes the user. The caller erased the data first.
     * [password] is required only for email/password accounts — the UI asks for it before calling.
     */
    suspend fun deleteAccount(activity: Activity, password: String? = null) {
        val auth = auth ?: run { prefs.edit().putBoolean("local", false).apply(); state = State.SignedOut; return }
        val user = auth.currentUser ?: return
        when (user.providerData.firstOrNull { it.providerId != "firebase" }?.providerId) {
            "google.com" -> { val c = googleCredential(activity); user.reauthenticate(GoogleAuthProvider.getCredential(c.idToken, null)).await() }
            "apple.com" -> { val provider = OAuthProvider.newBuilder("apple.com").build(); user.startActivityForReauthenticateWithProvider(activity, provider).await() }
            "password" -> {
                val email = user.email ?: error(context.getString(R.string.auth_not_signed_in))
                user.reauthenticate(EmailAuthProvider.getCredential(email, password ?: error(context.getString(R.string.auth_not_signed_in)))).await()
            }
        }
        user.delete().await()
        runCatching { CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest()) }
    }

    private suspend fun googleCredential(activity: Activity): GoogleIdTokenCredential {
        val webClientId = context.getString(context.resources.getIdentifier("default_web_client_id", "string", context.packageName))
        val option = GetGoogleIdOption.Builder().setFilterByAuthorizedAccounts(false).setServerClientId(webClientId).setAutoSelectEnabled(false).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        val result = try { CredentialManager.create(activity).getCredential(activity, request) } catch (e: GetCredentialCancellationException) { throw AuthCancelled() }
        val cred = result.credential
        if (cred is CustomCredential && cred.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) return GoogleIdTokenCredential.createFrom(cred.data)
        error(context.getString(R.string.auth_no_id_token))
    }
}
