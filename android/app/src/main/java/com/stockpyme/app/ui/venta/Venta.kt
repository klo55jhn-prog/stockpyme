package com.stockpyme.app.ui.venta

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stockpyme.app.datos.LineaVentaRequest
import com.stockpyme.app.datos.Producto
import com.stockpyme.app.datos.Repositorio
import com.stockpyme.app.datos.VentaRequest
import com.stockpyme.app.datos.VentaRespuesta
import com.stockpyme.app.ui.comun.Avatar
import com.stockpyme.app.ui.comun.FilaTarjeta
import com.stockpyme.app.ui.comun.IconoRedondo
import com.stockpyme.app.ui.comun.MostrarMensaje
import com.stockpyme.app.ui.comun.Pantalla
import com.stockpyme.app.ui.comun.dinero
import com.stockpyme.app.ui.comun.rememberEscaner
import com.stockpyme.app.ui.comun.repositorio
import com.stockpyme.app.ui.tema.coloresEstado
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.math.BigDecimal

data class LineaCarrito(val producto: Producto, val cantidad: Int) {
    val importe: BigDecimal get() = producto.precioVenta.multiply(BigDecimal(cantidad))
}

/** Resultado de una venta cobrada: el resumen y qué productos quedaron en stock bajo. */
data class VentaHecha(val resumen: VentaRespuesta, val bajos: List<String>)

/** Modo venta: carrito con buscador y escáner. Cobrar descuenta todo el stock en una sola operación. */
class VentaViewModel(private val repo: Repositorio) : ViewModel() {
    var lineas by mutableStateOf<List<LineaCarrito>>(emptyList())
        private set
    var texto by mutableStateOf("")
        private set
    var sugerencias by mutableStateOf<List<Producto>>(emptyList())
        private set
    var cobrando by mutableStateOf(false)
        private set
    var hecha by mutableStateOf<VentaHecha?>(null)
        private set
    var mensaje by mutableStateOf<String?>(null)

    val total: BigDecimal get() = lineas.fold(BigDecimal.ZERO) { a, l -> a + l.importe }
    val unidades: Int get() = lineas.sumOf { it.cantidad }

    private var busqueda: Job? = null

    fun cambiarTexto(nuevo: String) {
        texto = nuevo
        busqueda?.cancel()
        if (nuevo.isBlank()) {
            sugerencias = emptyList()
            return
        }
        busqueda = viewModelScope.launch {
            delay(250)
            repo.buscarProductos(nuevo, false).onSuccess { lista ->
                sugerencias = lista.filter { it.activo }.take(6)
            }
        }
    }

    fun anadir(producto: Producto) {
        val actual = lineas.firstOrNull { it.producto.id == producto.id }
        when {
            producto.stockActual <= 0 -> mensaje = "«${producto.nombre}» está agotado"
            actual == null -> lineas = lineas + LineaCarrito(producto, 1)
            actual.cantidad >= producto.stockActual ->
                mensaje = "Solo quedan ${producto.stockActual} u. de «${producto.nombre}»"
            else -> cambiarCantidad(producto.id, actual.cantidad + 1)
        }
        busqueda?.cancel()
        texto = ""
        sugerencias = emptyList()
    }

    fun cambiarCantidad(id: Long, cantidad: Int) {
        lineas = lineas.mapNotNull { l ->
            when {
                l.producto.id != id -> l
                cantidad <= 0 -> null
                else -> l.copy(cantidad = cantidad.coerceAtMost(l.producto.stockActual))
            }
        }
    }

    fun anadirPorCodigo(codigo: String) {
        viewModelScope.launch {
            repo.productoPorCodigo(codigo)
                .onSuccess { anadir(it) }
                .onFailure { mensaje = "No hay ningún producto con el código $codigo" }
        }
    }

    fun cobrar() {
        if (lineas.isEmpty() || cobrando) return
        viewModelScope.launch {
            cobrando = true
            repo.registrarVenta(VentaRequest(lineas.map { LineaVentaRequest(it.producto.id, it.cantidad) }))
                .onSuccess { resumen ->
                    val bajos = resumen.productos.filter { it.bajoMinimo }.map { it.nombre }
                    hecha = VentaHecha(resumen, bajos)
                    lineas = emptyList()
                }
                .onFailure { mensaje = it.message }
            cobrando = false
        }
    }

    fun nuevaVenta() {
        hecha = null
    }
}

