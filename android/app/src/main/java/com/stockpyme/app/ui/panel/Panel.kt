package com.stockpyme.app.ui.panel

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.AddBox
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import com.stockpyme.app.ui.comun.BotonCabecera
import com.stockpyme.app.ui.comun.Chip
import com.stockpyme.app.ui.comun.EstadoLista
import com.stockpyme.app.ui.comun.GraficaBarras
import com.stockpyme.app.ui.comun.IconoRedondo
import com.stockpyme.app.ui.comun.MostrarMensaje
import com.stockpyme.app.ui.comun.Pantalla
import com.stockpyme.app.ui.comun.Pildora
import com.stockpyme.app.ui.comun.PildoraStock
import com.stockpyme.app.ui.comun.Tarjeta
import com.stockpyme.app.ui.comun.bordeTarjeta
import com.stockpyme.app.ui.comun.dinero
import com.stockpyme.app.ui.comun.rememberEscaner
import com.stockpyme.app.ui.comun.repositorio
import com.stockpyme.app.ui.productos.FilaMovimiento
import com.stockpyme.app.ui.tema.LocalOscuro
import com.stockpyme.app.ui.tema.MarcaClara
import com.stockpyme.app.ui.tema.Tinta
import com.stockpyme.app.ui.tema.coloresEstado
import java.math.BigDecimal
import java.math.RoundingMode
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

    val fecha = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", ES))
        .replaceFirstChar { it.uppercase() }

    Pantalla(
        titulo = "${saludo()}, ${usuario.nombre.substringBefore(' ')}",
        subtitulo = fecha,
        snackbar = snackbar,
        acciones = {
            BotonCabecera(Icons.Outlined.Notifications, "Alertas de stock", onVerAlertas, aviso = vm.alertas.size)
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
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    item { Resumen(vm, usuario) }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Accion(Icons.Filled.ShoppingCart, "Vender", Modifier.weight(1f), destacada = true, onClick = onVender)
                            Accion(Icons.Outlined.QrCodeScanner, "Escanear", Modifier.weight(1f), onClick = escanear)
                            Accion(Icons.Outlined.LocalShipping, "Reponer", Modifier.weight(1f), onClick = onReponer)
                            if (usuario.esPropietario) {
                                Accion(Icons.Outlined.AddBox, "Nuevo", Modifier.weight(1f)) { onNuevo(null) }
                            } else {
                                Accion(Icons.Outlined.History, "Actividad", Modifier.weight(1f), onClick = onVerActividad)
                            }
                        }
                    }
                    if (usuario.esPropietario && vm.actividad != null) {
                        item { TarjetaGrafica(vm) }
                    }
                    item { TarjetaAlertas(vm.alertas, onAbrir, onVerAlertas) }
                    if (vm.recientes.isNotEmpty()) {
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                                Text(
                                    "Actividad reciente",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.weight(1f).padding(start = 2.dp),
                                )
                                TextButton(onClick = onVerActividad) { Text("Ver todo") }
                            }
                        }
                        items(vm.recientes.take(5), key = { "m${it.id}" }) { m ->
                            FilaMovimiento(m, mostrarProducto = true, onClick = { onAbrir(m.productoId) })
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

/** Diferencia en % entre las ventas de hoy y las de ayer, o null si ayer no hubo ventas. */
private fun variacion(hoy: BigDecimal, ayer: BigDecimal?): Int? {
    if (ayer == null || ayer.signum() <= 0) return null
    return hoy.subtract(ayer).multiply(BigDecimal(100)).divide(ayer, 0, RoundingMode.HALF_UP).toInt()
}

/** Tarjeta oscura de arriba: ventas de hoy y cifras del periodo (propietario) o movimientos (empleado). */
@Composable
private fun Resumen(vm: PanelViewModel, usuario: Usuario) {
    val actividad = vm.actividad
    val oscuro = LocalOscuro.current
    val suave = Color.White.copy(alpha = 0.65f)
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = if (oscuro) MaterialTheme.colorScheme.surfaceContainerHigh else Tinta,
        border = if (oscuro) bordeTarjeta() else null,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp)) {
            if (usuario.esPropietario && actividad != null) {
                val ayer = actividad.dias.getOrNull(actividad.dias.size - 2)?.ventas
                val cambio = variacion(actividad.ventasHoy, ayer)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Ventas de hoy", style = MaterialTheme.typography.bodyMedium, color = suave, modifier = Modifier.weight(1f))
                    if (cambio != null) {
                        val sube = cambio >= 0
                        Text(
                            "${if (sube) "▲" else "▼"} ${kotlin.math.abs(cambio)} % vs. ayer",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (sube) MarcaClara else Color(0xFFFCA5A5),
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 9.dp, vertical = 4.dp),
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(dinero(actividad.ventasHoy), style = MaterialTheme.typography.displayMedium, color = Color.White)
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.12f))
                Spacer(Modifier.height(14.dp))
                Row {
                    Mini("Últimos ${vm.dias} días", dinero(actividad.ventasPeriodo), Modifier.weight(1.3f))
                    Mini("Unidades", "${actividad.unidadesVendidasPeriodo}", Modifier.weight(1f))
                    Mini("Movs. hoy", "${actividad.movimientosHoy}", Modifier.weight(1f))
                }
            } else {
                val hoy = vm.recientes.count { esHoy(it.fecha) }
                Text("Movimientos de hoy", style = MaterialTheme.typography.bodyMedium, color = suave)
                Spacer(Modifier.height(2.dp))
                Text("$hoy", style = MaterialTheme.typography.displayMedium, color = Color.White)
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.12f))
                Spacer(Modifier.height(14.dp))
                Row {
                    Mini("Con stock bajo", "${vm.alertas.size}", Modifier.weight(1f))
                    Mini("Agotados", "${vm.alertas.count { it.stockActual == 0 }}", Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun Mini(titulo: String, valor: String, modifier: Modifier) {
    Column(modifier) {
        Text(titulo, style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.6f), maxLines = 1)
        Text(valor, style = MaterialTheme.typography.titleMedium, color = Color.White, maxLines = 1)
    }
}

/** Acceso rápido: icono en un cuadrado y su nombre debajo. El destacado va en el color de acento. */
@Composable
private fun Accion(icono: ImageVector, texto: String, modifier: Modifier, destacada: Boolean = false, onClick: () -> Unit) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Surface(
            onClick = onClick,
            shape = MaterialTheme.shapes.large,
            color = if (destacada) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = if (destacada) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onSurface,
            border = if (destacada) null else bordeTarjeta(),
            modifier = Modifier.fillMaxWidth().height(60.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icono, contentDescription = null, modifier = Modifier.size(24.dp))
            }
        }
        Text(texto, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center, maxLines = 1)
    }
}

@Composable
private fun TarjetaGrafica(vm: PanelViewModel) {
    val actividad = vm.actividad ?: return
    val dias = actividad.dias
    Tarjeta(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Ventas", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(7, 14, 30).forEach { n ->
                        Chip(selected = vm.dias == n, onClick = { vm.cambiarDias(n) }, label = { Text("$n d") })
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
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
private fun TarjetaAlertas(alertas: List<Producto>, onAbrir: (Long) -> Unit, onVerTodas: () -> Unit) {
    val c = coloresEstado
    Tarjeta(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(vertical = 6.dp)) {
            Row(Modifier.padding(start = 16.dp, end = 8.dp, top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Hay que reponer", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.width(8.dp))
                if (alertas.isNotEmpty()) Pildora("${alertas.size}", c.aviso, c.avisoFondo, punto = false)
                Spacer(Modifier.weight(1f))
                if (alertas.isNotEmpty()) TextButton(onClick = onVerTodas) { Text("Ver todas") }
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
                alertas.take(3).forEachIndexed { i, p ->
                    if (i > 0) HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    Row(
                        Modifier.fillMaxWidth().clickable { onAbrir(p.id) }.padding(horizontal = 16.dp, vertical = 12.dp),
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
