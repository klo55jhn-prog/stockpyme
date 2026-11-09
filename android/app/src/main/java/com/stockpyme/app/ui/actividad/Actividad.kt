package com.stockpyme.app.ui.actividad

import androidx.compose.foundation.layout.Arrangement
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
        PullToRefreshBox(
            isRefreshing = vm.cargando && vm.movimientos.isNotEmpty(),
            onRefresh = vm::cargar,
            modifier = Modifier.padding(margenes).fillMaxSize(),
        ) {
            EstadoLista(
                cargando = vm.cargando,
                error = vm.error,
                vacia = vm.movimientos.isEmpty(),
                textoVacio = "Todavía no hay movimientos",
                iconoVacio = Icons.Outlined.History,
                onReintentar = vm::cargar,
            ) {
                // Agrupados por día, conservando el orden (más reciente primero)
                val porDia = vm.movimientos.groupBy { dia(it.fecha) }
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
