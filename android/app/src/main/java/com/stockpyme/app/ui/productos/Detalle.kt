package com.stockpyme.app.ui.productos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Euro
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stockpyme.app.datos.Movimiento
import com.stockpyme.app.datos.MovimientoRequest
import com.stockpyme.app.datos.Producto
import com.stockpyme.app.datos.Repositorio
import com.stockpyme.app.datos.TipoMovimiento
import com.stockpyme.app.datos.Usuario
import com.stockpyme.app.ui.comun.EstadoLista
import com.stockpyme.app.ui.comun.EstadoStock
import com.stockpyme.app.ui.comun.FilaTarjeta
import com.stockpyme.app.ui.comun.IconoRedondo
import com.stockpyme.app.ui.comun.MostrarMensaje
import com.stockpyme.app.ui.comun.Pantalla
import com.stockpyme.app.ui.comun.TituloSeccion
import com.stockpyme.app.ui.comun.dinero
import com.stockpyme.app.ui.comun.estadoStock
import com.stockpyme.app.ui.comun.fechaLocal
import com.stockpyme.app.ui.comun.horaLocal
import com.stockpyme.app.ui.comun.repositorio
import com.stockpyme.app.ui.tema.MarcaClara
import com.stockpyme.app.ui.tema.MarcaOscura
import com.stockpyme.app.ui.tema.coloresEstado
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode

/** Ficha de producto: datos, historial (CU-10) y registro de movimientos (CU-07, CU-08, CU-09). */
class DetalleViewModel(private val repo: Repositorio, private val productoId: Long) : ViewModel() {

    var producto by mutableStateOf<Producto?>(null)
        private set
    var movimientos by mutableStateOf<List<Movimiento>>(emptyList())
        private set
    var cargando by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var guardando by mutableStateOf(false)
        private set
    var mensaje by mutableStateOf<String?>(null)

    fun cargar() {
        viewModelScope.launch {
            cargando = true
            repo.producto(productoId)
                .onSuccess {
                    producto = it
                    error = null
                }
                .onFailure { error = it.message }
            repo.historial(productoId).onSuccess { movimientos = it }
            cargando = false
        }
    }

    /** Devuelve true en [alTerminar] si el servidor aceptó el movimiento. */
    fun registrar(peticion: MovimientoRequest, alTerminar: (Boolean) -> Unit) {
        guardando = true
        viewModelScope.launch {
            repo.registrarMovimiento(productoId, peticion)
                .onSuccess { actualizado ->
                    producto = actualizado
                    val nombre = when (peticion.tipo) {
                        TipoMovimiento.ENTRADA -> "Entrada registrada"
                        TipoMovimiento.SALIDA -> "Salida registrada"
                        else -> "Ajuste registrado"
                    }
                    mensaje = "$nombre. Stock actual: ${actualizado.stockActual} u."
                    repo.historial(productoId).onSuccess { movimientos = it }
                    alTerminar(true)
                }
                .onFailure {
                    mensaje = it.message
                    alTerminar(false)
                }
            guardando = false
        }
    }
}

@Composable
fun PantallaDetalle(productoId: Long, usuario: Usuario, onAtras: () -> Unit, onEditar: () -> Unit) {
    val repo = repositorio()
    val vm: DetalleViewModel = viewModel { DetalleViewModel(repo, productoId) }
    val snackbar = remember { SnackbarHostState() }
    var dialogo by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { vm.cargar() }
    MostrarMensaje(vm.mensaje, snackbar) { vm.mensaje = null }

    val producto = vm.producto
    Pantalla(
        titulo = producto?.nombre ?: "Producto",
        onAtras = onAtras,
        snackbar = snackbar,
        acciones = {
            if (usuario.esPropietario && producto != null) {
                IconButton(onClick = onEditar) { Icon(Icons.Filled.Edit, contentDescription = "Editar producto") }
            }
        },
    ) { margenes ->
        EstadoLista(
            cargando = vm.cargando,
            error = vm.error,
            vacia = producto == null,
            textoVacio = "",
            onReintentar = vm::cargar,
            modifier = Modifier.padding(margenes),
        ) {
            if (producto != null) {
                LazyColumn(
                    modifier = Modifier.padding(margenes).fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item { Cabecera(producto) }
                    item {
                        if (producto.activo) {
                            BotonesMovimiento(usuario.esPropietario) { dialogo = it }
                        } else {
                            Text(
                                "Producto desactivado: no admite movimientos.",
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(vertical = 8.dp),
                            )
                        }
                    }
                    item { Datos(producto) }
                    item { TituloSeccion("Historial de movimientos") }
                    if (vm.movimientos.isEmpty()) {
                        item {
                            Text(
                                "Sin movimientos todavía. Registra una entrada para añadir el stock inicial.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(4.dp),
                            )
                        }
                    }
                    items(vm.movimientos, key = { it.id }) { m ->
                        FilaMovimiento(m, mostrarProducto = false)
                    }
                }
            }
        }
    }

    val tipo = dialogo
    if (tipo != null && producto != null) {
        DialogoMovimiento(
            tipo = tipo,
            producto = producto,
            guardando = vm.guardando,
            onCancelar = { dialogo = null },
            onConfirmar = { peticion -> vm.registrar(peticion) { ok -> if (ok) dialogo = null } },
        )
    }
}

