package ma.salat.maroc.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import ma.salat.maroc.data.ThemeMode

/** Vert profond + or : contrastes élevés (lisibilité pour les personnes âgées). */
private val LightColors = lightColorScheme(
    primary = Color(0xFF0B6E4F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCDEBDD),
    onPrimaryContainer = Color(0xFF002114),
    secondary = Color(0xFF7A5C12),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFBE7B5),
    onSecondaryContainer = Color(0xFF2B1E00),
    background = Color(0xFFFAFAF6),
    onBackground = Color(0xFF1A1C1A),
    surface = Color(0xFFFAFAF6),
    onSurface = Color(0xFF1A1C1A),
    surfaceVariant = Color(0xFFE3EBE6),
    onSurfaceVariant = Color(0xFF3F4944),
    outline = Color(0xFF6F7973),
    error = Color(0xFFB3261E),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF7FD6AE),
    onPrimary = Color(0xFF00382A),
    primaryContainer = Color(0xFF005139),
    onPrimaryContainer = Color(0xFFCDEBDD),
    secondary = Color(0xFFE8C170),
    onSecondary = Color(0xFF412D00),
    secondaryContainer = Color(0xFF5A4300),
    onSecondaryContainer = Color(0xFFFBE7B5),
    background = Color(0xFF101412),
    onBackground = Color(0xFFE1E3DF),
    surface = Color(0xFF101412),
    onSurface = Color(0xFFE1E3DF),
    surfaceVariant = Color(0xFF26302B),
    onSurfaceVariant = Color(0xFFBFC9C2),
    outline = Color(0xFF89938D),
    error = Color(0xFFF2B8B5),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
)

/** Tailles en sp : elles suivent la taille de police choisie dans le téléphone. */
private val AppTypography = Typography().let { t ->
    t.copy(
        displayLarge = t.displayLarge.copy(fontSize = 56.sp, lineHeight = 64.sp, fontWeight = FontWeight.Bold),
        displayMedium = t.displayMedium.copy(fontSize = 44.sp, lineHeight = 52.sp, fontWeight = FontWeight.Bold),
        displaySmall = t.displaySmall.copy(fontSize = 36.sp, lineHeight = 44.sp, fontWeight = FontWeight.Bold),
        headlineMedium = t.headlineMedium.copy(fontSize = 30.sp, lineHeight = 38.sp, fontWeight = FontWeight.SemiBold),
        headlineSmall = t.headlineSmall.copy(fontSize = 26.sp, lineHeight = 34.sp, fontWeight = FontWeight.SemiBold),
        titleLarge = t.titleLarge.copy(fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.SemiBold),
        titleMedium = t.titleMedium.copy(fontSize = 21.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold),
        bodyLarge = TextStyle(fontSize = 20.sp, lineHeight = 28.sp),
        bodyMedium = TextStyle(fontSize = 18.sp, lineHeight = 26.sp),
        bodySmall = TextStyle(fontSize = 16.sp, lineHeight = 22.sp),
        labelLarge = t.labelLarge.copy(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold),
        labelMedium = t.labelMedium.copy(fontSize = 16.sp, lineHeight = 22.sp),
    )
}

@Composable
fun SalatTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.AUTO -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !dark
            controller.isAppearanceLightNavigationBars = !dark
        }
    }
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = AppTypography,
        content = content,
    )
}
