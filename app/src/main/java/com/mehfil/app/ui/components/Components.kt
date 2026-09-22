package com.mehfil.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Backspace
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mehfil.app.R
import com.mehfil.app.model.AttendanceState
import com.mehfil.app.model.CrewMember
import com.mehfil.app.model.MilestoneStatus
import com.mehfil.app.model.PaymentState
import com.mehfil.app.model.Toast
import com.mehfil.app.ui.theme.ButtonShape
import com.mehfil.app.ui.theme.CardShape
import com.mehfil.app.ui.theme.Dim
import com.mehfil.app.ui.theme.FieldShape
import com.mehfil.app.ui.theme.M
import com.mehfil.app.ui.theme.Radius
import com.mehfil.app.ui.theme.Space
import com.mehfil.app.ui.theme.Type

// MARK: - 01 Button (primary, secondary, tertiary, destructive · default, pressed, disabled, loading)

enum class ButtonKind { PRIMARY, SECONDARY, TERTIARY, DESTRUCTIVE }

@Composable
fun MButton(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: ButtonKind = ButtonKind.PRIMARY,
    compact: Boolean = false,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    loading: Boolean = false,
    fullWidth: Boolean = true,
) {
    val m = M
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val fg = when {
        !enabled -> m.disabledText
        kind == ButtonKind.PRIMARY -> m.textOnPrimary
        kind == ButtonKind.DESTRUCTIVE -> m.danger
        else -> m.accentText
    }
    val bg by animateColorAsState(
        when {
            !enabled && kind == ButtonKind.PRIMARY -> m.disabledBg
            kind == ButtonKind.PRIMARY -> if (pressed) m.accentPress else m.accent
            kind == ButtonKind.DESTRUCTIVE -> if (pressed) m.dangerTint else Color.Transparent
            else -> if (pressed) m.accentTint else Color.Transparent
        }, label = "bg",
    )
    val border = when (kind) {
        ButtonKind.SECONDARY -> if (enabled) m.accentText else m.disabledText
        ButtonKind.DESTRUCTIVE -> m.danger
        else -> Color.Transparent
    }
    Row(
        modifier
            .then(if (fullWidth) Modifier.fillMaxWidth() else Modifier)
            .heightIn(min = if (compact) Dim.buttonCompact else Dim.button)
            .clip(ButtonShape)
            .background(bg)
            .border(1.dp, border, ButtonShape)
            .clickable(enabled = enabled && !loading, interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = if (compact) Space.lg else Space.xl),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
    ) {
        if (loading) CircularProgressIndicator(Modifier.size(18.dp), color = fg, strokeWidth = 2.dp)
        else if (icon != null) Icon(icon, null, Modifier.size(20.dp), tint = fg)
        if (loading || icon != null) Spacer(Modifier.width(Space.sm))
        Text(title, style = Type.buttonMd, color = fg)
    }
}

/** 44pt icon target for row actions and header buttons. */
@Composable
fun IconButton44(icon: ImageVector, onClick: () -> Unit, tint: Color = M.accentText, modifier: Modifier = Modifier, contentDescription: String? = null) {
    Box(modifier.size(Dim.touch).clip(CircleShape).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription, Modifier.size(22.dp), tint = tint)
    }
}

/** Round surface button for headers (back, search, overflow) — the Android sibling of the iOS glass buttons. */
@Composable
fun CircleAction(icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier, tint: Color = M.text, contentDescription: String? = null) {
    Surface(onClick = onClick, shape = CircleShape, color = M.surface, shadowElevation = 1.dp, border = BorderStroke(1.dp, M.line), modifier = modifier.size(Dim.touch)) {
        Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription, Modifier.size(20.dp), tint = tint) }
    }
}

// MARK: - 02 Status chip (icon + word, always both)

