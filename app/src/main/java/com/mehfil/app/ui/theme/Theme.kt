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

// Source of truth: tokens.css. Palette primitives are documented hues; everything else is an alias or an alpha.

object Palette {
    val primary = Color(0xFF533AFD)
    val primaryDeep = Color(0xFF4434D4)
    val primaryPress = Color(0xFF2E2B8C)
    val primarySoft = Color(0xFF665EFD)
    val primarySubdued = Color(0xFFB9B9F9)
    val brandDark = Color(0xFF1C1E54)
    val ink = Color(0xFF0D253D)
    val inkSecondary = Color(0xFF273951)
    val inkMute = Color(0xFF64748D)
    val onPrimary = Color.White
    val canvas = Color.White
    val canvasSoft = Color(0xFFF6F9FC)
    val canvasCream = Color(0xFFF5E9D4)
    val hairline = Color(0xFFE3E8EE)
    val hairlineInput = Color(0xFFA8C3DE)
    val ruby = Color(0xFFEA2261)
    val magenta = Color(0xFFF96BEE)
    val lemon = Color(0xFF9B6829)
    val shadowBlue = Color(0xFF003770)
}

/** Semantic aliases, resolved once per theme. */
data class MColors(
    val ground: Color, val surface: Color, val surfacePressed: Color, val surfaceSelected: Color,
    val text: Color, val textSecondary: Color, val textMute: Color, val textOnPrimary: Color,
    val line: Color, val lineInput: Color,
    val accent: Color, val accentPress: Color, val accentText: Color, val accentTint: Color, val accentSoftBg: Color, val accentSoftText: Color,
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
    ground = Palette.canvasSoft, surface = Palette.canvas, surfacePressed = Palette.canvasSoft, surfaceSelected = Palette.primary.copy(alpha = 0.08f),
    text = Palette.ink, textSecondary = Palette.inkSecondary, textMute = Palette.inkMute, textOnPrimary = Palette.onPrimary,
    line = Palette.hairline, lineInput = Palette.hairlineInput,
    accent = Palette.primary, accentPress = Palette.primaryPress, accentText = Palette.primary, accentTint = Palette.primary.copy(alpha = 0.08f),
    accentSoftBg = Palette.primarySubdued, accentSoftText = Palette.primaryDeep,
    danger = Palette.ruby, dangerTint = Palette.ruby.copy(alpha = 0.08f), warn = Palette.lemon, warnTint = Palette.lemon.copy(alpha = 0.10f),
    disabledBg = Palette.hairline, disabledText = Palette.inkMute,
    toastBg = Palette.brandDark, toastText = Palette.onPrimary, toastSuccess = Palette.primarySubdued, toastError = Palette.ruby, toastInfo = Palette.hairlineInput,
    scrim = Palette.ink.copy(alpha = 0.40f), emphasisBg = Palette.brandDark, emphasisText = Palette.onPrimary,
    segmentSelected = Palette.canvas, segmentTrack = Palette.hairline,
    event1 = Palette.primary, event2 = Palette.lemon, event3 = Palette.magenta, event4 = Palette.brandDark,
    shadow = Palette.shadowBlue.copy(alpha = 0.08f), isDark = false,
)

val DarkColors = MColors(
    ground = Palette.ink, surface = Palette.brandDark, surfacePressed = Color.White.copy(alpha = 0.06f), surfaceSelected = Palette.primarySoft.copy(alpha = 0.18f),
    text = Palette.onPrimary, textSecondary = Palette.hairline, textMute = Palette.hairlineInput, textOnPrimary = Palette.onPrimary,
    line = Color.White.copy(alpha = 0.12f), lineInput = Palette.hairlineInput.copy(alpha = 0.40f),
    accent = Palette.primary, accentPress = Palette.primaryPress, accentText = Palette.primarySubdued, accentTint = Palette.primarySoft.copy(alpha = 0.18f),
    accentSoftBg = Palette.primarySubdued.copy(alpha = 0.16f), accentSoftText = Palette.primarySubdued,
    danger = Palette.ruby, dangerTint = Palette.ruby.copy(alpha = 0.18f), warn = Palette.canvasCream, warnTint = Palette.lemon.copy(alpha = 0.28f),
    disabledBg = Color.White.copy(alpha = 0.08f), disabledText = Palette.hairlineInput.copy(alpha = 0.55f),
    toastBg = Palette.canvas, toastText = Palette.ink, toastSuccess = Palette.primary, toastError = Palette.ruby, toastInfo = Palette.inkMute,
    scrim = Palette.ink.copy(alpha = 0.72f), emphasisBg = Palette.canvas, emphasisText = Palette.ink,
    segmentSelected = Palette.hairlineInput.copy(alpha = 0.22f), segmentTrack = Color.White.copy(alpha = 0.12f),
    event1 = Palette.primary, event2 = Palette.lemon, event3 = Palette.magenta, event4 = Palette.canvasCream,
    shadow = Color.Black.copy(alpha = 0.35f), isDark = true,
)

