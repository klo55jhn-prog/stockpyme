package com.stockpyme.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.stockpyme.app.datos.Usuario
import com.stockpyme.app.ui.actividad.PantallaActividad
import com.stockpyme.app.ui.admin.PantallaCategorias
import com.stockpyme.app.ui.admin.PantallaProveedores
import com.stockpyme.app.ui.admin.PantallaUsuarios
import com.stockpyme.app.ui.comun.repositorio
import com.stockpyme.app.ui.mas.PantallaMas
import com.stockpyme.app.ui.mas.PantallaResumen
import com.stockpyme.app.ui.productos.PantallaAlertas
import com.stockpyme.app.ui.productos.PantallaDetalle
import com.stockpyme.app.ui.productos.PantallaFormulario
import com.stockpyme.app.ui.productos.PantallaProductos
import com.stockpyme.app.ui.panel.PantallaPanel
import com.stockpyme.app.ui.venta.PantallaVenta
import com.stockpyme.app.ui.reposicion.PantallaReposicion

/** Rutas de navegación de la app. */
object Rutas {
    const val INICIO = "inicio"
    const val VENTA = "venta"
    const val REPOSICION = "reposicion"
    const val PRODUCTOS = "productos"
    const val ALERTAS = "alertas"
    const val ACTIVIDAD = "actividad"
    const val MAS = "mas"
    const val DETALLE = "producto/{id}"
    const val NUEVO = "producto-nuevo?codigo={codigo}"
    const val EDITAR = "producto/{id}/editar"
    const val CATEGORIAS = "categorias"
    const val PROVEEDORES = "proveedores"
    const val USUARIOS = "usuarios"
    const val RESUMEN = "resumen"

    fun detalle(id: Long) = "producto/$id"
    fun nuevo(codigo: String? = null) = if (codigo == null) "producto-nuevo" else "producto-nuevo?codigo=$codigo"
    fun editar(id: Long) = "producto/$id/editar"
}

private data class Pestana(
    val ruta: String,
    val titulo: String,
    val icono: ImageVector,
    val iconoSinMarcar: ImageVector,
)

// Cuatro pestañas y, en el centro, el botón de vender (lo que más se hace en una tienda).
private val pestanas = listOf(
    Pestana(Rutas.INICIO, "Inicio", Icons.Filled.Home, Icons.Outlined.Home),
    Pestana(Rutas.PRODUCTOS, "Productos", Icons.Filled.Inventory2, Icons.Outlined.Inventory2),
    Pestana(Rutas.ACTIVIDAD, "Actividad", Icons.Filled.History, Icons.Outlined.History),
    Pestana(Rutas.MAS, "Más", Icons.Filled.AccountCircle, Icons.Outlined.AccountCircle),
)