enum class ChipKind(val label: String) {
    PAID("Paid"), PARTIAL("Partial"), OVERDUE("Overdue"), CONFIRMED("Confirmed"), PENDING("Pending"), CANCELLED("Cancelled"),
    CONFLICT("Crew conflict"), ARRIVED("Arrived"), ABSENT("Absent"), LOSS("Running at a loss");

    val icon: ImageVector get() = when (this) {
        PAID, ARRIVED -> Icons.Outlined.CheckCircle
        PARTIAL, PENDING -> Icons.Outlined.Schedule
        OVERDUE, LOSS -> Icons.Outlined.ErrorOutline
        CONFIRMED -> Icons.Outlined.Check
        CANCELLED -> Icons.Outlined.Cancel
        CONFLICT -> Icons.Outlined.WarningAmber
        ABSENT -> Icons.Outlined.Cancel
    }
    companion object {
        fun from(p: PaymentState) = when (p) { PaymentState.PAID -> PAID; PaymentState.PARTIAL -> PARTIAL; PaymentState.OVERDUE -> OVERDUE; PaymentState.PENDING -> PENDING }
        fun from(s: MilestoneStatus) = when (s) { MilestoneStatus.PAID -> PAID; MilestoneStatus.PENDING -> PENDING; MilestoneStatus.OVERDUE -> OVERDUE }
    }
}

@Composable
fun StatusChip(kind: ChipKind, modifier: Modifier = Modifier) {
    val m = M
    val fg = when (kind) {
        ChipKind.PAID -> m.accentSoftText
        ChipKind.PARTIAL -> m.warn
        ChipKind.OVERDUE, ChipKind.CONFLICT, ChipKind.ABSENT, ChipKind.LOSS -> m.danger
        ChipKind.CONFIRMED, ChipKind.ARRIVED -> m.text
        ChipKind.PENDING, ChipKind.CANCELLED -> m.textMute
    }
    val bg = when (kind) {
        ChipKind.PAID -> m.accentSoftBg
        ChipKind.PARTIAL -> m.warnTint
        ChipKind.OVERDUE, ChipKind.CONFLICT, ChipKind.ABSENT, ChipKind.LOSS -> m.dangerTint
        else -> m.surface
    }
    val neutral = kind in listOf(ChipKind.CONFIRMED, ChipKind.ARRIVED, ChipKind.PENDING, ChipKind.CANCELLED)
    Row(
        modifier.height(28.dp).clip(CircleShape).background(bg).border(1.dp, if (neutral) m.line else Color.Transparent, CircleShape).padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.xs),
    ) {
        Icon(kind.icon, null, Modifier.size(14.dp), tint = fg)
        Text(kind.label, style = Type.caption, color = fg, maxLines = 1)
    }
}

// MARK: - 07 Avatar / avatar stack

private val avatarRes = listOf(R.drawable.avatar_1, R.drawable.avatar_2, R.drawable.avatar_3, R.drawable.avatar_4, R.drawable.avatar_5, R.drawable.avatar_6, R.drawable.avatar_7, R.drawable.avatar_8)

@Composable
fun Avatar(index: Int?, name: String, size: Dp = Dim.avatar, modifier: Modifier = Modifier) {
    Box(modifier.size(size).clip(CircleShape).background(Initials.color(name)), contentAlignment = Alignment.Center) {
        Text(Initials.of(name), style = Type.bodyMdStrong.copy(fontSize = (size.value * 0.38f).sp), color = Color.White)
    }
}

/**
 * Initials and a stable colour for a person, so the same name always looks the same on every device and on both
 * platforms. Deliberately not random: a client's tile must not change between launches.
 */
object Initials {
    /** "Vikram Kapoor" → "VK" · "Ajay" → "A" · "" → "?" */
    fun of(name: String): String {
        val words = name.split(" ").filter { it.firstOrNull()?.isLetter() == true }
        val first = words.firstOrNull()?.first() ?: return "?"
        return if (words.size > 1) "$first${words.last().first()}".uppercase() else first.uppercase().toString()
    }

