package com.stockpyme.app.ui.actividad

import com.stockpyme.app.ui.comun.Chip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stockpyme.app.datos.Movimiento
import com.stockpyme.app.datos.Repositorio
import com.stockpyme.app.ui.comun.EstadoLista
import com.stockpyme.app.ui.comun.Pantalla
import com.stockpyme.app.ui.comun.TituloSeccion
import com.stockpyme.app.ui.comun.repositorio
import com.stockpyme.app.ui.productos.FilaMovimiento
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Últimos movimientos de todo el comercio. */
class ActividadViewModel(private val repo: Repositorio) : ViewModel() {
    var movimientos by mutableStateOf<List<Movimiento>>(emptyList())
        private set
    var cargando by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    /** null = todos; si no, ENTRADA, SALIDA o AJUSTE. */
    var filtro by mutableStateOf<String?>(null)

    val visibles: List<Movimiento>
        get() = filtro?.let { f -> movimientos.filter { it.tipo == f } } ?: movimientos

    fun cargar() {
        viewModelScope.launch {
            cargando = true
            repo.recientes()
                .onSuccess {
                    movimientos = it
                    error = null
                }
                .onFailure { error = it.message }
            cargando = false
        }
    }
}

private val formatoDia = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Locale.forLanguageTag("es-ES"))

/** "Hoy", "Ayer" o la fecha completa, en la zona horaria del móvil. */
private fun dia(iso: String): String = try {
    val fecha = Instant.parse(iso).atZone(ZoneId.systemDefault()).toLocalDate()
    val hoy = LocalDate.now()
    when (fecha) {
        hoy -> "Hoy"
        hoy.minusDays(1) -> "Ayer"
        else -> fecha.format(formatoDia)
    }
} catch (e: Exception) {
    ""
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaActividad(onAbrir: (Long) -> Unit) {
    val repo = repositorio()
    val vm: ActividadViewModel = viewModel { ActividadViewModel(repo) }
    LaunchedEffect(Unit) { vm.cargar() }

    Pantalla(titulo = "Actividad") { margenes ->
        Column(Modifier.padding(margenes).fillMaxSize()) {
        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf<Pair<String?, String>>(
                null to "Todo", "ENTRADA" to "Entradas", "SALIDA" to "Salidas", "AJUSTE" to "Ajustes",
            ).forEach { (tipo, texto) ->
                Chip(selected = vm.filtro == tipo, onClick = { vm.filtro = tipo }, label = { Text(texto) })
            }
        }
        PullToRefreshBox(
            isRefreshing = vm.cargando && vm.movimientos.isNotEmpty(),
            onRefresh = vm::cargar,
            modifier = Modifier.fillMaxSize(),
        ) {
            EstadoLista(
                cargando = vm.cargando,
                error = vm.error,
                vacia = vm.visibles.isEmpty(),
                textoVacio = if (vm.movimientos.isEmpty()) "Todavía no hay movimientos" else "No hay movimientos de este tipo",
                iconoVacio = Icons.Outlined.History,
                onReintentar = vm::cargar,
            ) {
                // Agrupados por día, conservando el orden (más reciente primero)
                val porDia = vm.visibles.groupBy { dia(it.fecha) }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    porDia.forEach { (titulo, lista) ->
                        item(key = "dia-$titulo") { TituloSeccion(titulo) }
                        items(lista, key = { it.id }) { m ->
                            FilaMovimiento(m, mostrarProducto = true, onClick = { onAbrir(m.productoId) })
                        }
                    }
                }
            }
        }
        }
    }
}
