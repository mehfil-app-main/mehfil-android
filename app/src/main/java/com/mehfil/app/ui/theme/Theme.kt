package com.mehfil.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.mehfil.app.R
import com.mehfil.app.model.EventHue

// Source of truth: tokens.css; the reasoning lives in NOTES.md. Palette primitives are the only hues in the
// app; everything else is an alias or an alpha of one.
//
// The system in brief: ground and surface are both white and structure comes from hairlines and 14dp clipping;
// rani is the single accent, spent only on filled CTAs, the FAB, the selected tab, save/assign, the hero bar,
// event 1 and chart bars (links, nav and date selection are ink); one shadow tier, reserved for things that float; buttons and inputs are 8dp, cards 14dp, sheets 20dp,
// chips and avatars pill.

object Palette {
    /** Rani pink — the accent. White text on it is 4.84:1, so filled buttons pass AA. */
    val rani = Color(0xFFD6246E)
    val raniPress = Color(0xFFB81F5F)
    val raniPale = Color(0xFFF6CFDF)
    val error = Color(0xFFC13515)
    val plum = Color(0xFF5B2C83)
    val forest = Color(0xFF2E6B4F)
    val ink = Color(0xFF222222)
    val body = Color(0xFF3F3F3F)
    val muted = Color(0xFF6A6A6A)
    val mutedSoft = Color(0xFF929292)
    val hairline = Color(0xFFDDDDDD)
    val hairlineSoft = Color(0xFFEBEBEB)
    val borderStrong = Color(0xFFC1C1C1)
    val canvas = Color.White
    val surfaceSoft = Color(0xFFF7F7F7)
    val surfaceStrong = Color(0xFFF2F2F2)
    val onPrimary = Color.White
    val onDark = Color.White
    val azure = Color(0xFF428BFF)

    /**
     * Avatar hues. Eight colours that all carry white text at 6:1 or better and stay distinguishable from
     * each other and from the status colours. Assigned by hashing the person's name, so the same client keeps
     * the same tile on every device (see Initials). Identical to the iOS list: rose, berry, plum, blue, teal,
     * amber, slate, ink.
     */
    val avatarHues = listOf(
        Color(0xFFB81F5F), Color(0xFF7E2349), Color(0xFF5B2C83), Color(0xFF225A9E),
        Color(0xFF1C665C), Color(0xFF734D21), Color(0xFF4D596B), Color(0xFF222222),
    )

    // DERIVED dark values: any hue below 4.5:1 on ink, mixed toward canvas until it passes.
    val darkSurface = Color(0xFF2F2F2F)
    val darkAccent = Color(0xFFE573A2)       // rani + 36% canvas (5.5:1 on ink, 4.7:1 on dark surface)
    val darkAccentPress = Color(0xFFD26B95)  // darkAccent 10% toward ink (ink text 4.8:1)
    val darkDanger = Color(0xFFD4725B)
    val darkWarn = Color(0xFFA68DBC)         // plum + 46% canvas
    val darkForest = Color(0xFF79A08E)       // forest + 36% canvas
}

/** Semantic aliases, resolved once per theme. */
data class MColors(
    val ground: Color, val surface: Color, val surfacePressed: Color, val surfaceSelected: Color,
    val text: Color, val textSecondary: Color, val textMute: Color, val textOnPrimary: Color,
    val line: Color, val lineInput: Color,
    val fillSoft: Color, val fillStrong: Color, val lineSoft: Color,
    val accent: Color, val accentPress: Color, val accentText: Color, val accentTint: Color, val accentPale: Color, val accentSoftBg: Color, val accentSoftText: Color,
    val settledBg: Color, val settledText: Color,
    val danger: Color, val dangerTint: Color, val warn: Color, val warnTint: Color,
    val disabledBg: Color, val disabledText: Color,
    val toastBg: Color, val toastText: Color, val toastSuccess: Color, val toastError: Color, val toastInfo: Color,
    val scrim: Color, val emphasisBg: Color, val emphasisText: Color, val segmentSelected: Color, val segmentTrack: Color,
    val event1: Color, val event2: Color, val event3: Color, val event4: Color,
    val shadow: Color, val isDark: Boolean,
) {
    fun event(h: EventHue) = when (h) { EventHue.ONE -> event1; EventHue.TWO -> event2; EventHue.THREE -> event3; EventHue.FOUR -> event4 }
}