    /** Eight hues that all carry white text at 4.5:1 or better, on brand with the Airbnb skin. */
    private val hues = listOf(
        Color(0xFFBF3A5C), Color(0xFF92174D), Color(0xFF460479), Color(0xFF225A9E),
        Color(0xFF1C665C), Color(0xFF734D21), Color(0xFF4D596B), Color(0xFF222222),
    )

    fun color(name: String): Color {
        if (name.isEmpty()) return hues.last()
        // FNV-1a over the bytes: stable across launches, platforms and Kotlin versions.
        var hash = 2166136261u
        for (b in name.lowercase().toByteArray()) { hash = (hash xor (b.toUInt() and 0xffu)) * 16777619u }
        return hues[(hash % hues.size.toUInt()).toInt()]
    }
}

@Composable
fun AvatarStack(members: List<CrewMember>, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy((-8).dp)) {
        members.take(4).forEach { Avatar(it.avatar, it.name, Dim.avatarSm, Modifier.border(2.dp, M.surface, CircleShape)) }
        if (members.size > 4) Box(Modifier.size(Dim.avatarSm).clip(CircleShape).background(M.line).border(2.dp, M.surface, CircleShape), contentAlignment = Alignment.Center) {
            Text("+${members.size - 4}", style = Type.caption, color = M.text)
        }
    }
}

// MARK: - 08 Commitment bar (committed · remaining · overflow) and plain progress

@Composable
fun CommitmentBar(committed: Int, total: Int, modifier: Modifier = Modifier) {
    val m = M
    val over = committed > total
    val denom = maxOf(committed, total, 1).toFloat()
    Row(modifier.fillMaxWidth().height(Dim.bar).clip(CircleShape)) {
        Box(Modifier.weight(minOf(committed, total) / denom + 0.0001f).fillMaxWidth().height(Dim.bar).background(m.accent))
        if (over) Box(Modifier.weight((committed - total) / denom).height(Dim.bar).background(m.danger))
        val rest = 1f - minOf(committed, total) / denom - (if (over) (committed - total) / denom else 0f)
        if (rest > 0.0001f) Box(Modifier.weight(rest).height(Dim.bar).background(m.line))
    }
}

@Composable
fun ProgressBar(fraction: Float, modifier: Modifier = Modifier, tint: Color = M.accent) {
    val f = fraction.coerceIn(0f, 1f)
    Row(modifier.fillMaxWidth().height(Dim.bar).clip(CircleShape).background(M.line)) {
        if (f > 0f) Box(Modifier.weight(f).height(Dim.bar).clip(CircleShape).background(tint))
        if (f < 1f) Spacer(Modifier.weight(1f - f))
    }
}

// MARK: - 09 Input field (label above, field, helper / error line)

@Composable
fun InputField(
    label: String, value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier,
    placeholder: String = "", helper: String? = null, error: String? = null, keyboard: KeyboardType = KeyboardType.Text,
    trailing: ImageVector? = null, singleLine: Boolean = true, minHeight: Dp = Dim.input, password: Boolean = false,
) {
    val m = M
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Space.sm)) {
        Text(label, style = Type.caption, color = m.textMute)
        Row(
            Modifier.fillMaxWidth().heightIn(min = minHeight).clip(FieldShape).background(m.surface)
                .border(1.dp, if (error != null) m.danger else if (focused) m.text else m.lineInput, FieldShape)
                .padding(horizontal = Space.md, vertical = if (singleLine) 0.dp else Space.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicTextField(
                value = value, onValueChange = onValueChange, modifier = Modifier.weight(1f), singleLine = singleLine,
                textStyle = Type.bodyMd.copy(color = m.text), cursorBrush = SolidColor(m.text), interactionSource = interaction,
                keyboardOptions = KeyboardOptions(keyboardType = if (password) KeyboardType.Password else keyboard),
                visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
                decorationBox = { inner -> Box { if (value.isEmpty()) Text(placeholder, style = Type.bodyMd, color = m.textMute); inner() } },
            )
            if (trailing != null) Icon(trailing, null, Modifier.size(18.dp), tint = m.textMute)
        }
        if (error != null) Text(error, style = Type.caption, color = m.danger)
        else if (helper != null) Text(helper, style = Type.caption, color = m.textMute)
    }
}

