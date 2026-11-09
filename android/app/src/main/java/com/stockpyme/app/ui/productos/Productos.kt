package com.stockpyme.app.ui.productos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.ReportProblem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stockpyme.app.datos.Producto
import com.stockpyme.app.datos.Repositorio
import com.stockpyme.app.datos.Usuario
import com.stockpyme.app.ui.comun.Avatar
import com.stockpyme.app.ui.comun.EstadoLista
import com.stockpyme.app.ui.comun.EstadoStock
import com.stockpyme.app.ui.comun.FilaTarjeta
import com.stockpyme.app.ui.comun.IconoRedondo
import com.stockpyme.app.ui.comun.MostrarMensaje
import com.stockpyme.app.ui.comun.Pantalla
import com.stockpyme.app.ui.comun.PildoraStock
import com.stockpyme.app.ui.comun.colores
import com.stockpyme.app.ui.comun.dinero
import com.stockpyme.app.ui.comun.estadoStock
import com.stockpyme.app.ui.comun.rememberEscaner
import com.stockpyme.app.ui.comun.repositorio
import com.stockpyme.app.ui.tema.coloresEstado
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** CU-05 Buscar producto (por texto o escaneando el código de barras). */
class ProductosViewModel(private val repo: Repositorio) : ViewModel() {

    var texto by mutableStateOf("")
        private set
    var incluirInactivos by mutableStateOf(false)
        private set
    var soloBajos by mutableStateOf(false)
    var productos by mutableStateOf<List<Producto>>(emptyList())
        private set
    var cargando by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var mensaje by mutableStateOf<String?>(null)

    /** Lo que se ve en la lista, con el filtro "Stock bajo" aplicado en el móvil. */
    val visibles: List<Producto>
        get() = if (soloBajos) productos.filter { it.activo && it.bajoMinimo } else productos

    private var busqueda: Job? = null

    fun cargar() {
        busqueda?.cancel()
        busqueda = viewModelScope.launch { buscarAhora() }
    }

    /** Espera a que el usuario deje de escribir 300 ms antes de buscar. */
    fun cambiarTexto(nuevo: String) {
        texto = nuevo
        busqueda?.cancel()
        busqueda = viewModelScope.launch {
            delay(300)
            buscarAhora()
        }
    }

    fun cambiarInactivos(valor: Boolean) {
        incluirInactivos = valor
        cargar()
    }

    private suspend fun buscarAhora() {
        cargando = true
        repo.buscarProductos(texto, incluirInactivos)
            .onSuccess {
                productos = it
                error = null
            }
            .onFailure { error = it.message }
        cargando = false
    }

    fun buscarCodigo(codigo: String, onEncontrado: (Long) -> Unit, onNoExiste: (String) -> Unit) {
        viewModelScope.launch {
            repo.productoPorCodigo(codigo)
                .onSuccess { onEncontrado(it.id) }
                .onFailure { onNoExiste(codigo) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaProductos(usuario: Usuario, onAbrir: (Long) -> Unit, onNuevo: (String?) -> Unit) {
    val repo = repositorio()
    val vm: ProductosViewModel = viewModel { ProductosViewModel(repo) }
    val snackbar = remember { SnackbarHostState() }
    val alcance = rememberCoroutineScope()

    // Se recarga cada vez que se vuelve a esta pantalla (por ejemplo, después de una salida).
    LaunchedEffect(Unit) { vm.cargar() }
    MostrarMensaje(vm.mensaje, snackbar) { vm.mensaje = null }

    val escanear = rememberEscaner(
        onCodigo = { codigo ->
            vm.buscarCodigo(codigo, onEncontrado = onAbrir, onNoExiste = { noExiste ->
                alcance.launch {
                    if (usuario.esPropietario) {
                        val r = snackbar.showSnackbar(
                            "No hay ningún producto con el código $noExiste",
                            actionLabel = "Crear",
                        )
                        if (r == SnackbarResult.ActionPerformed) onNuevo(noExiste)
                    } else {
                        snackbar.showSnackbar("No hay ningún producto con el código $noExiste")
                    }
                }
            })
        },
        onError = { vm.mensaje = it },
    )

    Pantalla(
        titulo = "Productos",
        snackbar = snackbar,
        acciones = {
            IconButton(onClick = escanear) {
                Icon(Icons.Filled.QrCodeScanner, contentDescription = "Escanear código de barras")
            }
        },
        botonFlotante = {
            if (usuario.esPropietario) {
                ExtendedFloatingActionButton(
                    onClick = { onNuevo(null) },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("Nuevo") },
                )
            }
        },
    ) { margenes ->
        Column(Modifier.padding(margenes).fillMaxSize()) {
            CampoBusqueda(vm.texto, vm::cambiarTexto)

            val bajos = vm.productos.count { it.activo && it.bajoMinimo }
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = vm.soloBajos,
                    onClick = { vm.soloBajos = !vm.soloBajos },
                    label = { Text(if (bajos > 0) "Stock bajo ($bajos)" else "Stock bajo") },
                )
                if (usuario.esPropietario) {
                    FilterChip(
                        selected = vm.incluirInactivos,
                        onClick = { vm.cambiarInactivos(!vm.incluirInactivos) },
                        label = { Text("Desactivados") },
                    )
                }
            }

            PullToRefreshBox(
                isRefreshing = vm.cargando && vm.productos.isNotEmpty(),
                onRefresh = vm::cargar,
                modifier = Modifier.fillMaxSize(),
            ) {
                EstadoLista(
                    cargando = vm.cargando,
                    error = vm.error,
                    vacia = vm.visibles.isEmpty(),
                    textoVacio = when {
                        vm.soloBajos -> "Ningún producto está en su stock mínimo"
                        vm.texto.isBlank() -> "Todavía no hay productos"
                        else -> "Ningún producto coincide con «${vm.texto}»"
                    },
                    iconoVacio = Icons.Outlined.Inventory2,
                    onReintentar = vm::cargar,
                ) {
                    ListaProductos(vm.visibles, onAbrir)
                }
            }
        }
    }
}

@Composable
private fun CampoBusqueda(texto: String, onCambio: (String) -> Unit) {
    TextField(
        value = texto,
        onValueChange = onCambio,
        placeholder = { Text("Buscar por nombre o código") },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            if (texto.isNotEmpty()) {
                IconButton(onClick = { onCambio("") }) {
                    Icon(Icons.Filled.Close, contentDescription = "Borrar búsqueda")
                }
            }
        },
        singleLine = true,
        shape = CircleShape,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
    )
}

