package com.mehfil.app.ui.screens

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mehfil.app.model.AppStore
import com.mehfil.app.model.AuthService
import com.mehfil.app.model.Fmt
import com.mehfil.app.ui.components.ButtonKind
import com.mehfil.app.ui.components.InputField
import com.mehfil.app.ui.components.MButton
import com.mehfil.app.ui.components.SheetContent
import com.mehfil.app.ui.theme.Dim
import com.mehfil.app.ui.theme.M
import com.mehfil.app.ui.theme.Radius
import com.mehfil.app.ui.theme.Space
import com.mehfil.app.ui.theme.Type
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Mobile number + OTP in two steps. Used to sign in, to attach a number to a Google/Apple account, to change it,
 * and to re-authenticate before deleting a phone account. Calls [onVerified] with the E.164 number and dismisses.
 */
@Composable
fun PhoneVerifySheet(store: AppStore, intent: AuthService.PhoneIntent, initialPhone: String = "", onVerified: (String) -> Unit, onDismiss: () -> Unit) {
    val m = M
    val auth = store.auth
    val activity = LocalContext.current as Activity
    val scope = rememberCoroutineScope()
    val digits = initialPhone.filter { it.isDigit() }
    var code by remember { mutableStateOf(if (initialPhone.startsWith("+") && !(digits.startsWith("91") && digits.length == 12)) "" else "91") }
    var number by remember { mutableStateOf(if (digits.startsWith("91") && digits.length == 12) digits.drop(2) else digits) }
    var otp by remember { mutableStateOf("") }
    var sentTo by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var resendIn by remember { mutableIntStateOf(0) }
    val e164 = Fmt.e164(code, number)
    val title = when (intent) {
        AuthService.PhoneIntent.SIGN_IN -> "Continue with your mobile number"
        AuthService.PhoneIntent.LINK -> "Verify your mobile number"
        AuthService.PhoneIntent.UPDATE -> "Change your mobile number"
        AuthService.PhoneIntent.REAUTH -> "Confirm it's you"
    }
    LaunchedEffect(resendIn) { if (resendIn > 0) { delay(1000); resendIn-- } }

    fun finish(phone: String) { busy = false; onVerified(phone); onDismiss() }
    fun verify() {
        if (otp.length < 6 || busy) return
        error = null; busy = true
        scope.launch { try { finish(auth.confirmCode(otp, intent)) } catch (e: Exception) { error = auth.phoneMessage(e); busy = false } }
    }
    fun send() {
        val phone = e164 ?: return
        error = null; busy = true
        scope.launch {
            try {
                val instant = auth.sendCode(activity, phone)
                if (instant) finish(auth.confirmCode(null, intent))   // Play services read the SMS itself
                else { sentTo = phone; otp = ""; resendIn = 30; busy = false }
            } catch (e: Exception) { error = auth.phoneMessage(e); busy = false }
        }
    }

    EditSheet(onDismiss) {
        SheetContent(title, sentTo?.let { "We sent a 6-digit code by SMS to ${Fmt.phone(it)}." } ?: "We'll text you a 6-digit code to confirm the number.", actions = {
            if (sentTo == null) MButton("Send code", { send() }, enabled = e164 != null, loading = busy)
            else {
                MButton("Verify", { verify() }, enabled = otp.length == 6, loading = busy)
                MButton(if (resendIn > 0) "Resend in ${resendIn}s" else "Resend code", { send() }, kind = ButtonKind.TERTIARY, compact = true, enabled = resendIn == 0 && !busy)
                MButton("Change number", { sentTo = null; otp = ""; error = null }, kind = ButtonKind.TERTIARY, compact = true)
            }
        }) {
            if (sentTo == null) Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                InputField("Code", code, { code = it.filter { c -> c.isDigit() }.take(3) }, Modifier.width(84.dp), placeholder = "91", keyboard = KeyboardType.Number)
                InputField("Mobile number", number, { number = it.filter { c -> c.isDigit() }.take(12) }, Modifier.weight(1f), placeholder = "98110 08123", keyboard = KeyboardType.Number)
            } else {
                InputField("6-digit code", otp, { otp = it.filter { c -> c.isDigit() }.take(6); if (otp.length == 6) verify() }, placeholder = "••••••", keyboard = KeyboardType.Number)
            }
            if (error != null) Text(error!!, style = Type.caption, color = m.danger)
        }
    }
}

/** A read-only phone row with its verification state and the action that opens the OTP sheet. */
@Composable
fun PhoneRow(phone: String, verified: Boolean, onClick: () -> Unit) {
    val m = M
    Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
        Text("Mobile number", style = Type.caption, color = m.textMute)
        Row(
            Modifier.fillMaxWidth().height(Dim.input).clip(RoundedCornerShape(Radius.sm)).background(m.surface).border(1.dp, m.lineInput, RoundedCornerShape(Radius.sm)).clickable(onClick = onClick).padding(horizontal = Space.md),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.sm),
        ) {
            Text(if (phone.isEmpty()) "Add and verify" else Fmt.phone(phone), style = Type.bodyMd, color = if (phone.isEmpty()) m.textMute else m.text)
            Spacer(Modifier.weight(1f))
            if (verified) {
                Icon(Icons.Outlined.Verified, null, Modifier.size(16.dp), tint = m.accentText); Text("Verified", style = Type.caption, color = m.accentText)
                Text("Change", style = Type.buttonSm, color = m.accentText, modifier = Modifier.padding(start = Space.sm))
            } else Text(if (phone.isEmpty()) "Verify" else "Verify now", style = Type.buttonSm, color = m.accentText)
        }
        Text(if (verified) "Clients see this number on your shared page." else "Numbers are confirmed with a one-time code before they're saved.", style = Type.caption, color = m.textMute)
    }
}
