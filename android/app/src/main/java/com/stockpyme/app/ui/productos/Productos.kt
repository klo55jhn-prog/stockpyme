package com.stockpyme.app.ui.productos

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stockpyme.app.datos.Producto
import com.stockpyme.app.datos.Repositorio
import com.stockpyme.app.datos.Usuario
import com.stockpyme.app.ui.comun.EstadoLista
import com.stockpyme.app.ui.comun.MostrarMensaje
import com.stockpyme.app.ui.comun.Pantalla
import com.stockpyme.app.ui.comun.dinero
import com.stockpyme.app.ui.comun.rememberEscaner
import com.stockpyme.app.ui.comun.repositorio
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** CU-05 Buscar producto (por texto o escaneando el código de barras). */
class ProductosViewModel(private val repo: Repositorio) : ViewModel() {

    var texto by mutableStateOf("")
        private set
    var incluirInactivos by mutableStateOf(false)
        private set
    var productos by mutableStateOf<List<Producto>>(emptyList())
        private set
    var cargando by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var mensaje by mutableStateOf<String?>(null)

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
                FloatingActionButton(onClick = { onNuevo(null) }) {
                    Icon(Icons.Filled.Add, contentDescription = "Nuevo producto")
                }
            }
        },
    ) { margenes ->
        Column(Modifier.padding(margenes).fillMaxSize()) {
            OutlinedTextField(
                value = vm.texto,
                onValueChange = vm::cambiarTexto,
                placeholder = { Text("Buscar por nombre o código") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (vm.texto.isNotEmpty()) {
                        IconButton(onClick = { vm.cambiarTexto("") }) {
                            Icon(Icons.Filled.Close, contentDescription = "Borrar búsqueda")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )
            if (usuario.esPropietario) {
                FilterChip(
                    selected = vm.incluirInactivos,
                    onClick = { vm.cambiarInactivos(!vm.incluirInactivos) },
                    label = { Text("Mostrar desactivados") },
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            PullToRefreshBox(
                isRefreshing = vm.cargando && vm.productos.isNotEmpty(),
                onRefresh = vm::cargar,
                modifier = Modifier.fillMaxSize(),
            ) {
                EstadoLista(
                    cargando = vm.cargando,
                    error = vm.error,
                    vacia = vm.productos.isEmpty(),
                    textoVacio = if (vm.texto.isBlank()) "Todavía no hay productos" else "Ningún producto coincide con la búsqueda",
                    onReintentar = vm::cargar,
                ) {
                    ListaProductos(vm.productos, onAbrir)
                }
            }
        }
    }
}

@Composable
fun ListaProductos(productos: List<Producto>, onAbrir: (Long) -> Unit) {
    LazyColumn(Modifier.fillMaxSize()) {
        items(productos, key = { it.id }) { producto ->
            FilaProducto(producto, onClick = { onAbrir(producto.id) })
            HorizontalDivider()
        }
    }
}

@Composable
fun FilaProducto(producto: Producto, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = {
            Text(
                producto.nombre + if (!producto.activo) " (desactivado)" else "",
                fontWeight = FontWeight.Medium,
                color = if (producto.activo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
            )
        },
        supportingContent = {
            Text("${producto.categoriaNombre} · ${dinero(producto.precioVenta)}")
        },
        trailingContent = { IndicadorStock(producto) },
    )
}

/** Stock en una etiqueta: roja si está en el mínimo o por debajo (RF-09). */
@Composable
fun IndicadorStock(producto: Producto) {
    val alerta = producto.bajoMinimo && producto.activo
    Badge(
        containerColor = if (alerta) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
        contentColor = if (alerta) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Text(
            "${producto.stockActual} u.",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

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

    Pantalla(titulo = "Stock bajo") { margenes ->
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
                onReintentar = vm::cargar,
            ) {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(vm.productos, key = { it.id }) { p ->
                        ListItem(
                            modifier = Modifier.clickable { onAbrir(p.id) },
                            headlineContent = { Text(p.nombre, fontWeight = FontWeight.Medium) },
                            supportingContent = {
                                val texto = if (p.stockActual == 0) "Agotado" else "Mínimo: ${p.stockMinimo} u."
                                Text(listOfNotNull(texto, p.proveedorNombre?.let { "Proveedor: $it" }).joinToString(" · "))
                            },
                            trailingContent = { IndicadorStock(p) },
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}
