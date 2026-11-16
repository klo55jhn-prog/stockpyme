package com.stockpyme.app.ui.comun

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Gráfica de barras sencilla con animación. Al tocar una barra se ve su valor encima de la gráfica.
 * Por defecto queda seleccionada la última (el día de hoy).
 *
 * @param valores altura de cada barra.
 * @param etiquetas texto bajo cada barra (puede ser "" para no ponerlo).
 * @param detalle texto que se enseña para la barra seleccionada, por ejemplo "mar 12 · 168,94 €".
 */
@Composable
fun GraficaBarras(
    valores: List<Float>,
    etiquetas: List<String>,
    detalle: (Int) -> String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    altura: Dp = 130.dp,
) {
    if (valores.isEmpty()) return
    var seleccionada by remember(valores.size) { mutableIntStateOf(valores.lastIndex) }
    val maximo = (valores.maxOrNull() ?: 0f).coerceAtLeast(0.0001f)
    val separacion = if (valores.size > 16) 2.dp else 6.dp

    Column(modifier) {
        Text(
            detalle(seleccionada.coerceIn(0, valores.lastIndex)),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.fillMaxWidth().height(altura).padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(separacion),
            verticalAlignment = Alignment.Bottom,
        ) {
            valores.forEachIndexed { i, valor ->
                val fraccion by animateFloatAsState(
                    targetValue = (valor / maximo).coerceIn(0.03f, 1f),
                    animationSpec = tween(durationMillis = 500),
                    label = "barra",
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { seleccionada = i },
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(fraccion)
                            .background(
                                if (i == seleccionada) color else color.copy(alpha = 0.3f),
                                RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 2.dp, bottomEnd = 2.dp),
                            ),
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(separacion),
        ) {
            etiquetas.forEachIndexed { i, texto ->
                Text(
                    texto,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (i == seleccionada) FontWeight.Bold else FontWeight.Normal,
                    color = if (i == seleccionada) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
