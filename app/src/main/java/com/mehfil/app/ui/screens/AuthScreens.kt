package com.mehfil.app.ui.screens

import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.AutoDelete
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.CurrencyRupee
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mehfil.app.BuildConfig
import com.mehfil.app.R
import com.mehfil.app.model.Account
import com.mehfil.app.model.AppStore
import com.mehfil.app.model.AuthCancelled
import com.mehfil.app.model.AuthService
import com.mehfil.app.model.Feature
import com.mehfil.app.model.Features
import com.mehfil.app.model.Fmt
import com.mehfil.app.model.Toast
import com.mehfil.app.model.VendorProfile
import com.mehfil.app.ui.Nav
import com.mehfil.app.ui.components.*
import com.mehfil.app.ui.theme.Dim
import com.mehfil.app.ui.theme.M
import com.mehfil.app.ui.theme.Radius
import com.mehfil.app.ui.theme.Space
import com.mehfil.app.ui.theme.Type
import kotlinx.coroutines.launch

const val PRIVACY_URL = "https://sanu5.github.io/privacy/"
const val TERMS_URL = "https://sanu5.github.io/terms/"

@Composable
fun SplashScreen() {
    Box(Modifier.fillMaxSize().background(M.ground), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Space.md)) {
            Image(painterResource(R.drawable.brand_mark), null, Modifier.size(72.dp).clip(RoundedCornerShape(Radius.md)))
            CircularProgressIndicator(Modifier.size(20.dp), color = M.textMute, strokeWidth = 2.dp)
        }
    }
}

/** Welcome — the only screen a signed-out user sees. Google first on Android; email second; Apple behind a flag. */
@Composable
fun WelcomeScreen(store: AppStore) {
    val m = M
    val auth = store.auth
    val activity = LocalContext.current as Activity
    val scope = rememberCoroutineScope()
    val uri = LocalUriHandler.current
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var emailSheet by remember { mutableStateOf(false) }
    fun run(work: suspend () -> Unit) { error = null; busy = true; scope.launch { try { work() } catch (_: AuthCancelled) {} catch (e: Exception) { error = auth.message(e) }; busy = false } }

    Box(Modifier.fillMaxSize().background(m.ground)) {
        Column(Modifier.fillMaxSize().padding(horizontal = Space.xl).padding(bottom = Space.xl, top = 48.dp), verticalArrangement = Arrangement.spacedBy(Space.xl)) {
            Spacer(Modifier.weight(1f))
            Image(painterResource(R.drawable.brand_mark), null, Modifier.size(64.dp).clip(RoundedCornerShape(Radius.md)))
            Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                Text("Mehfil", style = Type.displayLg, color = m.text)
                Text("Run the wedding season without the spreadsheet.", style = Type.headingMd, color = m.text)
                Text("Every date, every rupee outstanding, every double-booking — before it becomes a problem.", style = Type.bodyMd, color = m.textMute)
            }
            Column(verticalArrangement = Arrangement.spacedBy(Space.md)) {
                Point(Icons.Outlined.Event, "The calendar is the spine")
                Point(Icons.Outlined.CurrencyRupee, "Outstanding money on the home screen")
                Point(Icons.Outlined.WarningAmber, "Conflicts surface early, unprompted")
            }
            Spacer(Modifier.weight(1f))
            Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                if (auth.isCloud && Features.hasCloudSignIn) {
                    if (Features[Feature.GOOGLE_SIGN_IN]) OutlineButton(GoogleMarkIcon, "Continue with Google", !busy) { run { auth.signInWithGoogle(activity) } }
                    if (Features[Feature.EMAIL_PASSWORD]) OutlineButton(MailIcon, "Continue with email", !busy) { emailSheet = true }
                    // Apple on Android is Firebase's web flow and needs the Services ID from backend/README step 5.
                    if (Features[Feature.APPLE_SIGN_IN]) Row(
                        Modifier.fillMaxWidth().height(Dim.button).clip(RoundedCornerShape(Radius.sm)).background(if (m.isDark) Color.White else Color.Black).clickable(enabled = !busy) { run { auth.signInWithApple(activity) } },
                        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
                    ) { Text("", style = Type.headingSm, color = if (m.isDark) Color.Black else Color.White); Spacer(Modifier.size(Space.sm)); Text("Continue with Apple", style = Type.buttonMd, color = if (m.isDark) Color.Black else Color.White) }
                } else if (Features[Feature.DEVICE_ONLY_MODE]) {
                    MButton("Continue on this device", { auth.continueOnDevice() }, icon = Icons.Outlined.PhoneAndroid)
                    Text("This build has no cloud configuration, so your data stays on this device. See backend/README.md to enable Google, Apple and email sign-in.",
                        style = Type.caption, color = m.textMute, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }
                if (error != null) Text(error!!, style = Type.caption, color = m.danger, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Row(Modifier.fillMaxWidth().padding(top = Space.xs), horizontalArrangement = Arrangement.Center) {
                    Text("By continuing you agree to the ", style = Type.caption, color = m.textMute)
                    Text("Terms", style = Type.caption, color = m.accentText, modifier = Modifier.clickable { uri.openUri(TERMS_URL) })
                    Text(" and ", style = Type.caption, color = m.textMute)
                    Text("Privacy Policy", style = Type.caption, color = m.accentText, modifier = Modifier.clickable { uri.openUri(PRIVACY_URL) })
                }
            }
        }
        if (busy) Box(Modifier.fillMaxSize().background(m.scrim), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Color.White) }
    }
    if (emailSheet) EmailAuthSheet(store) { emailSheet = false }
}