@Composable
fun ListaProductos(productos: List<Producto>, onAbrir: (Long) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(productos, key = { it.id }) { producto ->
            FilaProducto(producto, onClick = { onAbrir(producto.id) })
        }
    }
}

@Composable
fun FilaProducto(producto: Producto, onClick: () -> Unit) {
    val activo = producto.activo
    FilaTarjeta(
        onClick = onClick,
        inicio = { Avatar(producto.nombre, clave = producto.categoriaNombre) },
        titulo = {
            Text(
                producto.nombre,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (activo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
            )
        },
        subtitulo = {
            Text(
                "${producto.categoriaNombre} · ${dinero(producto.precioVenta)}",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        fin = { PildoraStock(producto) },
    )
}

/** Se mantiene por compatibilidad con otras pantallas: etiqueta de stock coloreada. */
@Composable
fun IndicadorStock(producto: Producto) = PildoraStock(producto)

// ---------- CU-11 Productos bajo el mínimo ----------

class AlertasViewModel(private val repo: Repositorio) : ViewModel() {
    var productos by mutableStateOf<List<Producto>>(emptyList())
        private set
    var cargando by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    fun cargar() {
        viewModelScope.launch {
            cargando = true
            repo.bajoMinimo()
                .onSuccess {
                    productos = it
                    error = null
                }
                .onFailure { error = it.message }
            cargando = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaAlertas(onAbrir: (Long) -> Unit) {
    val repo = repositorio()
    val vm: AlertasViewModel = viewModel { AlertasViewModel(repo) }
    LaunchedEffect(Unit) { vm.cargar() }

    Pantalla(titulo = "Alertas de stock") { margenes ->
        PullToRefreshBox(
            isRefreshing = vm.cargando && vm.productos.isNotEmpty(),
            onRefresh = vm::cargar,
            modifier = Modifier.padding(margenes).fillMaxSize(),
        ) {
            EstadoLista(
                cargando = vm.cargando,
                error = vm.error,
                vacia = vm.productos.isEmpty(),
                textoVacio = "Todo en orden: ningún producto está en su stock mínimo",
                iconoVacio = Icons.Outlined.CheckCircle,
                onReintentar = vm::cargar,
            ) {
                val agotados = vm.productos.count { it.stockActual == 0 }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item { ResumenAlertas(vm.productos.size, agotados) }
                    items(vm.productos, key = { it.id }) { p -> TarjetaAlerta(p) { onAbrir(p.id) } }
                }
            }
        }
    }
}

@Composable
private fun ResumenAlertas(total: Int, agotados: Int) {
    val c = coloresEstado
    Surface(shape = MaterialTheme.shapes.large, color = c.avisoFondo, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            IconoRedondo(Icons.Outlined.ReportProblem, c.aviso, tamano = 44.dp)
            Column {
                Text(
                    if (total == 1) "1 producto necesita reposición" else "$total productos necesitan reposición",
                    style = MaterialTheme.typography.titleMedium,
                    color = c.aviso,
                )
                if (agotados > 0) {
                    Text(
                        if (agotados == 1) "1 está agotado" else "$agotados están agotados",
                        style = MaterialTheme.typography.bodyMedium,
                        color = c.aviso,
                    )
                }
            }
        }
    }
}

@Composable
private fun TarjetaAlerta(p: Producto, onClick: () -> Unit) {
    val estado = p.estadoStock()
    val (color, _) = colores(estado)
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shadowElevation = 0.5.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(p.nombre, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        p.proveedorNombre?.let { "Proveedor: $it" } ?: "Sin proveedor",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                PildoraStock(p)
            }
            Spacer(Modifier.height(12.dp))
            // Barra: cuánto queda respecto al mínimo
            val progreso = if (p.stockMinimo <= 0) 0f else (p.stockActual.toFloat() / p.stockMinimo).coerceIn(0f, 1f)
            LinearProgressIndicator(
                progress = { progreso },
                color = color,
                trackColor = color.copy(alpha = 0.15f),
                strokeCap = StrokeCap.Round,
                modifier = Modifier.fillMaxWidth().height(6.dp),
            )
            Spacer(Modifier.size(6.dp))
            Text(
                if (estado == EstadoStock.AGOTADO) "Agotado · mínimo ${p.stockMinimo} u."
                else "Quedan ${p.stockActual} de un mínimo de ${p.stockMinimo} u.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
