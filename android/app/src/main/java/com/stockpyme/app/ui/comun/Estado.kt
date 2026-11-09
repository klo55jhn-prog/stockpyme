package com.stockpyme.app.ui.comun

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.stockpyme.app.datos.Producto
import com.stockpyme.app.ui.tema.coloresEstado

/** Situación del stock de un producto, con su texto y sus colores (RF-09). */
enum class EstadoStock(val texto: String) {
    OK("En stock"),
    BAJO("Stock bajo"),
    AGOTADO("Agotado"),
    INACTIVO("Desactivado"),
}

fun Producto.estadoStock(): EstadoStock = when {
    !activo -> EstadoStock.INACTIVO
    stockActual == 0 -> EstadoStock.AGOTADO
    bajoMinimo -> EstadoStock.BAJO
    else -> EstadoStock.OK
}

/** Devuelve (color del texto, color de fondo) para un estado. */
@Composable
fun colores(estado: EstadoStock): Pair<Color, Color> {
    val c = coloresEstado
    return when (estado) {
        EstadoStock.OK -> c.ok to c.okFondo
        EstadoStock.BAJO -> c.aviso to c.avisoFondo
        EstadoStock.AGOTADO -> c.peligro to c.peligroFondo
        EstadoStock.INACTIVO -> Color.Gray to Color.Gray.copy(alpha = 0.15f)
    }
}

/** Colores de un aviso dentro de un formulario: rojo si es un error, verde si salió bien. */
@Composable
fun coloresEstadoMensaje(esError: Boolean): Pair<Color, Color> {
    val c = coloresEstado
    return if (esError) c.peligro to c.peligroFondo else c.ok to c.okFondo
}

/** Etiqueta con las unidades en stock, coloreada según su estado. */
@Composable
fun PildoraStock(producto: Producto, modifier: Modifier = Modifier) {
    val (color, fondo) = colores(producto.estadoStock())
    Pildora("${producto.stockActual} u.", color, fondo, modifier)
}