val LightColors = MColors(
    ground = Palette.canvas, surface = Palette.canvas, surfacePressed = Palette.surfaceSoft, surfaceSelected = Palette.rani.copy(alpha = 0.08f),
    text = Palette.ink, textSecondary = Palette.body, textMute = Palette.muted, textOnPrimary = Palette.onPrimary,
    line = Palette.hairline, lineInput = Palette.hairline,
    fillSoft = Palette.surfaceSoft, fillStrong = Palette.surfaceStrong, lineSoft = Palette.hairlineSoft,
    // accentText is deliberately ink, not the accent: inline links are ink with an underline, so the accent stays rare.
    accent = Palette.rani, accentPress = Palette.raniPress, accentText = Palette.ink, accentTint = Palette.rani.copy(alpha = 0.08f),
    accentPale = Palette.raniPale, accentSoftBg = Palette.surfaceStrong, accentSoftText = Palette.ink,
    settledBg = Palette.ink, settledText = Palette.onDark,
    danger = Palette.error, dangerTint = Palette.error.copy(alpha = 0.08f), warn = Palette.plum, warnTint = Palette.plum.copy(alpha = 0.08f),
    disabledBg = Palette.raniPale, disabledText = Palette.onPrimary,
    toastBg = Palette.ink, toastText = Palette.onDark, toastSuccess = Palette.onDark, toastError = Palette.rani, toastInfo = Palette.mutedSoft,
    scrim = Color.Black.copy(alpha = 0.5f), emphasisBg = Palette.ink, emphasisText = Palette.onDark,
    segmentSelected = Palette.canvas, segmentTrack = Palette.surfaceStrong,
    event1 = Palette.rani, event2 = Palette.forest, event3 = Palette.azure, event4 = Palette.ink,
    shadow = Color.Black.copy(alpha = 0.10f), isDark = false,
)

val DarkColors = MColors(
    ground = Palette.ink, surface = Palette.darkSurface, surfacePressed = Color.White.copy(alpha = 0.06f), surfaceSelected = Palette.rani.copy(alpha = 0.16f),
    // Ink, not white: the lifted accent is too light for white text (2.9:1), and white on the dark danger fill fails too (3.3:1).
    text = Palette.onDark, textSecondary = Palette.hairline, textMute = Palette.mutedSoft, textOnPrimary = Palette.ink,
    line = Color.White.copy(alpha = 0.14f), lineInput = Color.White.copy(alpha = 0.28f),
    fillSoft = Color.White.copy(alpha = 0.06f), fillStrong = Color.White.copy(alpha = 0.10f), lineSoft = Color.White.copy(alpha = 0.08f),
    accent = Palette.darkAccent, accentPress = Palette.darkAccentPress, accentText = Palette.onDark, accentTint = Palette.rani.copy(alpha = 0.16f),
    accentPale = Palette.rani.copy(alpha = 0.42f), accentSoftBg = Color.White.copy(alpha = 0.10f), accentSoftText = Palette.onDark,
    settledBg = Palette.canvas, settledText = Palette.ink,
    danger = Palette.darkDanger, dangerTint = Palette.error.copy(alpha = 0.18f), warn = Palette.darkWarn, warnTint = Palette.plum.copy(alpha = 0.30f),
    disabledBg = Color.White.copy(alpha = 0.10f), disabledText = Color.White.copy(alpha = 0.45f),
    toastBg = Palette.canvas, toastText = Palette.ink, toastSuccess = Palette.ink, toastError = Palette.error, toastInfo = Palette.muted,
    scrim = Color.Black.copy(alpha = 0.5f), emphasisBg = Palette.canvas, emphasisText = Palette.ink,
    segmentSelected = Color.White.copy(alpha = 0.18f), segmentTrack = Color.White.copy(alpha = 0.08f),
    event1 = Palette.darkAccent, event2 = Palette.darkForest, event3 = Palette.azure, event4 = Palette.hairline,
    shadow = Color.Black.copy(alpha = 0.40f), isDark = true,
)

val LocalMColors = staticCompositionLocalOf { LightColors }
val M: MColors @Composable get() = LocalMColors.current

object Space { val xxs = 2.dp; val xs = 4.dp; val sm = 8.dp; val md = 12.dp; val lg = 16.dp; val xl = 24.dp; val xxl = 32.dp; val huge = 64.dp }
/** rounded.xs 4 · rounded.sm 8 (buttons, inputs — no longer pills) · rounded.md 14 (cards, photos) · rounded.lg 20 (sheets). */
object Radius { val xs = 4.dp; val sm = 8.dp; val md = 14.dp; val lg = 14.dp; val xl = 20.dp }
object Dim {
    val touch = 44.dp; val row = 56.dp; val button = 56.dp; val buttonCompact = 48.dp; val input = 56.dp; val amountInput = 72.dp
    val icon = 24.dp; val iconSm = 16.dp; val avatar = 40.dp; val avatarSm = 32.dp; val calendarCell = 48.dp; val calendarNumeral = 32.dp
    val dot = 8.dp; val timelineHour = 32.dp; val timelineGutter = 48.dp; val bar = 8.dp; val edge = 4.dp; val keypadKey = 56.dp; val chart = 120.dp; val gutter = 16.dp
}
val CardShape = RoundedCornerShape(Radius.md)     // rounded.md — cards and photos
val FieldShape = RoundedCornerShape(Radius.sm)    // rounded.sm — inputs
val ButtonShape = RoundedCornerShape(Radius.sm)   // rounded.sm — buttons are no longer pills
val SheetShape = RoundedCornerShape(topStart = Radius.xl, topEnd = Radius.xl)

