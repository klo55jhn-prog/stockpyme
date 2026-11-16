package com.stockpyme.app.ui.tema

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Identidad visual de StockPyme 3: interfaz neutra en "tinta" (casi negro) sobre fondos claros,
// con un único color de acento (el verde de la marca) reservado para lo importante: vender y los datos.

val Tinta = Color(0xFF0F172A)
val Marca = Color(0xFF0D9488)
val MarcaClara = Color(0xFF2DD4BF)
val MarcaOscura = Color(0xFF0F766E)
val Ambar = Color(0xFFF59E0B)

private val Claro = lightColorScheme(
    primary = Tinta,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8EBF0),
    onPrimaryContainer = Tinta,
    secondary = Color(0xFF475569),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8EBF0),
    onSecondaryContainer = Tinta,
    tertiary = Marca,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFCCFBF1),
    onTertiaryContainer = Color(0xFF134E4A),
    error = Color(0xFFDC2626),
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D),
    background = Color(0xFFF4F5F7),
    onBackground = Tinta,
    surface = Color(0xFFF4F5F7),
    onSurface = Tinta,
    surfaceVariant = Color(0xFFE8EBF0),
    onSurfaceVariant = Color(0xFF64748B),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color(0xFFFFFFFF),
    surfaceContainerHighest = Color(0xFFEEF0F4),
    outline = Color(0xFF94A3B8),
    outlineVariant = Color(0xFFE2E6EC),
)

private val Oscuro = darkColorScheme(
    primary = Color(0xFFF1F5F9),
    onPrimary = Tinta,
    primaryContainer = Color(0xFF232A35),
    onPrimaryContainer = Color(0xFFF1F5F9),
    secondary = Color(0xFFCBD5E1),
    onSecondary = Color(0xFF1E293B),
    secondaryContainer = Color(0xFF232A35),
    onSecondaryContainer = Color(0xFFF1F5F9),
    tertiary = MarcaClara,
    onTertiary = Color(0xFF042F2E),
    tertiaryContainer = Color(0xFF134E4A),
    onTertiaryContainer = Color(0xFFCCFBF1),
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFEE2E2),
    background = Color(0xFF0A0D12),
    onBackground = Color(0xFFE8ECF1),
    surface = Color(0xFF0A0D12),
    onSurface = Color(0xFFE8ECF1),
    surfaceVariant = Color(0xFF1C222B),
    onSurfaceVariant = Color(0xFF94A3B8),
    surfaceContainerLowest = Color(0xFF12161D),
    surfaceContainerLow = Color(0xFF12161D),
    surfaceContainer = Color(0xFF12161D),
    surfaceContainerHigh = Color(0xFF171C24),
    surfaceContainerHighest = Color(0xFF1F2530),
    outline = Color(0xFF526071),
    outlineVariant = Color(0xFF222A35),
)

private val base = Typography()

private val Tipografia = Typography(
    displayMedium = base.displayMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = (-1.5).sp),
    displaySmall = base.displaySmall.copy(fontWeight = FontWeight.Bold, letterSpacing = (-1).sp),
    headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.8).sp),
    headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.6).sp),
    headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.4).sp),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.1).sp),
    titleSmall = base.titleSmall.copy(fontWeight = FontWeight.SemiBold),
    bodyLarge = base.bodyLarge.copy(letterSpacing = 0.sp),
    bodyMedium = base.bodyMedium.copy(letterSpacing = 0.sp),
    labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp),
    labelMedium = base.labelMedium.copy(fontWeight = FontWeight.Medium, letterSpacing = 0.2.sp),
)

private val Formas = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

/** Colores con significado para el stock, que se adaptan al modo claro y oscuro. */
data class ColoresEstado(
    val ok: Color,
    val okFondo: Color,
    val aviso: Color,
    val avisoFondo: Color,
    val peligro: Color,
    val peligroFondo: Color,
)

private val EstadoClaro = ColoresEstado(
    ok = Color(0xFF15803D), okFondo = Color(0xFFDCFCE7),
    aviso = Color(0xFFB45309), avisoFondo = Color(0xFFFEF3C7),
    peligro = Color(0xFFDC2626), peligroFondo = Color(0xFFFEE2E2),
)

private val EstadoOscuro = ColoresEstado(
    ok = Color(0xFF4ADE80), okFondo = Color(0xFF14532D),
    aviso = Color(0xFFFCD34D), avisoFondo = Color(0xFF713F12),
    peligro = Color(0xFFF87171), peligroFondo = Color(0xFF7F1D1D),
)

/** Si la app se está mostrando en oscuro (por el ajuste del usuario o por el del sistema). */
val LocalOscuro = staticCompositionLocalOf { false }

val coloresEstado: ColoresEstado
    @Composable @ReadOnlyComposable
    get() = if (LocalOscuro.current) EstadoOscuro else EstadoClaro

/** Color para cantidades que entran (verde) en el historial. */
val ColorEntrada: Color
    @Composable @ReadOnlyComposable
    get() = coloresEstado.ok

/** [modo] es "sistema", "claro" u "oscuro" (ver Sesion). */
@Composable
fun TemaStockPyme(modo: String = "sistema", contenido: @Composable () -> Unit) {
    val oscuro = when (modo) {
        "oscuro" -> true
        "claro" -> false
        else -> isSystemInDarkTheme()
    }
    CompositionLocalProvider(LocalOscuro provides oscuro) {
        MaterialTheme(
            colorScheme = if (oscuro) Oscuro else Claro,
            typography = Tipografia,
            shapes = Formas,
            content = contenido,
        )
    }
}
