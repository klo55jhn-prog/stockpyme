package com.stockpyme.app.ui.panel

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.AddBox
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stockpyme.app.datos.Actividad
import com.stockpyme.app.datos.DiaActividad
import com.stockpyme.app.datos.Movimiento
import com.stockpyme.app.datos.Producto
import com.stockpyme.app.datos.Repositorio
import com.stockpyme.app.datos.Usuario
import com.stockpyme.app.ui.comun.EstadoLista
import com.stockpyme.app.ui.comun.GraficaBarras
import com.stockpyme.app.ui.comun.IconoRedondo
import com.stockpyme.app.ui.comun.MostrarMensaje
import com.stockpyme.app.ui.comun.Pantalla
import com.stockpyme.app.ui.comun.PildoraStock
import com.stockpyme.app.ui.comun.Pildora
import com.stockpyme.app.ui.comun.TarjetaDato
import com.stockpyme.app.ui.comun.TituloSeccion
import com.stockpyme.app.ui.comun.dinero
import com.stockpyme.app.ui.comun.rememberEscaner
import com.stockpyme.app.ui.comun.repositorio
import com.stockpyme.app.ui.productos.FilaMovimiento
import com.stockpyme.app.ui.tema.MarcaClara
import com.stockpyme.app.ui.tema.MarcaOscura
import com.stockpyme.app.ui.tema.coloresEstado
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.launch

private val ES = Locale.forLanguageTag("es-ES")

/** Panel de inicio (V2): cómo va el día, qué reponer y accesos rápidos. */
class PanelViewModel(private val repo: Repositorio, private val esPropietario: Boolean) : ViewModel() {

    var dias by mutableIntStateOf(7)
        private set
    var actividad by mutableStateOf<Actividad?>(null)
        private set
    var alertas by mutableStateOf<List<Producto>>(emptyList())
        private set
    var recientes by mutableStateOf<List<Movimiento>>(emptyList())
        private set
    var cargando by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var mensaje by mutableStateOf<String?>(null)

    fun cargar() {
        viewModelScope.launch {
            cargando = true
            error = null
            if (esPropietario) {
                repo.actividad(dias).onSuccess { actividad = it }.onFailure { error = it.message }
            }
            repo.bajoMinimo().onSuccess { alertas = it }.onFailure { if (error == null) error = it.message }
            repo.recientes().onSuccess { recientes = it }
            cargando = false
        }
    }

