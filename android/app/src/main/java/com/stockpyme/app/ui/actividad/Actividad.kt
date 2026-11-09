package com.stockpyme.app.ui.actividad

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stockpyme.app.datos.Movimiento
import com.stockpyme.app.datos.Repositorio
import com.stockpyme.app.ui.comun.EstadoLista
import com.stockpyme.app.ui.comun.Pantalla
import com.stockpyme.app.ui.comun.repositorio
import com.stockpyme.app.ui.productos.FilaMovimiento
import kotlinx.coroutines.launch

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaActividad(onAbrir: (Long) -> Unit) {
    val repo = repositorio()
    val vm: ActividadViewModel = viewModel { ActividadViewModel(repo) }
    LaunchedEffect(Unit) { vm.cargar() }

    Pantalla(titulo = "Actividad reciente") { margenes ->
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
                onReintentar = vm::cargar,
            ) {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(vm.movimientos, key = { it.id }) { m ->
                        FilaMovimiento(m, mostrarProducto = true, onClick = { onAbrir(m.productoId) })
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}
