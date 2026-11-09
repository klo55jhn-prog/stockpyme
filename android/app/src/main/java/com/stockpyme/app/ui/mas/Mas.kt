package com.stockpyme.app.ui.mas

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stockpyme.app.datos.Repositorio
import com.stockpyme.app.datos.Resumen
import com.stockpyme.app.datos.Usuario
import com.stockpyme.app.ui.Rutas
import com.stockpyme.app.ui.comun.EstadoLista
import com.stockpyme.app.ui.comun.Pantalla
import com.stockpyme.app.ui.comun.dinero
import com.stockpyme.app.ui.comun.repositorio
import com.stockpyme.app.ui.comun.sesion
import kotlinx.coroutines.launch

@Composable
fun PantallaMas(usuario: Usuario, onIr: (String) -> Unit) {
    val repo = repositorio()
    val datosSesion = sesion()
    var confirmarSalida by remember { mutableStateOf(false) }

    Pantalla(titulo = "Más") { margenes ->
        Column(Modifier.padding(margenes).fillMaxSize().verticalScroll(rememberScrollState())) {
            Card(Modifier.fillMaxWidth().padding(16.dp)) {
                Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Icon(Icons.Filled.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column {
                        Text(usuario.nombre, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(usuario.email)
                        Text(if (usuario.esPropietario) "Propietario" else "Empleado", color = MaterialTheme.colorScheme.primary)
                        Text(
                            "Servidor: ${datosSesion.urlServidor}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            if (usuario.esPropietario) {
                Opcion(Icons.Filled.Assessment, "Resumen del inventario", "Valor del stock y productos con más movimiento") { onIr(Rutas.RESUMEN) }
                Opcion(Icons.Filled.Category, "Categorías", "Crear, renombrar y borrar categorías") { onIr(Rutas.CATEGORIAS) }
                Opcion(Icons.Filled.LocalShipping, "Proveedores", "Datos de contacto de los proveedores") { onIr(Rutas.PROVEEDORES) }
                Opcion(Icons.Filled.People, "Usuarios", "Cuentas de los empleados") { onIr(Rutas.USUARIOS) }
            }
            Opcion(Icons.AutoMirrored.Filled.Logout, "Cerrar sesión", null) { confirmarSalida = true }
        }
    }

    if (confirmarSalida) {
        AlertDialog(
            onDismissRequest = { confirmarSalida = false },
            title = { Text("Cerrar sesión") },
            text = { Text("¿Seguro que quieres salir?") },
            confirmButton = { TextButton(onClick = { repo.cerrarSesion() }) { Text("Salir") } },
            dismissButton = { TextButton(onClick = { confirmarSalida = false }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun Opcion(icono: ImageVector, titulo: String, detalle: String?, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = { Icon(icono, contentDescription = null) },
        headlineContent = { Text(titulo) },
        supportingContent = if (detalle != null) {
            { Text(detalle) }
        } else {
            null
        },
    )
    HorizontalDivider()
}

// ---------- RF-13 Resumen del inventario ----------

class ResumenViewModel(private val repo: Repositorio) : ViewModel() {
    var resumen by mutableStateOf<Resumen?>(null)
        private set
    var cargando by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    fun cargar() {
        viewModelScope.launch {
            cargando = true
            repo.resumen()
                .onSuccess {
                    resumen = it
                    error = null
                }
                .onFailure { error = it.message }
            cargando = false
        }
    }
}

@Composable
fun PantallaResumen(onAtras: () -> Unit, onAbrir: (Long) -> Unit) {
    val repo = repositorio()
    val vm: ResumenViewModel = viewModel { ResumenViewModel(repo) }
    LaunchedEffect(Unit) { vm.cargar() }

    Pantalla(titulo = "Resumen del inventario", onAtras = onAtras) { margenes ->
        val r = vm.resumen
        EstadoLista(
            cargando = vm.cargando,
            error = vm.error,
            vacia = r == null,
            textoVacio = "",
            onReintentar = vm::cargar,
            modifier = Modifier.padding(margenes),
        ) {
            if (r != null) {
                LazyColumn(Modifier.padding(margenes).fillMaxSize()) {
                    item {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Tarjeta("Valor a precio de coste", dinero(r.valorCompra), Modifier.weight(1f))
                                Tarjeta("Valor a precio de venta", dinero(r.valorVenta), Modifier.weight(1f))
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Tarjeta("Productos activos", r.productosActivos.toString(), Modifier.weight(1f))
                                Tarjeta("Unidades en stock", r.unidadesEnStock.toString(), Modifier.weight(1f))
                            }
                            Tarjeta("Productos en stock mínimo", r.productosBajoMinimo.toString(), Modifier.fillMaxWidth())
                            Text(
                                "Más movidos en los últimos 30 días",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                            if (r.masMovidos30Dias.isEmpty()) {
                                Text("Sin movimientos en los últimos 30 días")
                            }
                        }
                    }
                    items(r.masMovidos30Dias, key = { it.productoId }) { p ->
                        ListItem(
                            modifier = Modifier.clickable { onAbrir(p.productoId) },
                            headlineContent = { Text(p.nombre) },
                            trailingContent = { Text("${p.unidades} u.", fontWeight = FontWeight.Bold) },
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun Tarjeta(titulo: String, valor: String, modifier: Modifier) {
    Card(modifier) {
        Column(Modifier.padding(16.dp)) {
            Text(titulo, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(valor, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}