/** Field-shaped row that opens a picker or a date sheet. */
@Composable
fun PickerField(label: String, value: String, onClick: () -> Unit, modifier: Modifier = Modifier, placeholder: String = "Select", trailing: ImageVector = Icons.Outlined.ChevronRight, highlighted: Boolean = false) {
    val m = M
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Space.sm)) {
        Text(label, style = Type.caption, color = m.textMute)
        Row(
            Modifier.fillMaxWidth().height(Dim.input).clip(FieldShape).background(m.surface)
                .border(1.dp, if (highlighted) m.accent else m.lineInput, FieldShape).clickable(onClick = onClick).padding(horizontal = Space.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(value.ifEmpty { placeholder }, style = Type.bodyMd, color = if (value.isEmpty()) m.textMute else m.text, modifier = Modifier.weight(1f))
            Icon(trailing, null, Modifier.size(18.dp), tint = m.textMute)
        }
    }
}

// MARK: - 10 Amount input + numeric keypad (amount fields never open the alphanumeric keyboard, spec §5)

@Composable
fun AmountInput(amount: Int, modifier: Modifier = Modifier, focused: Boolean = true, label: String? = "Amount received", error: String? = null, onClick: (() -> Unit)? = null) {
    val m = M
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Space.sm)) {
        if (label != null) Text(label, style = Type.caption, color = m.textMute)
        Row(
            Modifier.fillMaxWidth().height(Dim.amountInput).clip(FieldShape).background(m.surface)
                .border(1.dp, if (error != null) m.danger else if (focused) m.text else m.lineInput, FieldShape)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(horizontal = Space.lg),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End,
        ) {
            Text("₹", style = Type.displayHero, color = m.accentText)
            Spacer(Modifier.width(Space.sm))
            Text(if (amount == 0) "0" else com.mehfil.app.model.Fmt.inr(amount, symbol = false), style = Type.displayHero, color = m.text)
            if (focused) { Spacer(Modifier.width(2.dp)); Box(Modifier.width(1.5.dp).height(32.dp).background(m.accent)) }
        }
        if (error != null) Text(error, style = Type.caption, color = m.danger)
    }
}

@Composable
fun NumericKeypad(amount: Int, onAmount: (Int) -> Unit, modifier: Modifier = Modifier) {
    val m = M
    val haptic = LocalHapticFeedback.current
    val keys = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("00", "0", "⌫"))
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Space.sm)) {
        keys.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                row.forEach { k ->
                    Box(
                        Modifier.weight(1f).height(Dim.keypadKey).clip(RoundedCornerShape(Radius.md)).background(m.ground).clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            when (k) {
                                "⌫" -> onAmount(amount / 10)
                                "00" -> if (amount * 100 < 100_000_000) onAmount(amount * 100)
                                else -> { val next = amount * 10 + k.toInt(); if (next < 100_000_000) onAmount(next) }
                            }
                        },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (k == "⌫") Icon(Icons.Outlined.Backspace, "Delete", Modifier.size(20.dp), tint = m.text)
                        else Text(k, style = Type.headingMd, color = m.text)
                    }
                }
            }
        }
    }
}

// MARK: - 11 List row (leading · two-line stack · trailing)

