package com.stockpyme.app.ui.comun

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.stockpyme.app.StockPymeApp
import com.stockpyme.app.datos.Repositorio
import com.stockpyme.app.datos.Sesion
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// ---------- Acceso a los datos desde la interfaz ----------

fun Context.repositorio(): Repositorio = (applicationContext as StockPymeApp).contenedor.repositorio

@Composable
fun repositorio(): Repositorio = LocalContext.current.repositorio()

@Composable
fun sesion(): Sesion = (LocalContext.current.applicationContext as StockPymeApp).contenedor.sesion

// ---------- Formatos ----------

private val formatoDinero = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-ES"))
private val formatoFecha = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

fun dinero(valor: BigDecimal): String = formatoDinero.format(valor)

/** La API manda las fechas en UTC (ISO 8601). Se muestran en la hora local del móvil. */
fun fechaLocal(iso: String): String = try {
    Instant.parse(iso).atZone(ZoneId.systemDefault()).format(formatoFecha)
} catch (e: Exception) {
    iso
}

/** Acepta "12,50" y "12.50". Devuelve null si no es un número. */
fun leerDecimal(texto: String): BigDecimal? = texto.trim().replace(',', '.').toBigDecimalOrNull()

// ---------- Componentes ----------

/** Estructura común de cada pantalla: barra superior, mensajes y botón flotante. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Pantalla(
    titulo: String,
    onAtras: (() -> Unit)? = null,
    snackbar: SnackbarHostState = remember { SnackbarHostState() },
    acciones: @Composable RowScope.() -> Unit = {},
    botonFlotante: @Composable () -> Unit = {},
    contenido: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        // Los márgenes del sistema (barra de estado y de navegación) ya los gestiona AppPrincipal
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text(titulo, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    if (onAtras != null) {
                        IconButton(onClick = onAtras) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                        }
                    }
                },
                actions = acciones,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = botonFlotante,
        content = contenido,
    )
}

/** Muestra [mensaje] en la barra inferior y avisa cuando se ha mostrado para borrarlo. */
@Composable
fun MostrarMensaje(mensaje: String?, snackbar: SnackbarHostState, alMostrar: () -> Unit) {
    LaunchedEffect(mensaje) {
        if (mensaje != null) {
            snackbar.showSnackbar(mensaje)
            alMostrar()
        }
    }
}

/** Contenido de una lista mientras carga, si falla o si está vacía. */
@Composable
fun EstadoLista(
    cargando: Boolean,
    error: String?,
    vacia: Boolean,
    textoVacio: String,
    onReintentar: () -> Unit,
    modifier: Modifier = Modifier,
    contenido: @Composable () -> Unit,
) {
    when {
        error != null && vacia -> Centrado(modifier) {
            Text(error, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.error)
            Button(onClick = onReintentar) { Text("Reintentar") }
        }
        cargando && vacia -> Centrado(modifier) { CircularProgressIndicator() }
        vacia -> Centrado(modifier) {
            Text(textoVacio, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        else -> contenido()
    }
}

@Composable
private fun Centrado(modifier: Modifier, contenido: @Composable () -> Unit) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) { contenido() }
}

/** Campo de selección (desplegable) de solo lectura. */
@Composable
fun <T> Selector(
    etiqueta: String,
    opciones: List<T>,
    seleccionado: T?,
    texto: (T) -> String,
    onSeleccion: (T?) -> Unit,
    modifier: Modifier = Modifier,
    textoNinguno: String? = null,
) {
    var abierto by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedTextField(
            value = seleccionado?.let(texto) ?: (textoNinguno ?: ""),
            onValueChange = {},
            readOnly = true,
            label = { Text(etiqueta) },
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
        )
        // Capa transparente encima del campo para abrir el menú al tocar
        Box(Modifier.matchParentSize().clickable { abierto = true })
        DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            if (textoNinguno != null) {
                DropdownMenuItem(text = { Text(textoNinguno) }, onClick = {
                    onSeleccion(null)
                    abierto = false
                })
            }
            opciones.forEach { opcion ->
                DropdownMenuItem(text = { Text(texto(opcion)) }, onClick = {
                    onSeleccion(opcion)
                    abierto = false
                })
            }
        }
    }
}

/**
 * Devuelve una función que abre el escáner de códigos de barras de Google Play Services (RF-12).
 * No necesita pedir permiso de cámara: el escáner lo gestiona Google.
 */
@Composable
fun rememberEscaner(onCodigo: (String) -> Unit, onError: (String) -> Unit): () -> Unit {
    val context = LocalContext.current
    return {
        GmsBarcodeScanning.getClient(context).startScan()
            .addOnSuccessListener { codigo -> codigo.rawValue?.let(onCodigo) }
            .addOnFailureListener { e -> onError("No se pudo abrir el escáner: ${e.localizedMessage}") }
    }
}
