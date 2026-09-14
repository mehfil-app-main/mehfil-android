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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.OAuthProvider
import com.mehfil.app.BuildConfig
import kotlinx.coroutines.tasks.await

data class Account(val uid: String, val name: String?, val email: String?, val provider: String) {
    val providerLabel get() = when (provider) { "apple.com" -> "Apple"; "google.com" -> "Google"; else -> "This device" }
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
    private fun account(u: FirebaseUser) = Account(u.uid, u.displayName, u.email, u.providerData.firstOrNull { it.providerId != "firebase" }?.providerId ?: "firebase")

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
