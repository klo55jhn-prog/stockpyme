package com.stockpyme.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.History
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

/** Rutas de navegación de la app. */
object Rutas {
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

private val pestanas = listOf(
    Pestana(Rutas.PRODUCTOS, "Productos", Icons.Filled.Inventory2, Icons.Outlined.Inventory2),
    Pestana(Rutas.ALERTAS, "Alertas", Icons.Filled.NotificationsActive, Icons.Outlined.NotificationsActive),
    Pestana(Rutas.ACTIVIDAD, "Actividad", Icons.Filled.History, Icons.Outlined.History),
    Pestana(Rutas.MAS, "Más", Icons.Filled.AccountCircle, Icons.Outlined.AccountCircle),
)

@Composable
fun AppPrincipal(usuario: Usuario) {
    val nav = rememberNavController()
    val repo = repositorio()
    val entrada by nav.currentBackStackEntryAsState()
    val rutaActual = entrada?.destination?.route
    var alertas by remember { mutableIntStateOf(0) }

    // Al abrir la app comprueba que la sesión sigue siendo válida y si ha cambiado el rol.
    LaunchedEffect(Unit) { repo.refrescarUsuario() }
    // Número de la pestaña Alertas: se actualiza al cambiar de pantalla (por ejemplo, tras una salida).
    LaunchedEffect(rutaActual) { repo.bajoMinimo().onSuccess { alertas = it.size } }

    Scaffold(
        bottomBar = {
            if (pestanas.any { it.ruta == rutaActual }) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                    pestanas.forEach { pestana ->
                        val marcada = rutaActual == pestana.ruta
                        NavigationBarItem(
                            selected = marcada,
                            onClick = { irAPestana(nav, pestana.ruta) },
                            icon = {
                                BadgedBox(badge = {
                                    if (pestana.ruta == Rutas.ALERTAS && alertas > 0) Badge { Text("$alertas") }
                                }) {
                                    Icon(if (marcada) pestana.icono else pestana.iconoSinMarcar, contentDescription = null)
                                }
                            },
                            label = { Text(pestana.titulo) },
                        )
                    }
                }
            }
        },
    ) { margenes ->
        NavHost(
            navController = nav,
            startDestination = Rutas.PRODUCTOS,
            modifier = Modifier.padding(bottom = margenes.calculateBottomPadding()),
        ) {
            composable(Rutas.PRODUCTOS) {
                PantallaProductos(
                    usuario = usuario,
                    onAbrir = { nav.navigate(Rutas.detalle(it)) },
                    onNuevo = { codigo -> nav.navigate(Rutas.nuevo(codigo)) },
                )
            }
            composable(Rutas.ALERTAS) {
                PantallaAlertas(onAbrir = { nav.navigate(Rutas.detalle(it)) })
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
