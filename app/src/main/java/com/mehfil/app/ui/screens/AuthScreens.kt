package com.mehfil.app.ui.screens

import android.app.Activity
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.CurrencyRupee
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mehfil.app.model.Account
import com.mehfil.app.model.AppStore
import com.mehfil.app.model.AuthCancelled
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

const val PRIVACY_URL = "https://github.com/Sanu5/mehfil-android/blob/main/PRIVACY.md"
const val TERMS_URL = "https://github.com/Sanu5/mehfil-android/blob/main/TERMS.md"

@Composable
fun SplashScreen() {
    Box(Modifier.fillMaxSize().background(M.ground), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Space.md)) {
            Box(Modifier.size(72.dp).clip(RoundedCornerShape(Radius.xl)).background(M.emphasisBg), contentAlignment = Alignment.Center) { Text("M", style = Type.displayHero, color = M.emphasisText) }
            CircularProgressIndicator(Modifier.size(20.dp), color = M.textMute, strokeWidth = 2.dp)
        }
    }
}

/** Welcome — the only screen a signed-out user sees. Google first on Android; Apple via Firebase's web flow. */
@Composable
fun WelcomeScreen(store: AppStore) {
    val m = M
    val auth = store.auth
    val activity = LocalContext.current as Activity
    val scope = rememberCoroutineScope()
    val uri = LocalUriHandler.current
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    fun run(work: suspend () -> Unit) { error = null; busy = true; scope.launch { try { work() } catch (_: AuthCancelled) {} catch (e: Exception) { error = e.message ?: "Sign-in failed" }; busy = false } }

    Box(Modifier.fillMaxSize().background(m.ground)) {
        Column(Modifier.fillMaxSize().padding(horizontal = Space.xl).padding(bottom = Space.xl, top = 48.dp), verticalArrangement = Arrangement.spacedBy(Space.xl)) {
            Spacer(Modifier.weight(1f))
            Box(Modifier.size(64.dp).clip(RoundedCornerShape(Radius.lg)).background(m.emphasisBg), contentAlignment = Alignment.Center) { Text("M", style = Type.displayHero, color = m.emphasisText) }
            Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                Text("Mehfil", style = Type.displayHero, color = m.text)
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
                if (auth.isCloud) {
                    Row(
                        Modifier.fillMaxWidth().height(Dim.button).clip(CircleShape).background(m.surface).border(1.dp, m.lineInput, CircleShape).clickable(enabled = !busy) { run { auth.signInWithGoogle(activity) } },
                        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
                    ) { GoogleMark(); Spacer(Modifier.size(Space.sm)); Text("Continue with Google", style = Type.buttonMd, color = m.text) }
                    Row(
                        Modifier.fillMaxWidth().height(Dim.button).clip(CircleShape).background(if (m.isDark) Color.White else Color.Black).clickable(enabled = !busy) { run { auth.signInWithApple(activity) } },
                        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
                    ) { Text("", style = Type.headingSm, color = if (m.isDark) Color.Black else Color.White); Spacer(Modifier.size(Space.sm)); Text("Continue with Apple", style = Type.buttonMd, color = if (m.isDark) Color.Black else Color.White) }
                } else {
                    MButton("Continue on this device", { auth.continueOnDevice() }, icon = Icons.Outlined.PhoneAndroid)
                    Text("This build has no cloud configuration, so your data stays on this device. See backend/README.md to enable Google and Apple sign-in.",
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
}

@Composable
private fun Point(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.md)) {
        Icon(icon, null, Modifier.size(Dim.icon), tint = M.accentText); Text(text, style = Type.bodyMd, color = M.text)
    }
}

/** The four-colour G, drawn so no asset is needed. */
@Composable
fun GoogleMark() {
    androidx.compose.foundation.Canvas(Modifier.size(18.dp)) {
        val stroke = androidx.compose.ui.graphics.drawscope.Stroke(width = 4.dp.toPx())
        drawArc(Color(0xFFFBBC05), -90f + 18f, 90f, false, style = stroke)
        drawArc(Color(0xFF34A853), 18f, 90f, false, style = stroke)
        drawArc(Color(0xFF4285F4), 108f, 90f, false, style = stroke)
        drawArc(Color(0xFFEA4335), 198f, 54f, false, style = stroke)
        drawRect(Color(0xFF4285F4), topLeft = androidx.compose.ui.geometry.Offset(size.width / 2, size.height / 2 - 2.dp.toPx()), size = androidx.compose.ui.geometry.Size(size.width / 2, 4.dp.toPx()))
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
                scope.launch { store.completeOnboarding(VendorProfile(owner.trim(), business.trim(), phone, area, "since ${java.time.LocalDate.now().year}"), loadSample); saving = false }
            }, enabled = owner.isNotBlank() && business.isNotBlank(), loading = saving)
        },
    ) {
        InputField("Your name", owner, { owner = it }, placeholder = "Anand Mehra")
        InputField("Business name", business, { business = it }, placeholder = "Mehfil Decor")
        InputField("Phone", phone, { phone = it }, placeholder = "98110 08123", keyboard = androidx.compose.ui.text.input.KeyboardType.Phone)
        InputField("Area", area, { area = it }, placeholder = "Sector 44, Gurugram")
        Row(Modifier.fillMaxWidth().padding(top = Space.sm), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.md)) {
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
    var confirmDelete by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    fun perform(work: suspend () -> Unit) { error = null; busy = true; scope.launch { try { work() } catch (_: AuthCancelled) {} catch (e: Exception) { error = e.message }; busy = false } }

    Box {
        Screen(title = "Account", subtitle = account?.let { it.providerLabel + (it.email?.let { e -> " · $e" } ?: "") }, onBack = nav::back, spacing = Space.lg) {
            RowGroup {
                ListRow(account?.name ?: store.vendor?.ownerName ?: "You", subtitle = account?.email ?: if (auth.isCloud) "Signed in" else "On this device only", leading = { IconTile(Icons.Outlined.Person) })
                Hairline(68.dp)
                ListRow("Storage", subtitle = if (auth.isCloud) "Synced to your Mehfil cloud account" else "This device — not backed up", leading = { IconTile(if (auth.isCloud) Icons.Outlined.Cloud else Icons.Outlined.PhoneAndroid) })
            }
            RowGroup {
                ListRow("Load the sample season", subtitle = "Adds the demo events, crew and payments", onClick = { scope.launch { store.loadSampleSeason() } }, leading = { IconTile(Icons.Outlined.AutoAwesome) }) { Chevron() }
                Hairline(68.dp)
                ListRow("Erase all data", subtitle = "Events, clients, crew, inventory and payments", onClick = { confirmErase = true }, leading = { IconTile(Icons.Outlined.DeleteOutline, tint = m.danger) }) { Chevron() }
            }
            Column(Modifier.padding(top = Space.md), verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                MButton("Sign out", { scope.launch { auth.signOut() } }, kind = ButtonKind.SECONDARY)
                MButton("Delete account", { confirmDelete = true }, kind = ButtonKind.DESTRUCTIVE)
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
    if (confirmErase) ModalBottomSheet(onDismissRequest = { confirmErase = false }, containerColor = m.surface) {
        SheetContent("Erase everything in ${store.vendor?.businessName ?: "Mehfil"}?", "Every event, client, crew member, item and payment record goes. Your account and profile stay.", actions = {
            MButton("Erase all data", { confirmErase = false; perform { store.eraseAllData(); store.show(Toast.Kind.INFO, "All data erased") } }, kind = ButtonKind.DESTRUCTIVE)
            MButton("Keep it", { confirmErase = false }, kind = ButtonKind.TERTIARY, compact = true)
        }) {}
    }
    if (confirmDelete) ModalBottomSheet(onDismissRequest = { confirmDelete = false }, containerColor = m.surface) {
        SheetContent("Delete your Mehfil account?", "You'll be asked to sign in again to confirm. ${store.vendor?.businessName ?: "Your business"} and all its records are deleted permanently.", actions = {
            MButton("Delete account and data", { confirmDelete = false; perform { store.eraseAllData(); auth.deleteAccount(activity) } }, kind = ButtonKind.DESTRUCTIVE)
            MButton("Keep my account", { confirmDelete = false }, kind = ButtonKind.TERTIARY, compact = true)
        }) {}
    }
}