    fun cambiarDias(nuevo: Int) {
        dias = nuevo
        viewModelScope.launch { repo.actividad(nuevo).onSuccess { actividad = it } }
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
fun PantallaPanel(
    usuario: Usuario,
    onAbrir: (Long) -> Unit,
    onVender: () -> Unit,
    onReponer: () -> Unit,
    onNuevo: (String?) -> Unit,
    onVerAlertas: () -> Unit,
    onVerActividad: () -> Unit,
) {
    val repo = repositorio()
    val vm: PanelViewModel = viewModel(key = "panel-${usuario.id}") { PanelViewModel(repo, usuario.esPropietario) }
    val snackbar = remember { SnackbarHostState() }
    val alcance = rememberCoroutineScope()

    LaunchedEffect(Unit) { vm.cargar() }
    MostrarMensaje(vm.mensaje, snackbar) { vm.mensaje = null }

    val escanear = rememberEscaner(
        onCodigo = { codigo ->
            vm.buscarCodigo(codigo, onEncontrado = onAbrir, onNoExiste = { noExiste ->
                alcance.launch {
                    if (usuario.esPropietario) {
                        val r = snackbar.showSnackbar("No hay ningún producto con el código $noExiste", actionLabel = "Crear")
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
        titulo = "${saludo()}, ${usuario.nombre.substringBefore(' ')}",
        snackbar = snackbar,
        botonFlotante = {
            ExtendedFloatingActionButton(
                onClick = onVender,
                icon = { Icon(Icons.Filled.ShoppingCart, contentDescription = null) },
                text = { Text("Nueva venta") },
            )
        },
    ) { margenes ->
        PullToRefreshBox(
            isRefreshing = vm.cargando && (vm.actividad != null || vm.recientes.isNotEmpty()),
            onRefresh = vm::cargar,
            modifier = Modifier.padding(margenes).fillMaxSize(),
        ) {
            EstadoLista(
                cargando = vm.cargando,
                error = vm.error,
                vacia = vm.actividad == null && vm.alertas.isEmpty() && vm.recientes.isEmpty(),
                textoVacio = "Todavía no hay datos. Crea tus productos y registra una entrada de stock.",
                onReintentar = vm::cargar,
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 104.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item { Cabecera(vm, usuario) }
                    if (usuario.esPropietario && vm.actividad != null) {
                        item { Indicadores(vm.actividad!!, vm.dias) }
                        item { TarjetaGrafica(vm) }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Accion(Icons.Outlined.QrCodeScanner, "Escanear", Modifier.weight(1f), escanear)
                            Accion(Icons.Outlined.LocalShipping, "Reponer", Modifier.weight(1f), onReponer)
                            if (usuario.esPropietario) {
                                Accion(Icons.Outlined.AddBox, "Nuevo", Modifier.weight(1f)) { onNuevo(null) }
                            } else {
                                Accion(Icons.Outlined.History, "Actividad", Modifier.weight(1f), onVerActividad)
                            }
                        }
                    }
                    item { TarjetaAlertas(vm.alertas, onAbrir, onVerAlertas) }
                    if (vm.recientes.isNotEmpty()) {
                        item { TituloSeccion("Actividad reciente") }
                        items(vm.recientes.take(5), key = { "m${it.id}" }) { m ->
                            FilaMovimiento(m, mostrarProducto = true, onClick = { onAbrir(m.productoId) })
                        }
                        item {
                            TextButton(onClick = onVerActividad, modifier = Modifier.fillMaxWidth()) {
                                Text("Ver toda la actividad")
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun saludo(): String = when (LocalTime.now().hour) {
    in 6..13 -> "Buenos días"
    in 14..20 -> "Buenas tardes"
    else -> "Buenas noches"
}

private fun esHoy(iso: String): Boolean = try {
    Instant.parse(iso).atZone(ZoneId.systemDefault()).toLocalDate() == LocalDate.now()
} catch (e: Exception) {
    false
}

/** Tarjeta grande de arriba: ventas de hoy (propietario) o movimientos de hoy (empleado). */
@Composable
private fun Cabecera(vm: PanelViewModel, usuario: Usuario) {
    val actividad = vm.actividad
    val fecha = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", ES))
        .replaceFirstChar { it.uppercase() }
    Surface(shape = MaterialTheme.shapes.extraLarge, modifier = Modifier.fillMaxWidth()) {
        Column(
            Modifier
                .background(Brush.linearGradient(listOf(MarcaClara, MarcaOscura)))
                .padding(20.dp),
        ) {
            Text(fecha, style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.85f))
            Spacer(Modifier.height(10.dp))
            if (usuario.esPropietario && actividad != null) {
                Text("VENTAS DE HOY", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.8f))
                Text(dinero(actividad.ventasHoy), style = MaterialTheme.typography.displayMedium, color = Color.White)
                val ayer = actividad.dias.getOrNull(actividad.dias.size - 2)
                Text(
                    buildString {
                        append("${actividad.movimientosHoy} movimientos hoy")
                        if (ayer != null) append(" · Ayer ${dinero(ayer.ventas)}")
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f),
                )
            } else {
                val hoy = vm.recientes.count { esHoy(it.fecha) }
                Text("HOY", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.8f))
                Text("$hoy", style = MaterialTheme.typography.displayMedium, color = Color.White)
                Text(
                    if (hoy == 1) "movimiento registrado" else "movimientos registrados",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f),
                )
            }
        }
    }
}

@Composable
private fun Indicadores(actividad: Actividad, dias: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        TarjetaDato(
            "Ventas · $dias días", dinero(actividad.ventasPeriodo), Icons.Outlined.Sell,
            MaterialTheme.colorScheme.primary, Modifier.weight(1f),
        )
        TarjetaDato(
            "Unidades vendidas", actividad.unidadesVendidasPeriodo.toString(), Icons.Outlined.Widgets,
            Color(0xFF7C3AED), Modifier.weight(1f),
        )
    }
}

@Composable
private fun TarjetaGrafica(vm: PanelViewModel) {
    val actividad = vm.actividad ?: return
    val dias = actividad.dias
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shadowElevation = 0.5.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Ventas por día", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(7, 14, 30).forEach { n ->
                        FilterChip(selected = vm.dias == n, onClick = { vm.cambiarDias(n) }, label = { Text("$n d") })
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            GraficaBarras(
                valores = dias.map { it.ventas.toFloat() },
                etiquetas = etiquetas(dias),
                detalle = { i -> detalleDia(dias[i]) },
            )
        }
    }
}

private fun etiquetas(dias: List<DiaActividad>): List<String> = dias.mapIndexed { i, d ->
    val fecha = LocalDate.parse(d.fecha)
    when {
        dias.size <= 7 -> fecha.dayOfWeek.getDisplayName(TextStyle.NARROW, ES).uppercase()
        dias.size <= 14 -> fecha.dayOfMonth.toString()
        else -> if ((dias.lastIndex - i) % 5 == 0) fecha.dayOfMonth.toString() else ""
    }
}

private fun detalleDia(d: DiaActividad): String {
    val fecha = LocalDate.parse(d.fecha).format(DateTimeFormatter.ofPattern("EEE d MMM", ES))
    return "$fecha · ${dinero(d.ventas)} · ${d.salidas} salidas"
}

@Composable
private fun Accion(icono: ImageVector, texto: String, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shadowElevation = 0.5.dp,
        modifier = modifier,
    ) {
        Column(
            Modifier.padding(vertical = 14.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            IconoRedondo(icono, MaterialTheme.colorScheme.primary, tamano = 44.dp)
            Text(texto, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun TarjetaAlertas(alertas: List<Producto>, onAbrir: (Long) -> Unit, onVerTodas: () -> Unit) {
    val c = coloresEstado
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shadowElevation = 0.5.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(vertical = 8.dp)) {
            Row(Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Stock bajo", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (alertas.isNotEmpty()) {
                    Pildora("${alertas.size}", c.aviso, c.avisoFondo, punto = false)
                    TextButton(onClick = onVerTodas) { Text("Ver todas") }
                }
            }
            if (alertas.isEmpty()) {
                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    IconoRedondo(Icons.Outlined.CheckCircle, c.ok)
                    Text("Todo en orden: ningún producto en su mínimo", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                alertas.take(3).forEach { p ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onAbrir(p.id) }.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(p.nombre, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                if (p.stockActual == 0) "Agotado" else "Mínimo ${p.stockMinimo} u.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        PildoraStock(p)
                    }
                }
            }
        }
    }
}
