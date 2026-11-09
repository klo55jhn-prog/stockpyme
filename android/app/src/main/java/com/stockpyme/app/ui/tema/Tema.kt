package com.stockpyme.app.ui.tema

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Identidad visual de StockPyme: verde azulado (confianza, orden) con acento ámbar (avisos, la cinta del logo).

val Marca = Color(0xFF0F766E)
val MarcaClara = Color(0xFF14B8A6)
val MarcaOscura = Color(0xFF0B4F4A)
val Ambar = Color(0xFFF59E0B)

private val Claro = lightColorScheme(
    primary = Marca,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCCFBF1),
    onPrimaryContainer = Color(0xFF042F2E),
    secondary = Color(0xFF475569),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE2E8F0),
    onSecondaryContainer = Color(0xFF0F172A),
    tertiary = Color(0xFFB45309),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFEF3C7),
    onTertiaryContainer = Color(0xFF451A03),
    error = Color(0xFFDC2626),
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D),
    background = Color(0xFFF6F8F8),
    onBackground = Color(0xFF0F1A1A),
    surface = Color(0xFFF6F8F8),
    onSurface = Color(0xFF0F1A1A),
    surfaceVariant = Color(0xFFE6ECEB),
    onSurfaceVariant = Color(0xFF5B6B69),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color(0xFFF0F4F3),
    surfaceContainerHigh = Color(0xFFEAF0EF),
    surfaceContainerHighest = Color(0xFFE3EAE9),
    outline = Color(0xFFA3B1AF),
    outlineVariant = Color(0xFFDCE4E2),
)

private val Oscuro = darkColorScheme(
    primary = Color(0xFF5EEAD4),
    onPrimary = Color(0xFF00332E),
    primaryContainer = Color(0xFF115E59),
    onPrimaryContainer = Color(0xFFCCFBF1),
    secondary = Color(0xFFCBD5E1),
    onSecondary = Color(0xFF1E293B),
    secondaryContainer = Color(0xFF334155),
    onSecondaryContainer = Color(0xFFE2E8F0),
    tertiary = Color(0xFFFCD34D),
    onTertiary = Color(0xFF422006),
    tertiaryContainer = Color(0xFF713F12),
    onTertiaryContainer = Color(0xFFFEF3C7),
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFEE2E2),
    background = Color(0xFF0B1413),
    onBackground = Color(0xFFE3EBEA),
    surface = Color(0xFF0B1413),
    onSurface = Color(0xFFE3EBEA),
    surfaceVariant = Color(0xFF26332F),
    onSurfaceVariant = Color(0xFF9FB0AD),
    surfaceContainerLowest = Color(0xFF070E0D),
    surfaceContainerLow = Color(0xFF131E1D),
    surfaceContainer = Color(0xFF172322),
    surfaceContainerHigh = Color(0xFF1E2B29),
    surfaceContainerHighest = Color(0xFF263432),
    outline = Color(0xFF5F716E),
    outlineVariant = Color(0xFF2C3A38),
)

private val base = Typography()

private val Tipografia = Typography(
    displayMedium = base.displayMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = (-1).sp),
    headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    titleSmall = base.titleSmall.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold),
)

private val Formas = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
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

val coloresEstado: ColoresEstado
    @Composable @ReadOnlyComposable
    get() = if (isSystemInDarkTheme()) EstadoOscuro else EstadoClaro

/** Color para cantidades que entran (verde) en el historial. */
val ColorEntrada: Color
    @Composable @ReadOnlyComposable
    get() = coloresEstado.ok

@Composable
fun TemaStockPyme(contenido: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) Oscuro else Claro,
        typography = Tipografia,
        shapes = Formas,
        content = contenido,
    )
}