/** Tarjeta grande con el stock: verde de marca si todo va bien, ámbar o roja si hay que reponer. */
@Composable
private fun Cabecera(p: Producto) {
    val estado = p.estadoStock()
    val degradado = when (estado) {
        EstadoStock.OK -> listOf(MarcaClara, MarcaOscura)
        EstadoStock.BAJO -> listOf(Color(0xFFF59E0B), Color(0xFFB45309))
        EstadoStock.AGOTADO -> listOf(Color(0xFFEF4444), Color(0xFF991B1B))
        EstadoStock.INACTIVO -> listOf(Color(0xFF94A3B8), Color(0xFF475569))
    }
    Surface(shape = MaterialTheme.shapes.extraLarge, modifier = Modifier.fillMaxWidth()) {
        Column(
            Modifier
                .background(Brush.linearGradient(degradado))
                .padding(20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    p.categoriaNombre.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.weight(1f),
                )
                Text(
                    estado.texto,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.2f), MaterialTheme.shapes.small)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    "${p.stockActual}",
                    style = MaterialTheme.typography.displayMedium,
                    color = Color.White,
                )
                Text(
                    if (p.stockActual == 1) " unidad" else " unidades",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            Text(
                "Aviso cuando queden ${p.stockMinimo} o menos",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f),
            )
        }
    }
}

@Composable
private fun Datos(p: Producto) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shadowElevation = 0.5.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(vertical = 8.dp)) {
            Dato(Icons.Outlined.Sell, "Precio de venta", dinero(p.precioVenta))
            Dato(Icons.Outlined.ShoppingCart, "Precio de compra", dinero(p.precioCompra))
            margen(p)?.let { Dato(Icons.Outlined.Euro, "Margen", it) }
            HorizontalDivider(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)
            Dato(Icons.Outlined.Category, "Categoría", p.categoriaNombre)
            Dato(Icons.Outlined.LocalShipping, "Proveedor", p.proveedorNombre ?: "Sin proveedor")
            Dato(Icons.Outlined.QrCode2, "Código de barras", p.codigoBarras ?: "Sin código")
        }
    }
}

/** Margen bruto sobre el precio de venta, por ejemplo "4,50 € (45 %)". */
private fun margen(p: Producto): String? {
    if (p.precioVenta.signum() <= 0) return null
    val ganancia = p.precioVenta - p.precioCompra
    val porcentaje = ganancia.multiply(BigDecimal(100)).divide(p.precioVenta, 0, RoundingMode.HALF_UP)
    return "${dinero(ganancia)} ($porcentaje %)"
}

@Composable
private fun Dato(icono: ImageVector, etiqueta: String, valor: String) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icono, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(14.dp))
        Text(etiqueta, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(valor, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End)
    }
}