private val GoogleMarkIcon = 1
private val MailIcon = 2

/** An outlined, 8dp sign-in button with a leading mark. */
@Composable
private fun OutlineButton(kind: Int, label: String, enabled: Boolean, onClick: () -> Unit) {
    val m = M
    Row(
        Modifier.fillMaxWidth().height(Dim.button).clip(RoundedCornerShape(Radius.sm)).background(m.surface)
            .border(1.dp, m.lineInput, RoundedCornerShape(Radius.sm)).clickable(enabled = enabled, onClick = onClick),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
    ) {
        if (kind == GoogleMarkIcon) GoogleMark() else Icon(Icons.Outlined.MailOutline, null, Modifier.size(18.dp), tint = m.text)
        Spacer(Modifier.size(Space.sm))
        Text(label, style = Type.buttonMd, color = m.text)
    }
}

@Composable
private fun Point(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.md)) {
        Icon(icon, null, Modifier.size(Dim.icon), tint = M.text); Text(text, style = Type.bodyMd, color = M.text)
    }
}

/** Google's four-colour G (official geometry) as a vector drawable. */
@Composable
fun GoogleMark() {
    Image(painterResource(R.drawable.ic_google_g), null, Modifier.size(18.dp))
}

/** Email and password: create an account or sign in, with a password-reset path. One sheet, three modes. */
@Composable
fun EmailAuthSheet(store: AppStore, onDismiss: () -> Unit) {
    val m = M
    val auth = store.auth
    val scope = rememberCoroutineScope()
    var mode by remember { mutableStateOf("signIn") }     // signIn · signUp · reset
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }

    val emailOk = AuthService.isValidEmail(email)
    val canSubmit = emailOk && when (mode) {
        "reset" -> true
        "signIn" -> password.isNotEmpty()
        else -> name.isNotBlank() && AuthService.passwordProblem(password) == null && password == confirm
    }
    val title = when (mode) { "signUp" -> "Create your account"; "reset" -> "Reset your password"; else -> "Sign in" }
    val subtitle = when (mode) {
        "signUp" -> "We'll email you a link to confirm the address."
        "reset" -> "We'll email you a link to set a new password."
        else -> "With the email and password you signed up with."
    }
    fun switchTo(m2: String) { mode = m2; error = null; notice = null }
    fun submit() {
        if (!canSubmit || busy) return
        error = null; notice = null; busy = true
        scope.launch {
            try {
                when (mode) {
                    "signIn" -> { auth.signIn(email, password); onDismiss() }
                    "signUp" -> { auth.signUp(email, password, name); onDismiss() }
                    else -> { auth.sendPasswordReset(email); notice = "Sent. Open the link in your inbox, then come back and sign in."; mode = "signIn" }
                }
            } catch (e: Exception) { error = auth.message(e) }
            busy = false
        }
    }

    EditSheet(onDismiss) {
        SheetContent(title, subtitle, actions = {
            MButton(when (mode) { "signUp" -> "Create account"; "reset" -> "Send reset link"; else -> "Sign in" }, { submit() }, enabled = canSubmit, loading = busy)
        }) {
            if (mode == "signUp") InputField("Your name", name, { name = it }, placeholder = "Anand Mehra")
            InputField("Email", email, { email = it.trim() }, placeholder = "you@example.com", keyboard = KeyboardType.Email)
            if (mode != "reset") InputField("Password", password, { password = it },
                placeholder = if (mode == "signUp") "At least 8 characters" else "Your password",
                helper = if (mode == "signUp") AuthService.passwordProblem(password) else null, password = true)
            if (mode == "signUp") InputField("Confirm password", confirm, { confirm = it }, placeholder = "Type it again",
                helper = if (confirm.isNotEmpty() && confirm != password) "The two passwords don't match." else null, password = true)
            if (notice != null) Text(notice!!, style = Type.caption, color = m.text)
            if (error != null) Text(error!!, style = Type.caption, color = m.danger)
            Row(Modifier.fillMaxWidth().padding(top = Space.xs)) {
                when (mode) {
                    "signIn" -> {
                        Text("Create an account", style = Type.buttonSm, color = m.accentText, modifier = Modifier.clickable { switchTo("signUp") })
                        Spacer(Modifier.weight(1f))
                        Text("Forgot password?", style = Type.buttonSm, color = m.accentText, modifier = Modifier.clickable { switchTo("reset") })
                    }
                    "signUp" -> Text("I already have an account", style = Type.buttonSm, color = m.accentText, modifier = Modifier.clickable { switchTo("signIn") })
                    else -> Text("Back to sign in", style = Type.buttonSm, color = m.accentText, modifier = Modifier.clickable { switchTo("signIn") })
                }
            }
        }
    }
}