val LocalMColors = staticCompositionLocalOf { LightColors }
val M: MColors @Composable get() = LocalMColors.current

object Space { val xxs = 2.dp; val xs = 4.dp; val sm = 8.dp; val md = 12.dp; val lg = 16.dp; val xl = 24.dp; val xxl = 32.dp; val huge = 64.dp }
object Radius { val xs = 4.dp; val sm = 6.dp; val md = 8.dp; val lg = 12.dp; val xl = 16.dp }
object Dim {
    val touch = 44.dp; val row = 56.dp; val button = 56.dp; val buttonCompact = 44.dp; val input = 48.dp; val amountInput = 64.dp
    val icon = 24.dp; val iconSm = 16.dp; val avatar = 40.dp; val avatarSm = 32.dp; val calendarCell = 48.dp; val calendarNumeral = 28.dp
    val dot = 8.dp; val timelineHour = 32.dp; val timelineGutter = 48.dp; val bar = 8.dp; val edge = 4.dp; val keypadKey = 56.dp; val chart = 120.dp; val gutter = 16.dp
}
val CardShape = RoundedCornerShape(Radius.lg)
val FieldShape = RoundedCornerShape(Radius.sm)

// Type roles (tokens.css). Inter at 300/400; 400 only on row primary labels, trailing amounts, buttons and captions.
val Inter = FontFamily(
    Font(R.font.inter_light, FontWeight.Light),
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
)

private fun role(size: Int, weight: FontWeight, lh: Double, ls: Double, tabular: Boolean = false) = TextStyle(
    fontFamily = Inter, fontSize = size.sp, fontWeight = weight, lineHeight = (size * lh).sp, letterSpacing = ls.sp,
    fontFeatureSettings = if (tabular) "tnum" else null,
)

object Type {
    val displayHero = role(36, FontWeight.Light, 1.1, -0.72, tabular = true)
    val displayMd = role(26, FontWeight.Light, 1.12, -0.26)
    val headingLg = role(22, FontWeight.Light, 1.1, -0.22)
    val headingMd = role(20, FontWeight.Light, 1.4, -0.2)
    val headingSm = role(18, FontWeight.Light, 1.4, 0.0)
    val bodyLg = role(16, FontWeight.Light, 1.4, 0.0)
    val bodyMd = role(15, FontWeight.Light, 1.4, 0.0)
    val bodyMdStrong = role(15, FontWeight.Normal, 1.4, 0.0)
    val bodyTabular = role(14, FontWeight.Light, 1.4, -0.42, tabular = true)
    val bodyTabularStrong = role(14, FontWeight.Normal, 1.4, -0.42, tabular = true)
    val buttonMd = role(16, FontWeight.Normal, 1.0, 0.0)
    val buttonSm = role(14, FontWeight.Normal, 1.0, 0.0)
    val caption = role(13, FontWeight.Normal, 1.4, -0.39, tabular = true)
    val micro = role(11, FontWeight.Light, 1.4, 0.0)
    val microCap = role(10, FontWeight.Normal, 1.15, 0.1)
}

@Composable
fun MehfilTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (dark) DarkColors else LightColors
    val scheme = if (dark) darkColorScheme(
        primary = Palette.primary, onPrimary = Palette.onPrimary, background = Palette.ink, onBackground = Palette.onPrimary,
        surface = Palette.brandDark, onSurface = Palette.onPrimary, surfaceVariant = Palette.brandDark, onSurfaceVariant = Palette.hairlineInput,
        outline = colors.line, secondary = Palette.primarySubdued, error = Palette.ruby, surfaceContainer = Palette.brandDark, surfaceContainerLow = Palette.brandDark,
        surfaceContainerHigh = Palette.brandDark, surfaceContainerHighest = Palette.brandDark,
    ) else lightColorScheme(
        primary = Palette.primary, onPrimary = Palette.onPrimary, background = Palette.canvasSoft, onBackground = Palette.ink,
        surface = Palette.canvas, onSurface = Palette.ink, surfaceVariant = Palette.canvasSoft, onSurfaceVariant = Palette.inkMute,
        outline = Palette.hairline, secondary = Palette.primaryDeep, error = Palette.ruby, surfaceContainer = Palette.canvas, surfaceContainerLow = Palette.canvas,
        surfaceContainerHigh = Palette.canvas, surfaceContainerHighest = Palette.canvasSoft,
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

/** Level-1 card elevation as a soft shadow (design.md). */
val cardElevation: Dp = 1.dp