// Type roles (tokens.css). Inter at 400–700: 400 is the lightest weight in the scale, and headings lean on 600/700.
val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
)

private fun role(size: Int, weight: FontWeight, lh: Double, ls: Double, tabular: Boolean = false) = TextStyle(
    fontFamily = Inter, fontSize = size.sp, fontWeight = weight, lineHeight = (size * lh).sp, letterSpacing = ls.sp,
    fontFeatureSettings = if (tabular) "tnum" else null,
)

object Type {
    val displayHero = role(40, FontWeight.Bold, 1.1, -0.63, tabular = true)   // the rupee hero: 64 collides with the chevron at 361pt (NOTES.md)
    val displayLg = role(28, FontWeight.Bold, 1.25, 0.0)                      // E2 title
    val displayMd = role(22, FontWeight.Medium, 1.18, -0.44)                  // screen titles
    val headingLg = role(21, FontWeight.Bold, 1.43, 0.0)
    val headingMd = role(20, FontWeight.SemiBold, 1.2, -0.18)                 // sheet titles
    val headingSm = role(16, FontWeight.SemiBold, 1.25, 0.0)                  // card titles
    val bodyLg = role(16, FontWeight.Normal, 1.5, 0.0)                        // E2 and lead copy
    val bodyMd = role(14, FontWeight.Normal, 1.43, 0.0)                       // default UI text
    val bodyMdStrong = role(16, FontWeight.SemiBold, 1.25, 0.0)               // a row's primary label
    val bodyTabular = role(14, FontWeight.Normal, 1.43, 0.0, tabular = true)
    val bodyTabularStrong = role(14, FontWeight.Medium, 1.29, 0.0, tabular = true)
    val buttonMd = role(16, FontWeight.Medium, 1.25, 0.0)
    val buttonSm = role(14, FontWeight.Medium, 1.29, 0.0)
    val caption = role(13, FontWeight.Normal, 1.23, 0.0)                      // the load-bearing floor
    val micro = role(12, FontWeight.Bold, 1.33, 0.0)
    val microCap = role(11, FontWeight.SemiBold, 1.18, 0.0)                   // tab labels, eyebrows only
}

@Composable
fun MehfilTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (dark) DarkColors else LightColors
    val scheme = if (dark) darkColorScheme(
        primary = Palette.darkAccent, onPrimary = Palette.ink, background = Palette.ink, onBackground = Palette.onDark,
        surface = Palette.darkSurface, onSurface = Palette.onDark, surfaceVariant = Palette.darkSurface, onSurfaceVariant = Palette.mutedSoft,
        outline = colors.line, secondary = Palette.onDark, error = Palette.darkDanger, surfaceContainer = Palette.darkSurface, surfaceContainerLow = Palette.darkSurface,
        surfaceContainerHigh = Palette.darkSurface, surfaceContainerHighest = Palette.darkSurface,
    ) else lightColorScheme(
        primary = Palette.rani, onPrimary = Palette.onPrimary, background = Palette.canvas, onBackground = Palette.ink,
        surface = Palette.canvas, onSurface = Palette.ink, surfaceVariant = Palette.surfaceSoft, onSurfaceVariant = Palette.muted,
        outline = Palette.hairline, secondary = Palette.ink, error = Palette.error, surfaceContainer = Palette.canvas, surfaceContainerLow = Palette.canvas,
        surfaceContainerHigh = Palette.canvas, surfaceContainerHighest = Palette.surfaceSoft,
    )
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !dark
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !dark
        }
    }
    CompositionLocalProvider(LocalMColors provides colors) {
        MaterialTheme(colorScheme = scheme, typography = androidx.compose.material3.Typography(bodyLarge = Type.bodyMd, bodyMedium = Type.bodyMd, labelLarge = Type.buttonMd, titleMedium = Type.headingSm), content = content)
    }
}

/** Level-1 card elevation as a soft shadow. */
val cardElevation: Dp = 1.dp