/** Shown after an email sign-up until the address is confirmed. Nothing else in the app is reachable from here. */
@Composable
fun VerifyEmailScreen(store: AppStore, account: Account) {
    val m = M
    val auth = store.auth
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var checking by remember { mutableStateOf(false) }
    var sent by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    // Opening the link happens in a browser or mail app, so re-check when this screen appears.
    LaunchedEffect(Unit) { auth.refreshVerification() }

    Box(Modifier.fillMaxSize().background(m.ground)) {
        Column(Modifier.fillMaxSize().padding(horizontal = Space.xl).padding(bottom = Space.xl, top = 48.dp), verticalArrangement = Arrangement.spacedBy(Space.xl)) {
            Spacer(Modifier.weight(1f))
            Icon(Icons.Outlined.MailOutline, null, Modifier.size(44.dp), tint = m.accent)
            Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                Text("Confirm your email", style = Type.displayMd, color = m.text)
                Text("We sent a link to ${account.email ?: "your inbox"}. Open it, then tap I've confirmed.", style = Type.bodyMd, color = m.textMute)
            }
            if (error != null) Text(error!!, style = Type.caption, color = m.danger)
            if (sent) Text("Sent again. It can take a minute — check spam too.", style = Type.caption, color = m.text)
            Spacer(Modifier.weight(1f))
            Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                MButton("I've confirmed", {
                    checking = true; error = null
                    scope.launch { if (!auth.refreshVerification()) error = "Not confirmed yet. Open the link in your inbox first."; checking = false }
                }, loading = checking)
                MButton("Send the link again", {
                    busy = true; error = null
                    scope.launch { try { auth.sendVerificationEmail(); sent = true } catch (e: Exception) { error = auth.message(e) }; busy = false }
                }, kind = ButtonKind.SECONDARY, loading = busy)
                MButton("Sign out", { scope.launch { auth.signOut() } }, kind = ButtonKind.TERTIARY, compact = true)
            }
        }
    }
}

