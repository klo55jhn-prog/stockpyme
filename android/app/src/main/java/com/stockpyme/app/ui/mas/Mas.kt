package com.stockpyme.app.ui.mas

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.ReportProblem
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stockpyme.app.datos.Repositorio
import com.stockpyme.app.datos.Resumen
import com.stockpyme.app.datos.Usuario
import com.stockpyme.app.ui.Rutas
import com.stockpyme.app.ui.comun.Avatar
import com.stockpyme.app.ui.comun.EstadoLista
import com.stockpyme.app.ui.comun.FilaTarjeta
import com.stockpyme.app.ui.comun.IconoRedondo
import com.stockpyme.app.ui.comun.Logo
import com.stockpyme.app.ui.comun.Pantalla
import com.stockpyme.app.ui.comun.Pildora
import com.stockpyme.app.ui.comun.TarjetaDato
import com.stockpyme.app.ui.comun.TituloSeccion
import com.stockpyme.app.ui.comun.dinero
import com.stockpyme.app.ui.comun.repositorio
import com.stockpyme.app.ui.comun.sesion
import com.stockpyme.app.ui.tema.coloresEstado
import kotlinx.coroutines.launch

@Composable
fun PantallaMas(usuario: Usuario, onIr: (String) -> Unit) {
    val repo = repositorio()
    val datosSesion = sesion()
    var confirmarSalida by remember { mutableStateOf(false) }

    Pantalla(titulo = "Más") { margenes ->
        Column(
            Modifier
                .padding(margenes)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Perfil
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Avatar(usuario.nombre, tamano = 60.dp)
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            usuario.nombre,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Text(
                            usuario.email,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                        )
                        Spacer(Modifier.height(4.dp))
                        Pildora(
                            if (usuario.esPropietario) "Propietario" else "Empleado",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fondo = MaterialTheme.colorScheme.primary,
                            punto = false,
                        )
                    }
                }
            }

            if (usuario.esPropietario) {
                TituloSeccion("Gestión del comercio")
                Grupo(
                    listOf(
                        Opcion(Icons.Outlined.Assessment, "Resumen del inventario", "Valor del stock y lo más vendido") { onIr(Rutas.RESUMEN) },
                        Opcion(Icons.Outlined.Category, "Categorías", "Organiza tus productos") { onIr(Rutas.CATEGORIAS) },
                        Opcion(Icons.Outlined.LocalShipping, "Proveedores", "Contactos para reponer") { onIr(Rutas.PROVEEDORES) },
                        Opcion(Icons.Outlined.People, "Usuarios", "Cuentas de tus empleados") { onIr(Rutas.USUARIOS) },
                    ),
                )
            }

            TituloSeccion("Cuenta")
            Grupo(
                listOf(
                    Opcion(Icons.Outlined.Dns, "Servidor", datosSesion.urlServidor, flecha = false, onClick = null),
                    Opcion(
                        Icons.AutoMirrored.Outlined.Logout,
                        "Cerrar sesión",
                        null,
                        color = MaterialTheme.colorScheme.error,
                        flecha = false,
                    ) { confirmarSalida = true },
                ),
            )

            Column(
                Modifier.fillMaxWidth().padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Logo(36.dp)
                Spacer(Modifier.height(8.dp))
                Text("StockPyme 1.0", style = MaterialTheme.typography.labelLarge)
                Text(
                    "Inventario para pequeños comercios",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }

    if (confirmarSalida) {
        AlertDialog(
            onDismissRequest = { confirmarSalida = false },
            icon = { Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null) },
            title = { Text("Cerrar sesión") },
            text = { Text("¿Seguro que quieres salir? Tendrás que volver a escribir tu contraseña.") },
            confirmButton = { TextButton(onClick = { repo.cerrarSesion() }) { Text("Salir") } },
            dismissButton = { TextButton(onClick = { confirmarSalida = false }) { Text("Cancelar") } },
        )
    }
}

private class Opcion(
    val icono: ImageVector,
    val titulo: String,
    val detalle: String?,
    val color: Color? = null,
    val flecha: Boolean = true,
    val onClick: (() -> Unit)?,
)

/** Varias opciones dentro de una misma tarjeta, separadas por líneas. */
@Composable
private fun Grupo(opciones: List<Opcion>) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shadowElevation = 0.5.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            opciones.forEachIndexed { i, o ->
                val color = o.color ?: MaterialTheme.colorScheme.primary
                ListItem(
                    modifier = if (o.onClick != null) Modifier.clickable(onClick = o.onClick) else Modifier,
                    leadingContent = { IconoRedondo(o.icono, color) },
                    headlineContent = {
                        Text(o.titulo, fontWeight = FontWeight.Medium, color = o.color ?: Color.Unspecified)
                    },
                    supportingContent = o.detalle?.let { d -> { Text(d) } },
                    trailingContent = if (o.flecha) {
                        { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) }
                    } else {
                        null
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
                if (i < opciones.lastIndex) {
                    HorizontalDivider(Modifier.padding(start = 72.dp), color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
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
    val c = coloresEstado

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
                val maximo = r.masMovidos30Dias.maxOfOrNull { it.unidades }?.coerceAtLeast(1) ?: 1
                LazyColumn(
                    modifier = Modifier.padding(margenes).fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                TarjetaDato("Valor a precio de venta", dinero(r.valorVenta), Icons.Outlined.Sell, MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                                TarjetaDato("Valor a precio de coste", dinero(r.valorCompra), Icons.Outlined.Savings, Color(0xFF2563EB), Modifier.weight(1f))
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                TarjetaDato("Productos activos", r.productosActivos.toString(), Icons.Outlined.Inventory2, Color(0xFF7C3AED), Modifier.weight(1f))
                                TarjetaDato("Unidades en stock", r.unidadesEnStock.toString(), Icons.Outlined.Widgets, Color(0xFF0891B2), Modifier.weight(1f))
                            }
                            TarjetaDato(
                                "Productos en su stock mínimo",
                                r.productosBajoMinimo.toString(),
                                Icons.Outlined.ReportProblem,
                                if (r.productosBajoMinimo > 0) c.aviso else c.ok,
                                Modifier.fillMaxWidth(),
                            )
                        }
                    }
                    item { TituloSeccion("Más movidos · últimos 30 días") }
                    if (r.masMovidos30Dias.isEmpty()) {
                        item {
                            Text(
                                "Sin movimientos en los últimos 30 días",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(4.dp),
                            )
                        }
                    }
                    itemsIndexed(r.masMovidos30Dias, key = { _, p -> p.productoId }) { i, p ->
                        FilaTarjeta(
                            onClick = { onAbrir(p.productoId) },
                            inicio = {
                                Text(
                                    "${i + 1}",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp),
                                    textAlign = TextAlign.Center,
                                )
                            },
                            titulo = { Text(p.nombre, fontWeight = FontWeight.SemiBold) },
                            subtitulo = {
                                LinearProgressIndicator(
                                    progress = { p.unidades.toFloat() / maximo },
                                    strokeCap = StrokeCap.Round,
                                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp).height(6.dp),
                                )
                            },
                            fin = { Text("${p.unidades} u.", fontWeight = FontWeight.Bold) },
                        )
                    }
                }
            }
        }
    }
}
