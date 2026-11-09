package com.stockpyme.app.ui.productos

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
import com.stockpyme.app.ui.comun.MostrarMensaje
import com.stockpyme.app.ui.comun.Pantalla
import com.stockpyme.app.ui.comun.dinero
import com.stockpyme.app.ui.comun.fechaLocal
import com.stockpyme.app.ui.comun.repositorio
import com.stockpyme.app.ui.tema.ColorEntrada
import kotlinx.coroutines.launch

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
                LazyColumn(Modifier.padding(margenes).fillMaxSize()) {
                    item { Cabecera(producto) }
                    item {
                        if (producto.activo) {
                            BotonesMovimiento(usuario.esPropietario) { dialogo = it }
                        } else {
                            Text(
                                "Producto desactivado: no admite movimientos.",
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(16.dp),
                            )
                        }
                    }
                    item {
                        Text(
                            "Historial de movimientos",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
                        )
                    }
                    if (vm.movimientos.isEmpty()) {
                        item {
                            Text(
                                "Sin movimientos todavía. Registra una entrada para añadir el stock inicial.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp),
                            )
                        }
                    }
                    items(vm.movimientos, key = { it.id }) { m ->
                        FilaMovimiento(m, mostrarProducto = false)
                        HorizontalDivider()
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

@Composable
private fun Cabecera(p: Producto) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (p.bajoMinimo && p.activo) MaterialTheme.colorScheme.errorContainer
            else MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("${p.stockActual}", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
                Text(" unidades", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
            }
            Text(
                if (p.bajoMinimo) "En el stock mínimo o por debajo (mínimo ${p.stockMinimo})"
                else "Stock mínimo: ${p.stockMinimo}",
            )
            Spacer(Modifier.padding(4.dp))
            Dato("Precio de venta", dinero(p.precioVenta))
            Dato("Precio de compra", dinero(p.precioCompra))
            Dato("Categoría", p.categoriaNombre)
            Dato("Proveedor", p.proveedorNombre ?: "Sin proveedor")
            Dato("Código de barras", p.codigoBarras ?: "Sin código")
        }
    }
}

@Composable
private fun Dato(etiqueta: String, valor: String) {
    Row {
        Text("$etiqueta: ", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(valor, fontWeight = FontWeight.Medium)
    }
}

/** RNF-05: registrar un movimiento habitual lleva dos toques (botón + confirmar). */
@Composable
private fun BotonesMovimiento(esPropietario: Boolean, onElegir: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Button(
            onClick = { onElegir(TipoMovimiento.ENTRADA) },
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ColorEntrada),
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Text("Entrada", maxLines = 1)
        }
        Button(
            onClick = { onElegir(TipoMovimiento.SALIDA) },
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
        ) {
            Icon(Icons.Filled.Remove, contentDescription = null)
            Text("Salida", maxLines = 1)
        }
        if (esPropietario) {
            OutlinedButton(
                onClick = { onElegir(TipoMovimiento.AJUSTE) },
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 8.dp),
            ) {
                Icon(Icons.Filled.Tune, contentDescription = null)
                Text("Ajuste", maxLines = 1)
            }
        }
    }
}

@Composable
private fun DialogoMovimiento(
    tipo: String,
    producto: Producto,
    guardando: Boolean,
    onCancelar: () -> Unit,
    onConfirmar: (MovimientoRequest) -> Unit,
) {
    var numero by remember { mutableStateOf(if (tipo == TipoMovimiento.AJUSTE) "" else "1") }
    var motivo by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val esAjuste = tipo == TipoMovimiento.AJUSTE

    val titulo = when (tipo) {
        TipoMovimiento.ENTRADA -> "Registrar entrada"
        TipoMovimiento.SALIDA -> "Registrar salida"
        else -> "Ajustar stock"
    }

    AlertDialog(
        onDismissRequest = { if (!guardando) onCancelar() },
        title = { Text(titulo) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${producto.nombre}\nStock actual: ${producto.stockActual} u.")
                OutlinedTextField(
                    value = numero,
                    onValueChange = { numero = it.filter(Char::isDigit).take(7) },
                    label = { Text(if (esAjuste) "Stock real contado" else "Cantidad") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = motivo,
                    onValueChange = { motivo = it.take(255) },
                    label = {
                        Text(
                            when (tipo) {
                                TipoMovimiento.AJUSTE -> "Motivo (obligatorio)"
                                TipoMovimiento.SALIDA -> "Motivo (opcional: venta, merma...)"
                                else -> "Motivo (opcional: compra, devolución...)"
                            },
                        )
                    },
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(
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
                if (guardando) CircularProgressIndicator(Modifier.width(20.dp), strokeWidth = 2.dp) else Text("Confirmar")
            }
        },
        dismissButton = { TextButton(onClick = onCancelar, enabled = !guardando) { Text("Cancelar") } },
    )
}

/** Una línea del historial. Se usa en la ficha del producto y en la pestaña Actividad. */
@Composable
fun FilaMovimiento(m: Movimiento, mostrarProducto: Boolean, onClick: (() -> Unit)? = null) {
    val color = when {
        m.cantidad > 0 -> ColorEntrada
        else -> MaterialTheme.colorScheme.error
    }
    val tipo = when (m.tipo) {
        TipoMovimiento.ENTRADA -> "Entrada"
        TipoMovimiento.SALIDA -> "Salida"
        else -> "Ajuste"
    }
    ListItem(
        modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
        headlineContent = { Text(if (mostrarProducto) m.productoNombre else tipo, fontWeight = FontWeight.Medium) },
        supportingContent = {
            val partes = buildList {
                if (mostrarProducto) add(tipo)
                add(m.usuarioNombre)
                add(fechaLocal(m.fecha))
            }
            Column {
                Text(partes.joinToString(" · "))
                m.motivo?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            }
        },
        trailingContent = {
            Text(
                (if (m.cantidad > 0) "+" else "") + m.cantidad,
                color = color,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        },
    )
}
