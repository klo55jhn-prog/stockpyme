package com.stockpyme.app.ui.reposicion

import com.stockpyme.app.ui.comun.bordeTarjeta
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stockpyme.app.datos.LineaReposicion
import com.stockpyme.app.datos.Repositorio
import com.stockpyme.app.ui.comun.EstadoLista
import com.stockpyme.app.ui.comun.Pantalla
import com.stockpyme.app.ui.comun.compartirTexto
import com.stockpyme.app.ui.comun.dinero
import com.stockpyme.app.ui.comun.repositorio
import kotlinx.coroutines.launch
import java.math.BigDecimal

/** Un grupo de líneas de pedido que van al mismo proveedor (o a ninguno). */
class GrupoProveedor(val nombre: String?, val telefono: String?, val email: String?, val lineas: List<LineaReposicion>)

/** Lista de la compra: lo que está por debajo del mínimo, agrupado por proveedor, con cantidades editables. */
class ReposicionViewModel(private val repo: Repositorio) : ViewModel() {
    var lineas by mutableStateOf<List<LineaReposicion>>(emptyList())
        private set
    var cantidades by mutableStateOf<Map<Long, Int>>(emptyMap())
        private set
    var cargando by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    fun cargar() {
        viewModelScope.launch {
            cargando = true
            repo.reposicion()
                .onSuccess {
                    lineas = it
                    cantidades = it.associate { l -> l.productoId to l.sugerido }
                    error = null
                }
                .onFailure { error = it.message }
            cargando = false
        }
    }

    fun cantidad(l: LineaReposicion): Int = cantidades[l.productoId] ?: l.sugerido

    fun cambiar(l: LineaReposicion, nueva: Int) {
        cantidades = cantidades + (l.productoId to nueva.coerceIn(1, 100000))
    }

    fun coste(l: LineaReposicion): BigDecimal = l.precioCompra.multiply(BigDecimal(cantidad(l)))

    val total: BigDecimal get() = lineas.fold(BigDecimal.ZERO) { a, l -> a + coste(l) }

    val grupos: List<GrupoProveedor>
        get() = lineas.groupBy { it.proveedorId }.values
            .map { g -> GrupoProveedor(g.first().proveedorNombre, g.first().proveedorTelefono, g.first().proveedorEmail, g) }
            .sortedWith(compareBy({ it.nombre == null }, { it.nombre }))

    fun pedido(g: GrupoProveedor): String = buildString {
        appendLine("Hola${g.nombre?.let { ", $it" } ?: ""}, necesitamos reponer:")
        appendLine()
        g.lineas.forEach { appendLine("- ${cantidad(it)} x ${it.nombre}") }
        appendLine()
        append("Gracias.")
    }

    fun pedidoCompleto(): String = grupos.joinToString("\n\n") { g ->
        "== ${g.nombre ?: "Sin proveedor"} ==\n" + g.lineas.joinToString("\n") { "- ${cantidad(it)} x ${it.nombre}" }
    }
}

@Composable
fun PantallaReposicion(onAtras: () -> Unit) {
    val repo = repositorio()
    val vm: ReposicionViewModel = viewModel { ReposicionViewModel(repo) }
    val context = LocalContext.current
    LaunchedEffect(Unit) { vm.cargar() }

    Pantalla(titulo = "Reposición", onAtras = onAtras) { margenes ->
        EstadoLista(
            cargando = vm.cargando,
            error = vm.error,
            vacia = vm.lineas.isEmpty(),
            textoVacio = "Todo en orden: no hay productos por debajo de su mínimo.",
            iconoVacio = Icons.Outlined.TaskAlt,
            onReintentar = vm::cargar,
            modifier = Modifier.padding(margenes),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.primaryContainer) {
                        Row(
                            Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    if (vm.lineas.size == 1) "1 producto por reponer" else "${vm.lineas.size} productos por reponer",
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                Text(
                                    "Coste estimado ${dinero(vm.total)}",
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                            FilledTonalButton(onClick = {
                                compartirTexto(context, vm.pedidoCompleto(), "Pedido de reposición")
                            }) {
                                Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                Text("  Todo")
                            }
                        }
                    }
                }
                vm.grupos.forEach { g ->
                    item(key = "g-${g.nombre}") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(g.nombre ?: "Sin proveedor", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    g.telefono?.takeIf { it.isNotBlank() }?.let {
                                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                FilledTonalButton(onClick = {
                                    compartirTexto(context, vm.pedido(g), "Pedido de reposición", g.email)
                                }) {
                                    Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Text("  Enviar pedido")
                                }
                            }
                            g.lineas.forEach { l -> FilaLinea(vm, l) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilaLinea(vm: ReposicionViewModel, l: LineaReposicion) {
    val cantidad = vm.cantidad(l)
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = bordeTarjeta(),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(l.nombre, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "Stock ${l.stockActual} (mín. ${l.stockMinimo}) · ${dinero(vm.coste(l))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            FilledTonalIconButton(onClick = { vm.cambiar(l, cantidad - 1) }, enabled = cantidad > 1, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.Remove, contentDescription = "Pedir una unidad menos", modifier = Modifier.size(16.dp))
            }
            Text("$cantidad", fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp))
            FilledTonalIconButton(onClick = { vm.cambiar(l, cantidad + 1) }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.Add, contentDescription = "Pedir una unidad más", modifier = Modifier.size(16.dp))
            }
        }
    }
}