/** RNF-05: registrar un movimiento habitual lleva dos toques (botón + confirmar). */
@Composable
private fun BotonesMovimiento(esPropietario: Boolean, onElegir: (String) -> Unit) {
    val c = coloresEstado
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Button(
            onClick = { onElegir(TipoMovimiento.ENTRADA) },
            modifier = Modifier.weight(1f).height(52.dp),
            shape = MaterialTheme.shapes.medium,
            contentPadding = PaddingValues(horizontal = 8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = c.ok, contentColor = Color.White),
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, Modifier.size(20.dp))
            Spacer(Modifier.width(4.dp))
            Text("Entrada", maxLines = 1)
        }
        Button(
            onClick = { onElegir(TipoMovimiento.SALIDA) },
            modifier = Modifier.weight(1f).height(52.dp),
            shape = MaterialTheme.shapes.medium,
            contentPadding = PaddingValues(horizontal = 8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = c.peligro, contentColor = Color.White),
        ) {
            Icon(Icons.Filled.Remove, contentDescription = null, Modifier.size(20.dp))
            Spacer(Modifier.width(4.dp))
            Text("Salida", maxLines = 1)
        }
        if (esPropietario) {
            OutlinedButton(
                onClick = { onElegir(TipoMovimiento.AJUSTE) },
                modifier = Modifier.weight(1f).height(52.dp),
                shape = MaterialTheme.shapes.medium,
                contentPadding = PaddingValues(horizontal = 8.dp),
            ) {
                Icon(Icons.Filled.Tune, contentDescription = null, Modifier.size(20.dp))
                Spacer(Modifier.width(4.dp))
                Text("Ajuste", maxLines = 1)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DialogoMovimiento(
    tipo: String,
    producto: Producto,
    guardando: Boolean,
    onCancelar: () -> Unit,
    onConfirmar: (MovimientoRequest) -> Unit,
) {
    val esAjuste = tipo == TipoMovimiento.AJUSTE
    var numero by remember { mutableStateOf(if (esAjuste) producto.stockActual.toString() else "1") }
    var motivo by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    val titulo = when (tipo) {
        TipoMovimiento.ENTRADA -> "Registrar entrada"
        TipoMovimiento.SALIDA -> "Registrar salida"
        else -> "Ajustar stock"
    }
    val sugerencias = when (tipo) {
        TipoMovimiento.ENTRADA -> listOf("Compra a proveedor", "Devolución de cliente")
        TipoMovimiento.SALIDA -> listOf("Venta", "Merma", "Rotura")
        else -> listOf("Recuento", "Rotura", "Pérdida")
    }
    val c = coloresEstado
    val (icono, color) = when (tipo) {
        TipoMovimiento.ENTRADA -> Icons.Filled.Add to c.ok
        TipoMovimiento.SALIDA -> Icons.Filled.Remove to c.peligro
        else -> Icons.Filled.Tune to MaterialTheme.colorScheme.primary
    }

    fun cambiar(delta: Int) {
        val actual = numero.toIntOrNull() ?: 0
        numero = (actual + delta).coerceAtLeast(if (esAjuste) 0 else 1).toString()
    }

    AlertDialog(
        onDismissRequest = { if (!guardando) onCancelar() },
        icon = { IconoRedondo(icono, color, tamano = 48.dp) },
        title = { Text(titulo) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "${producto.nombre}\nStock actual: ${producto.stockActual} u.",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    if (esAjuste) "Stock real contado" else "Cantidad",
                    style = MaterialTheme.typography.labelLarge,
                )
                // Selector de cantidad con botones - y +
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilledTonalIconButton(onClick = { cambiar(-1) }, enabled = !guardando) {
                        Icon(Icons.Filled.Remove, contentDescription = "Uno menos")
                    }
                    OutlinedTextField(
                        value = numero,
                        onValueChange = { numero = it.filter(Char::isDigit).take(7) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.headlineSmall.copy(textAlign = TextAlign.Center),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.width(110.dp),
                    )
                    FilledTonalIconButton(onClick = { cambiar(1) }, enabled = !guardando) {
                        Icon(Icons.Filled.Add, contentDescription = "Uno más")
                    }
                }
                OutlinedTextField(
                    value = motivo,
                    onValueChange = { motivo = it.take(255) },
                    label = { Text(if (esAjuste) "Motivo (obligatorio)" else "Motivo (opcional)") },
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    sugerencias.forEach { s ->
                        SuggestionChip(onClick = { motivo = s }, label = { Text(s) })
                    }
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            Button(
                enabled = !guardando,
                onClick = {
                    val valor = numero.toIntOrNull()
                    error = when {
                        valor == null -> "Escribe un número"
                        !esAjuste && valor <= 0 -> "La cantidad debe ser mayor que 0"
                        tipo == TipoMovimiento.SALIDA && valor > producto.stockActual ->
                            "Solo quedan ${producto.stockActual} unidades"
                        esAjuste && motivo.isBlank() -> "Indica el motivo del ajuste"
                        esAjuste && valor == producto.stockActual -> "El stock ya es $valor"
                        else -> null
                    }
                    if (error == null && valor != null) {
                        val texto = motivo.trim().ifBlank { null }
                        onConfirmar(
                            if (esAjuste) MovimientoRequest(tipo, stockNuevo = valor, motivo = texto)
                            else MovimientoRequest(tipo, cantidad = valor, motivo = texto),
                        )
                    }
                },
            ) {
                if (guardando) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Confirmar")
                }
            }
        },
        dismissButton = { TextButton(onClick = onCancelar, enabled = !guardando) { Text("Cancelar") } },
    )
}

/** Una línea del historial. Se usa en la ficha del producto y en la pestaña Actividad. */
@Composable
fun FilaMovimiento(m: Movimiento, mostrarProducto: Boolean, onClick: (() -> Unit)? = null) {
    val c = coloresEstado
    val (icono, color) = when {
        m.tipo == TipoMovimiento.AJUSTE -> Icons.Filled.Tune to MaterialTheme.colorScheme.primary
        m.cantidad > 0 -> Icons.Filled.Add to c.ok
        else -> Icons.Filled.Remove to c.peligro
    }
    val tipo = when (m.tipo) {
        TipoMovimiento.ENTRADA -> "Entrada"
        TipoMovimiento.SALIDA -> "Salida"
        else -> "Ajuste"
    }
    FilaTarjeta(
        onClick = onClick,
        inicio = { IconoRedondo(icono, color) },
        titulo = { Text(if (mostrarProducto) m.productoNombre else tipo, fontWeight = FontWeight.SemiBold) },
        subtitulo = {
            val partes = buildList {
                if (mostrarProducto) add(tipo)
                add(m.usuarioNombre)
                add(if (mostrarProducto) horaLocal(m.fecha) else fechaLocal(m.fecha))
            }
            Column {
                Text(partes.joinToString(" · "))
                m.motivo?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }
            }
        },
        fin = {
            Text(
                (if (m.cantidad > 0) "+" else "") + m.cantidad,
                color = color,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        },
    )
}