@Composable
fun PantallaVenta(onAtras: () -> Unit) {
    val repo = repositorio()
    val vm: VentaViewModel = viewModel { VentaViewModel(repo) }
    val snackbar = remember { SnackbarHostState() }
    MostrarMensaje(vm.mensaje, snackbar) { vm.mensaje = null }

    val escanear = rememberEscaner(onCodigo = vm::anadirPorCodigo, onError = { vm.mensaje = it })

    Pantalla(
        titulo = "Nueva venta",
        onAtras = onAtras,
        snackbar = snackbar,
        acciones = {
            if (vm.hecha == null) {
                IconButton(onClick = escanear) {
                    Icon(Icons.Filled.QrCodeScanner, contentDescription = "Escanear código de barras")
                }
            }
        },
        barraInferior = {
            if (vm.hecha == null && vm.lineas.isNotEmpty()) BarraCobro(vm)
        },
    ) { margenes ->
        val hecha = vm.hecha
        Column(Modifier.padding(margenes).fillMaxSize()) {
            if (hecha != null) {
                VentaCobrada(hecha, onNueva = vm::nuevaVenta, onTerminar = onAtras)
            } else {
                CampoProducto(vm.texto, vm::cambiarTexto)
                if (vm.sugerencias.isNotEmpty()) {
                    Sugerencias(vm.sugerencias, vm::anadir)
                } else if (vm.lineas.isEmpty()) {
                    CarritoVacio()
                } else {
                    Carrito(vm)
                }
            }
        }
    }
}

@Composable
private fun CampoProducto(texto: String, onCambio: (String) -> Unit) {
    TextField(
        value = texto,
        onValueChange = onCambio,
        placeholder = { Text("Buscar producto para añadir") },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            if (texto.isNotEmpty()) {
                IconButton(onClick = { onCambio("") }) {
                    Icon(Icons.Filled.Close, contentDescription = "Borrar búsqueda")
                }
            }
        },
        singleLine = true,
        shape = MaterialTheme.shapes.medium,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium),
    )
}

@Composable
private fun Sugerencias(productos: List<Producto>, onElegir: (Producto) -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(productos, key = { it.id }) { p ->
            FilaTarjeta(
                onClick = { onElegir(p) },
                inicio = { Avatar(p.nombre, clave = p.categoriaNombre) },
                titulo = { Text(p.nombre, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                subtitulo = {
                    Text("${dinero(p.precioVenta)} · ${if (p.stockActual > 0) "${p.stockActual} en stock" else "agotado"}")
                },
                fin = { Icon(Icons.Filled.Add, contentDescription = "Añadir", tint = MaterialTheme.colorScheme.primary) },
            )
        }
    }
}

@Composable
private fun CarritoVacio() {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IconoRedondo(Icons.Outlined.ShoppingCart, MaterialTheme.colorScheme.primary, tamano = 72.dp)
        Text("El carrito está vacío", style = MaterialTheme.typography.titleMedium)
        Text(
            "Busca un producto o escanea su código de barras para añadirlo.",
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Carrito(vm: VentaViewModel) {
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(vm.lineas, key = { it.producto.id }) { l ->
            FilaTarjeta(
                inicio = { Avatar(l.producto.nombre, clave = l.producto.categoriaNombre) },
                titulo = {
                    Text(l.producto.nombre, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                subtitulo = { Text("${dinero(l.producto.precioVenta)} × ${l.cantidad} = ${dinero(l.importe)}") },
                fin = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        FilledTonalIconButton(
                            onClick = { vm.cambiarCantidad(l.producto.id, l.cantidad - 1) },
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(Icons.Filled.Remove, contentDescription = "Quitar una unidad", modifier = Modifier.size(16.dp))
                        }
                        Text("${l.cantidad}", fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp))
                        FilledTonalIconButton(
                            onClick = { vm.cambiarCantidad(l.producto.id, l.cantidad + 1) },
                            enabled = l.cantidad < l.producto.stockActual,
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Añadir una unidad", modifier = Modifier.size(16.dp))
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun BarraCobro(vm: VentaViewModel) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, shadowElevation = 8.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    if (vm.unidades == 1) "1 unidad" else "${vm.unidades} unidades",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(dinero(vm.total), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
            Button(onClick = vm::cobrar, enabled = !vm.cobrando) {
                Text(if (vm.cobrando) "Cobrando…" else "Cobrar")
            }
        }
    }
}

@Composable
private fun VentaCobrada(hecha: VentaHecha, onNueva: () -> Unit, onTerminar: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IconoRedondo(Icons.Outlined.CheckCircle, coloresEstado.ok, tamano = 88.dp)
        Text("Venta registrada", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(dinero(hecha.resumen.total), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
        Text(
            "${hecha.resumen.unidades} ${if (hecha.resumen.unidades == 1) "unidad" else "unidades"} · ${hecha.resumen.productos.size} ${if (hecha.resumen.productos.size == 1) "producto" else "productos"}",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (hecha.bajos.isNotEmpty()) {
            Surface(shape = MaterialTheme.shapes.large, color = coloresEstado.avisoFondo) {
                Text(
                    "Stock bajo tras la venta: ${hecha.bajos.joinToString(", ")}",
                    color = coloresEstado.aviso,
                    modifier = Modifier.padding(12.dp),
                    textAlign = TextAlign.Center,
                )
            }
        }
        Button(onClick = onNueva, modifier = Modifier.fillMaxWidth()) { Text("Nueva venta") }
        FilledTonalButton(onClick = onTerminar, modifier = Modifier.fillMaxWidth()) { Text("Terminar") }
    }
}