/** Onboarding — the business card the rest of the app is signed with. */
@Composable
fun OnboardingScreen(store: AppStore, account: Account) {
    val m = M
    val scope = rememberCoroutineScope()
    var owner by remember { mutableStateOf(account.name ?: "") }
    var business by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var area by remember { mutableStateOf("") }
    var loadSample by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    Screen(
        title = "Set up your business", subtitle = "Signed in with ${account.providerLabel}" + (account.email?.let { " · $it" } ?: ""), isRoot = true, spacing = Space.lg,
        headerTrailing = { Text("Sign out", style = Type.buttonSm, color = m.accentText, modifier = Modifier.clickable { scope.launch { store.auth.signOut() } }.padding(Space.sm)) },
        docked = {
            MButton("Open Mehfil", {
                saving = true
                scope.launch {
                    store.completeOnboarding(VendorProfile(owner.trim(), business.trim(), phone, area, "since ${java.time.LocalDate.now().year}"), loadSample && Features[Feature.SAMPLE_SEASON])
                    saving = false
                }
            }, enabled = owner.isNotBlank() && business.isNotBlank() && Fmt.isValidPhone(phone), loading = saving)
        },
    ) {
        InputField("Your name", owner, { owner = it }, placeholder = "Anand Mehra")
        InputField("Business name", business, { business = it }, placeholder = "Mehfil Decor")
        PhoneField(phone, { phone = it }, placeholder = "98110 08123", helper = "Shown to clients on the page you share with them.")
        InputField("Area", area, { area = it }, placeholder = "Sector 44, Gurugram")
        if (Features[Feature.SAMPLE_SEASON]) Row(Modifier.fillMaxWidth().padding(top = Space.sm), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.md)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Start with a sample season", style = Type.bodyMdStrong, color = m.text)
                Text("Twelve events, crew, inventory and payments to explore. You can erase it later from Profile.", style = Type.caption, color = m.textMute)
            }
            Switch(loadSample, { loadSample = it }, colors = SwitchDefaults.colors(checkedTrackColor = m.accent))
        }
    }
}

