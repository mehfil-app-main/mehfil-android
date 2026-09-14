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
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.OAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.mehfil.app.BuildConfig
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await

/** [phone] is E.164 and present once a number has been verified with an OTP. */
data class Account(val uid: String, val name: String?, val email: String?, val provider: String, val phone: String? = null) {
    val providerLabel get() = when (provider) { "apple.com" -> "Apple"; "google.com" -> "Google"; "phone" -> "Phone"; else -> "This device" }
}

class AuthCancelled : Exception("Sign-in was cancelled.")

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
    private fun account(u: FirebaseUser) = Account(u.uid, u.displayName, u.email, u.providerData.firstOrNull { it.providerId != "firebase" }?.providerId ?: "firebase", u.phoneNumber)

    init {
        // The emulator has no Play Integrity: let the fictional test numbers from the Firebase console work without app verification.
        val emulator = android.os.Build.HARDWARE.contains("ranchu") || android.os.Build.HARDWARE.contains("goldfish") || android.os.Build.PRODUCT.startsWith("sdk")
        if (auth != null && BuildConfig.DEBUG && emulator) auth.firebaseAuthSettings.setAppVerificationDisabledForTesting(true)
    }

    fun continueOnDevice() { prefs.edit().putBoolean("local", true).apply(); state = State.SignedIn(localAccount) }

    /** Google via Credential Manager: the Web client id comes from google-services.json (default_web_client_id). */
    suspend fun signInWithGoogle(activity: Activity) {
        val auth = auth ?: error("Cloud sign-in is not configured in this build.")
        val credential = googleCredential(activity)
        auth.signInWithCredential(GoogleAuthProvider.getCredential(credential.idToken, null)).await()
    }

    /** Sign in with Apple on Android uses Firebase's web OAuth flow (Services ID configured in the console). */
    suspend fun signInWithApple(activity: Activity) {
        val auth = auth ?: error("Cloud sign-in is not configured in this build.")
        val provider = OAuthProvider.newBuilder("apple.com").setScopes(listOf("email", "name")).build()
        val pending = auth.pendingAuthResult
        if (pending != null) { pending.await(); return }
        auth.startActivityForSignInWithProvider(activity, provider).await()
    }

    // ---- Phone number + OTP ----

    /** What a verified code should do: start a session, attach the number to the signed-in account, replace it, or prove identity. */
    enum class PhoneIntent { SIGN_IN, LINK, UPDATE, REAUTH }
    private var verificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null
    private var autoCredential: PhoneAuthCredential? = null

    /** Texts a 6-digit code. Returns true when Play services verified the number instantly (no code needed). */
    suspend fun sendCode(activity: Activity, phone: String): Boolean {
        val auth = auth ?: error("Cloud sign-in is not configured in this build.")
        autoCredential = null
        return suspendCancellableCoroutine { cont ->
            val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) { autoCredential = credential; if (cont.isActive) cont.resume(true) }
                override fun onVerificationFailed(e: FirebaseException) { if (cont.isActive) cont.resumeWithException(e) }
                override fun onCodeSent(id: String, token: PhoneAuthProvider.ForceResendingToken) { verificationId = id; resendToken = token; if (cont.isActive) cont.resume(false) }
            }
            val b = PhoneAuthOptions.newBuilder(auth).setPhoneNumber(phone).setTimeout(60L, TimeUnit.SECONDS).setActivity(activity).setCallbacks(callbacks)
            resendToken?.let { b.setForceResendingToken(it) }
            PhoneAuthProvider.verifyPhoneNumber(b.build())
        }
    }

    /** Confirms the code (or the instant credential) and applies the intent. Returns the verified E.164 number. */
    suspend fun confirmCode(code: String?, intent: PhoneIntent): String {
        val auth = auth ?: error("Cloud sign-in is not configured in this build.")
        val credential = autoCredential ?: PhoneAuthProvider.getCredential(verificationId ?: error("Send a code first."), code?.filter { it.isDigit() } ?: error("Enter the code."))
        val user: FirebaseUser = when (intent) {
            PhoneIntent.SIGN_IN -> auth.signInWithCredential(credential).await().user!!
            PhoneIntent.LINK -> {
                val u = auth.currentUser ?: error("You're not signed in.")
                try { u.linkWithCredential(credential).await().user!! }
                catch (e: FirebaseAuthUserCollisionException) { if (e.errorCode == "ERROR_PROVIDER_ALREADY_LINKED") { u.updatePhoneNumber(credential).await(); u } else throw e }
            }
            PhoneIntent.UPDATE -> { val u = auth.currentUser ?: error("You're not signed in."); u.updatePhoneNumber(credential).await(); u }
            PhoneIntent.REAUTH -> { val u = auth.currentUser ?: error("You're not signed in."); u.reauthenticate(credential).await(); u }
        }
        verificationId = null; resendToken = null; autoCredential = null
        state = State.SignedIn(account(user))
        return user.phoneNumber ?: ""
    }

    /** Firebase's phone-auth errors in plain words. */
    fun phoneMessage(e: Throwable): String = when {
        e is FirebaseAuthInvalidCredentialsException && e.errorCode == "ERROR_INVALID_VERIFICATION_CODE" -> "That code isn't right. Check the SMS and try again."
        e is FirebaseAuthInvalidCredentialsException && e.errorCode == "ERROR_SESSION_EXPIRED" -> "The code has expired — send a new one."
        e is FirebaseAuthInvalidCredentialsException -> "Enter a valid mobile number with its country code."
        e is FirebaseAuthUserCollisionException -> "This number is already linked to another Mehfil account."
        e is FirebaseTooManyRequestsException -> "Too many attempts from this device. Try again in a while."
        e is FirebaseAuthException && e.errorCode == "ERROR_REQUIRES_RECENT_LOGIN" -> "Please sign in again before changing your number."
        e is FirebaseException && (e.message ?: "").contains("verify", true) -> "We couldn't confirm this app. Try again on a real device."
        else -> e.message ?: "Something went wrong."
    }

    suspend fun signOut() {
        if (auth != null) {
            auth.signOut()
            runCatching { CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest()) }
        } else { prefs.edit().putBoolean("local", false).apply(); state = State.SignedOut }
    }

    /** Re-authenticates (Firebase requires a recent sign-in) and deletes the user. The caller erased the data first. */
    suspend fun deleteAccount(activity: Activity) {
        val auth = auth ?: run { prefs.edit().putBoolean("local", false).apply(); state = State.SignedOut; return }
        val user = auth.currentUser ?: return
        when (user.providerData.firstOrNull { it.providerId != "firebase" }?.providerId) {
            "google.com" -> { val c = googleCredential(activity); user.reauthenticate(GoogleAuthProvider.getCredential(c.idToken, null)).await() }
            "apple.com" -> { val provider = OAuthProvider.newBuilder("apple.com").build(); user.startActivityForReauthenticateWithProvider(activity, provider).await() }
            // "phone" accounts re-authenticate with an OTP (confirmCode(…, REAUTH)) before the caller gets here.
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
        error("Google did not return an ID token.")
    }
}