@Composable
fun ListRow(
    title: String, modifier: Modifier = Modifier, subtitle: String? = null, subtitleColor: Color = M.textMute, selected: Boolean = false,
    strongTitle: Boolean = true, onClick: (() -> Unit)? = null, leading: (@Composable () -> Unit)? = null, trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    val m = M
    Row(
        modifier.fillMaxWidth().background(if (selected) m.surfaceSelected else Color.Transparent)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .heightIn(min = Dim.row).padding(horizontal = Space.lg, vertical = Space.sm),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.md),
    ) {
        if (leading != null) leading()
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = if (strongTitle) Type.bodyMdStrong else Type.bodyMd, color = m.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) Text(subtitle, style = Type.caption, color = subtitleColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (trailing != null) trailing()
    }
}

@Composable fun Chevron(modifier: Modifier = Modifier) = Icon(Icons.Outlined.ChevronRight, null, modifier.size(20.dp), tint = M.textMute)

@Composable
fun IconTile(icon: ImageVector, tint: Color = M.textMute, background: Color = Color.Transparent) {
    Box(Modifier.size(Dim.avatar).clip(CircleShape).background(background), contentAlignment = Alignment.Center) { Icon(icon, null, Modifier.size(22.dp), tint = tint) }
}

/** Grouped rows in one card with hairline dividers. */
@Composable
fun RowGroup(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(modifier.fillMaxWidth(), shape = CardShape, color = M.surface, shadowElevation = 1.dp, border = BorderStroke(1.dp, M.line)) {
        Column { content() }
    }
}

@Composable fun Hairline(inset: Dp = Space.lg) = HorizontalDivider(Modifier.padding(start = inset), thickness = 1.dp, color = M.line)

/** design.md card: surface + hairline + Level 1, radius lg. */
@Composable
fun Card(modifier: Modifier = Modifier, fill: Color = M.surface, padding: Dp = Space.lg, onClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
    val base = modifier.fillMaxWidth()
    // Tints are alphas of documented hues; composite them over the surface so the elevation shadow does not show through.
    val solid = fill.compositeOver(M.surface)
    if (onClick != null) Surface(onClick = onClick, modifier = base, shape = CardShape, color = solid, shadowElevation = 1.dp, border = BorderStroke(1.dp, M.line)) { Box(Modifier.padding(padding)) { content() } }
    else Surface(base, shape = CardShape, color = solid, shadowElevation = 1.dp, border = BorderStroke(1.dp, M.line)) { Box(Modifier.padding(padding)) { content() } }
}

// MARK: - 12 Conflict banner (always actionable, never dismissible)

@Composable
fun ConflictBanner(text: String, modifier: Modifier = Modifier, actionTitle: String = "Resolve", onResolve: () -> Unit) {
    val m = M
    Row(modifier.fillMaxWidth().clip(CardShape).background(m.dangerTint).padding(Space.lg), horizontalArrangement = Arrangement.spacedBy(Space.md)) {
        Icon(Icons.Outlined.WarningAmber, null, Modifier.size(Dim.icon), tint = m.danger)
        Column(verticalArrangement = Arrangement.spacedBy(Space.xs)) {
            Text(text, style = Type.bodyMd, color = m.text)
            MButton(actionTitle, onResolve, kind = ButtonKind.TERTIARY, compact = true, fullWidth = false, modifier = Modifier.offset(x = (-16).dp))
        }
    }
}

// MARK: - 13 Stepper

@Composable
fun MStepper(value: Int, onValue: (Int) -> Unit, modifier: Modifier = Modifier, range: IntRange = 0..10_000, step: Int = 10) {
    val m = M
    val haptic = LocalHapticFeedback.current
    @Composable fun btn(icon: ImageVector, enabled: Boolean, onClick: () -> Unit) {
        val c = if (enabled) m.accentText else m.disabledText
        Box(Modifier.size(Dim.touch).clip(CircleShape).border(1.dp, c, CircleShape).clickable(enabled = enabled) { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onClick() }, contentAlignment = Alignment.Center) {
            Icon(icon, null, Modifier.size(18.dp), tint = c)
        }
    }
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.md)) {
        btn(Icons.Outlined.Remove, value > range.first) { onValue(maxOf(range.first, value - step)) }
        Text("$value", style = Type.headingMd, color = m.text, modifier = Modifier.widthIn(min = 48.dp), textAlign = TextAlign.Center)
        btn(Icons.Outlined.Add, value < range.last) { onValue(minOf(range.last, value + step)) }
    }
}