@Composable
fun AppPrincipal(usuario: Usuario) {
    val nav = rememberNavController()
    val repo = repositorio()
    val entrada by nav.currentBackStackEntryAsState()
    val rutaActual = entrada?.destination?.route

    // Al abrir la app comprueba que la sesión sigue siendo válida y si ha cambiado el rol.
    LaunchedEffect(Unit) { repo.refrescarUsuario() }

    Scaffold(
        bottomBar = {
            if (pestanas.any { it.ruta == rutaActual }) {
                BarraInferior(rutaActual, onPestana = { irAPestana(nav, it) }, onVender = { nav.navigate(Rutas.VENTA) })
            }
        },
    ) { margenes ->
        NavHost(
            navController = nav,
            startDestination = Rutas.INICIO,
            modifier = Modifier.padding(bottom = margenes.calculateBottomPadding()),
        ) {
            composable(Rutas.INICIO) {
                PantallaPanel(
                    usuario = usuario,
                    onAbrir = { nav.navigate(Rutas.detalle(it)) },
                    onVender = { nav.navigate(Rutas.VENTA) },
                    onReponer = { nav.navigate(Rutas.REPOSICION) },
                    onNuevo = { codigo -> nav.navigate(Rutas.nuevo(codigo)) },
                    onVerAlertas = { nav.navigate(Rutas.ALERTAS) },
                    onVerActividad = { irAPestana(nav, Rutas.ACTIVIDAD) },
                )
            }
            composable(Rutas.VENTA) { PantallaVenta(onAtras = { nav.popBackStack() }) }
            composable(Rutas.REPOSICION) { PantallaReposicion(onAtras = { nav.popBackStack() }) }
            composable(Rutas.PRODUCTOS) {
                PantallaProductos(
                    usuario = usuario,
                    onAbrir = { nav.navigate(Rutas.detalle(it)) },
                    onNuevo = { codigo -> nav.navigate(Rutas.nuevo(codigo)) },
                )
            }
            composable(Rutas.ALERTAS) {
                PantallaAlertas(onAtras = { nav.popBackStack() }, onAbrir = { nav.navigate(Rutas.detalle(it)) })
            }
            composable(Rutas.ACTIVIDAD) {
                PantallaActividad(onAbrir = { nav.navigate(Rutas.detalle(it)) })
            }
            composable(Rutas.MAS) {
                PantallaMas(usuario = usuario, onIr = { nav.navigate(it) })
            }
            composable(
                Rutas.DETALLE,
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) { e ->
                val id = e.arguments?.getLong("id") ?: 0L
                PantallaDetalle(
                    productoId = id,
                    usuario = usuario,
                    onAtras = { nav.popBackStack() },
                    onEditar = { nav.navigate(Rutas.editar(id)) },
                )
            }
            composable(
                Rutas.NUEVO,
                arguments = listOf(navArgument("codigo") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }),
            ) { e ->
                PantallaFormulario(
                    productoId = null,
                    codigoInicial = e.arguments?.getString("codigo"),
                    onAtras = { nav.popBackStack() },
                    onIrACategorias = { nav.navigate(Rutas.CATEGORIAS) },
                    onGuardado = { id ->
                        // CU-03 paso 6: tras crear el producto se abre su ficha para registrar el stock inicial
                        nav.popBackStack()
                        nav.navigate(Rutas.detalle(id))
                    },
                )
            }
            composable(
                Rutas.EDITAR,
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) { e ->
                PantallaFormulario(
                    productoId = e.arguments?.getLong("id"),
                    codigoInicial = null,
                    onAtras = { nav.popBackStack() },
                    onIrACategorias = { nav.navigate(Rutas.CATEGORIAS) },
                    onGuardado = { nav.popBackStack() },
                )
            }
            composable(Rutas.CATEGORIAS) { PantallaCategorias(onAtras = { nav.popBackStack() }) }
            composable(Rutas.PROVEEDORES) { PantallaProveedores(onAtras = { nav.popBackStack() }) }
            composable(Rutas.USUARIOS) { PantallaUsuarios(usuario, onAtras = { nav.popBackStack() }) }
            composable(Rutas.RESUMEN) {
                PantallaResumen(onAtras = { nav.popBackStack() }, onAbrir = { nav.navigate(Rutas.detalle(it)) })
            }
        }
    }
}

private fun irAPestana(nav: NavHostController, ruta: String) {
    nav.navigate(ruta) {
        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/** Barra inferior propia: pestañas a los lados y el botón de vender, más grande, en el centro. */
@Composable
private fun BarraInferior(rutaActual: String?, onPestana: (String) -> Unit, onVender: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                Modifier.fillMaxWidth().navigationBarsPadding().height(68.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                pestanas.take(2).forEach { ItemPestana(it, rutaActual == it.ruta, Modifier.weight(1f)) { onPestana(it.ruta) } }
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Surface(
                        onClick = onVender,
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.tertiary,
                        contentColor = MaterialTheme.colorScheme.onTertiary,
                        shadowElevation = 6.dp,
                        modifier = Modifier.size(54.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.ShoppingCart, contentDescription = "Nueva venta", modifier = Modifier.size(26.dp))
                        }
                    }
                }
                pestanas.drop(2).forEach { ItemPestana(it, rutaActual == it.ruta, Modifier.weight(1f)) { onPestana(it.ruta) } }
            }
        }
    }
}

@Composable
private fun ItemPestana(pestana: Pestana, marcada: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val color = if (marcada) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier
            .fillMaxHeight()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Tab,
                onClick = onClick,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(if (marcada) pestana.icono else pestana.iconoSinMarcar, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(3.dp))
        Text(
            pestana.titulo,
            color = color,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (marcada) FontWeight.Bold else FontWeight.Medium,
        )
    }
}