/** Account — who is signed in, sign out, erase data, delete the account (Play's account-deletion requirement). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(store: AppStore, nav: Nav) {
    val m = M
    val auth = store.auth
    val account = auth.account
    val activity = LocalContext.current as Activity
    val scope = rememberCoroutineScope()
    val uri = LocalUriHandler.current
    var confirmErase by remember { mutableStateOf(false) }
    var confirmRemoveSample by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var passwordSheet by remember { mutableStateOf(false) }
    var flagsSheet by remember { mutableStateOf(false) }
    var deletePassword by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    fun perform(work: suspend () -> Unit) { error = null; busy = true; scope.launch { try { work() } catch (_: AuthCancelled) {} catch (e: Exception) { error = auth.message(e) }; busy = false } }

    Box {
        Screen(title = "Account", subtitle = account?.let { it.providerLabel + (it.email?.let { e -> " · $e" } ?: "") }, onBack = nav::back, spacing = Space.lg) {
            RowGroup {
                ListRow(account?.name ?: store.vendor?.ownerName ?: "You", subtitle = account?.email ?: if (auth.isCloud) "Signed in" else "On this device only", leading = { IconTile(Icons.Outlined.Person) })
                Hairline(68.dp)
                ListRow("Storage", subtitle = if (auth.isCloud) "Synced to your Mehfil cloud account" else "This device — not backed up", leading = { IconTile(if (auth.isCloud) Icons.Outlined.Cloud else Icons.Outlined.PhoneAndroid) })
                if (account?.provider == "password") {
                    Hairline(68.dp)
                    ListRow("Change password", subtitle = "You'll confirm the current one first", onClick = { passwordSheet = true }, leading = { IconTile(Icons.Outlined.VpnKey) }) { Chevron() }
                }
            }
            RowGroup {
                if (Features[Feature.SAMPLE_SEASON]) {
                    ListRow("Load the sample season", subtitle = "Adds the demo events, crew and payments", onClick = { scope.launch { store.loadSampleSeason() } }, leading = { IconTile(Icons.Outlined.AutoAwesome) }) { Chevron() }
                    Hairline(68.dp)
                }
                if (Features[Feature.REMOVE_SAMPLE_SEASON]) {
                    ListRow("Remove the sample season", subtitle = "Deletes only the demo records — your own work stays", onClick = { confirmRemoveSample = true }, leading = { IconTile(Icons.Outlined.AutoDelete) }) { Chevron() }
                    Hairline(68.dp)
                }
                ListRow("Erase all data", subtitle = "Events, clients, crew, inventory and payments", onClick = { confirmErase = true }, leading = { IconTile(Icons.Outlined.DeleteOutline, tint = m.danger) }) { Chevron() }
            }
            if (BuildConfig.DEBUG) RowGroup {
                ListRow("Feature flags", subtitle = "Debug builds only — turn parts of the app on and off", onClick = { flagsSheet = true }, leading = { IconTile(Icons.Outlined.Tune) }) { Chevron() }
            }
            Column(Modifier.padding(top = Space.md), verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                MButton("Sign out", { scope.launch { auth.signOut() } }, kind = ButtonKind.SECONDARY)
                MButton("Delete account", { deletePassword = ""; confirmDelete = true }, kind = ButtonKind.DESTRUCTIVE)
                if (error != null) Text(error!!, style = Type.caption, color = m.danger, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
            Text("Deleting removes your account and every event, client, crew member and payment record. It cannot be undone.", style = Type.caption, color = m.textMute)
            Row(horizontalArrangement = Arrangement.spacedBy(Space.lg)) {
                Text("Privacy Policy", style = Type.caption, color = m.accentText, modifier = Modifier.clickable { uri.openUri(PRIVACY_URL) })
                Text("Terms", style = Type.caption, color = m.accentText, modifier = Modifier.clickable { uri.openUri(TERMS_URL) })
            }
        }
        if (busy) Box(Modifier.fillMaxSize().background(m.scrim), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Color.White) }
    }
    if (passwordSheet) ChangePasswordSheet(store) { passwordSheet = false }
    if (flagsSheet) FeatureFlagsSheet { flagsSheet = false }
    if (confirmRemoveSample) ModalBottomSheet(onDismissRequest = { confirmRemoveSample = false }, containerColor = m.surface) {
        SheetContent("Remove the sample season?", "The twelve demo events and their clients, crew, inventory, payments and tasks go. Anything you created yourself stays exactly as it is.", actions = {
            MButton("Remove the sample data", { confirmRemoveSample = false; perform { store.removeSampleSeason() } }, kind = ButtonKind.DESTRUCTIVE)
            MButton("Keep it", { confirmRemoveSample = false }, kind = ButtonKind.TERTIARY, compact = true)
        }) {}
    }
    if (confirmErase) ModalBottomSheet(onDismissRequest = { confirmErase = false }, containerColor = m.surface) {
        SheetContent("Erase everything in ${store.vendor?.businessName ?: "Mehfil"}?", "Every event, client, crew member, item and payment record goes. Your account and profile stay.", actions = {
            MButton("Erase all data", { confirmErase = false; perform { store.eraseAllData(); store.show(Toast.Kind.INFO, "All data erased") } }, kind = ButtonKind.DESTRUCTIVE)
            MButton("Keep it", { confirmErase = false }, kind = ButtonKind.TERTIARY, compact = true)
        }) {}
    }
    if (confirmDelete) ModalBottomSheet(onDismissRequest = { confirmDelete = false }, containerColor = m.surface) {
        SheetContent("Delete your Mehfil account?", "${store.vendor?.businessName ?: "Your business"} and all its records are deleted permanently.", actions = {
            MButton("Delete account and data", { confirmDelete = false; perform { store.eraseAllData(); auth.deleteAccount(activity, deletePassword.ifBlank { null }) } },
                kind = ButtonKind.DESTRUCTIVE, enabled = account?.provider != "password" || deletePassword.isNotEmpty())
            MButton("Keep my account", { confirmDelete = false }, kind = ButtonKind.TERTIARY, compact = true)
        }) {
            if (account?.provider == "password") InputField("Confirm your password", deletePassword, { deletePassword = it },
                helper = "Firebase needs a fresh sign-in before deleting an account.", password = true)
        }
    }
}

/** Change password — current, new, confirm. */
@Composable
fun ChangePasswordSheet(store: AppStore, onDismiss: () -> Unit) {
    val m = M
    val auth = store.auth
    val scope = rememberCoroutineScope()
    var current by remember { mutableStateOf("") }
    var new by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val canSubmit = current.isNotEmpty() && AuthService.passwordProblem(new) == null && new == confirm
    EditSheet(onDismiss) {
        SheetContent("Change password", "You'll stay signed in on this device.", actions = {
            MButton("Save password", {
                busy = true; error = null
                scope.launch { try { auth.changePassword(current, new); onDismiss() } catch (e: Exception) { error = auth.message(e) }; busy = false }
            }, enabled = canSubmit, loading = busy)
        }) {
            InputField("Current password", current, { current = it }, password = true)
            InputField("New password", new, { new = it }, placeholder = "At least 8 characters", helper = AuthService.passwordProblem(new), password = true)
            InputField("Confirm new password", confirm, { confirm = it }, placeholder = "Type it again",
                helper = if (confirm.isNotEmpty() && confirm != new) "The two passwords don't match." else null, password = true)
            if (error != null) Text(error!!, style = Type.caption, color = m.danger)
        }
    }
}