// MARK: - 14 Segmented control (pill track, selected segment lifted)

@Composable
fun <T> SegmentedControl(items: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    val m = M
    Row(modifier.fillMaxWidth().clip(CircleShape).background(m.segmentTrack).padding(3.dp)) {
        items.forEach { item ->
            val on = item == selected
            Surface(
                onClick = { onSelect(item) }, modifier = Modifier.weight(1f).height(36.dp), shape = CircleShape,
                color = if (on) m.segmentSelected else Color.Transparent, shadowElevation = if (on) 1.dp else 0.dp,
            ) {
                Box(contentAlignment = Alignment.Center) { Text(label(item), style = if (on) Type.bodyMdStrong else Type.bodyMd, color = if (on) m.text else m.textMute, maxLines = 1) }
            }
        }
    }
}

// MARK: - 17 Empty state

@Composable
fun EmptyState(headline: String, line: String, modifier: Modifier = Modifier, actionTitle: String? = null, actionIcon: ImageVector? = null, primary: Boolean = false, onAction: (() -> Unit)? = null) {
    Column(modifier.fillMaxWidth().padding(horizontal = Space.xl), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Space.xs)) {
            Text(headline, style = Type.headingSm, color = M.text)
            Text(line, style = Type.bodyMd, color = M.textMute, textAlign = TextAlign.Center)
        }
        if (actionTitle != null && onAction != null) MButton(actionTitle, onAction, kind = if (primary) ButtonKind.PRIMARY else ButtonKind.SECONDARY, compact = true, icon = actionIcon, fullWidth = false)
    }
}

// MARK: - 18 Toast

@Composable
fun ToastView(toast: Toast, modifier: Modifier = Modifier) {
    val m = M
    val (icon, tint) = when (toast.kind) {
        Toast.Kind.SUCCESS -> Icons.Outlined.CheckCircle to m.toastSuccess
        Toast.Kind.ERROR -> Icons.Outlined.ErrorOutline to m.toastError
        Toast.Kind.INFO -> Icons.Outlined.Info to m.toastInfo
    }
    Surface(modifier, shape = CircleShape, color = m.toastBg, shadowElevation = 6.dp) {
        Row(Modifier.padding(horizontal = Space.lg, vertical = Space.md), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.md)) {
            Icon(icon, null, Modifier.size(18.dp), tint = tint)
            Text(toast.message, style = Type.bodyMd, color = m.toastText, maxLines = 2)
        }
    }
}

/** Tri-state control (D3): confirmed = outline · arrived = filled emphasis · absent = ruby outline on ruby tint. */
@Composable
fun TriState(state: AttendanceState, onClick: () -> Unit) {
    val m = M
    val fg = when (state) { AttendanceState.ARRIVED -> m.emphasisText; AttendanceState.ABSENT -> m.danger; else -> m.text }
    val bg = when (state) { AttendanceState.ARRIVED -> m.emphasisBg; AttendanceState.ABSENT -> m.dangerTint; else -> Color.Transparent }
    val border = when (state) { AttendanceState.ARRIVED -> Color.Transparent; AttendanceState.ABSENT -> m.danger; else -> m.lineInput }
    Box(Modifier.defaultMinSize(minHeight = Dim.touch).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Row(Modifier.height(28.dp).clip(CircleShape).background(bg).border(1.dp, border, CircleShape).padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
            Icon(when (state) { AttendanceState.ARRIVED -> Icons.Outlined.CheckCircle; AttendanceState.ABSENT -> Icons.Outlined.Cancel; else -> Icons.Outlined.Check }, null, Modifier.size(14.dp), tint = fg)
            Text(state.label, style = Type.caption, color = fg)
        }
    }
}