/** Debug-only switchboard over [Feature]. Release builds never reach this. */
@Composable
fun FeatureFlagsSheet(onDismiss: () -> Unit) {
    val m = M
    EditSheet(onDismiss) {
        SheetContent("Feature flags", "Debug builds only. Overrides persist until you reset them.", actions = {
            MButton("Reset to shipping defaults", { Features.resetOverrides() }, kind = ButtonKind.SECONDARY)
            MButton("Done", onDismiss)
        }) {
            RowGroup {
                Feature.entries.forEachIndexed { i, f ->
                    if (i > 0) Hairline()
                    Row(Modifier.fillMaxWidth().padding(horizontal = Space.lg, vertical = Space.sm), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.md)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(f.title, style = Type.bodyMd, color = m.text)
                            Text(if (Features.isOverridden(f)) "Overridden · ships ${if (f.default) "on" else "off"}" else "Default",
                                style = Type.caption, color = if (Features.isOverridden(f)) m.accent else m.textMute)
                        }
                        Switch(Features[f], { Features.override(f, it) }, colors = SwitchDefaults.colors(checkedTrackColor = m.accent))
                    }
                }
            }
            Text("Some changes need the screen to be reopened, and sign-in flags need a sign-out.", style = Type.caption, color = m.textMute)
        }
    }
}
