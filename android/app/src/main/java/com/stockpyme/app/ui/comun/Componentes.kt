package com.stockpyme.app.ui.comun

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.stockpyme.app.R
import com.stockpyme.app.StockPymeApp
import com.stockpyme.app.datos.Repositorio
import com.stockpyme.app.datos.Sesion
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

// ---------- Acceso a los datos desde la interfaz ----------

fun Context.repositorio(): Repositorio = (applicationContext as StockPymeApp).contenedor.repositorio

@Composable
fun repositorio(): Repositorio = LocalContext.current.repositorio()

@Composable
fun sesion(): Sesion = (LocalContext.current.applicationContext as StockPymeApp).contenedor.sesion

// ---------- Formatos ----------

private val formatoDinero = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-ES"))
private val formatoFecha = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
private val formatoHora = DateTimeFormatter.ofPattern("HH:mm")

fun dinero(valor: BigDecimal): String = formatoDinero.format(valor)

/** La API manda las fechas en UTC (ISO 8601). Se muestran en la hora local del móvil. */
fun fechaLocal(iso: String): String = try {
    Instant.parse(iso).atZone(ZoneId.systemDefault()).format(formatoFecha)
} catch (e: Exception) {
    iso
}

/** Solo la hora local ("14:05"), para listas que ya están agrupadas por día. */
fun horaLocal(iso: String): String = try {
    Instant.parse(iso).atZone(ZoneId.systemDefault()).format(formatoHora)
} catch (e: Exception) {
    iso
}

/** Acepta "12,50" y "12.50". Devuelve null si no es un número. */
fun leerDecimal(texto: String): BigDecimal? = texto.trim().replace(',', '.').toBigDecimalOrNull()

/** Iniciales para los avatares: "Martillo de carpintero" -> "MC". */
fun iniciales(texto: String): String =
    texto.split(' ').filter { it.isNotBlank() && it.first().isLetterOrDigit() }
        .take(2).joinToString("") { it.first().uppercase() }.ifEmpty { "?" }

/** Paleta suave para avatares. Cada texto tiene siempre el mismo color. */
private val coloresAvatar = listOf(
    Color(0xFF0F766E), Color(0xFF2563EB), Color(0xFF7C3AED), Color(0xFFDB2777),
    Color(0xFFEA580C), Color(0xFF0891B2), Color(0xFF65A30D), Color(0xFF9333EA),
)

fun colorPara(texto: String): Color = coloresAvatar[abs(texto.hashCode()) % coloresAvatar.size]

// ---------- Estructura de pantalla ----------

/**
 * Estructura común de cada pantalla: barra superior, mensajes y botón flotante.
 * Las pestañas principales usan una barra con título grande que se encoge al hacer scroll.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Pantalla(
    titulo: String,
    onAtras: (() -> Unit)? = null,
    snackbar: SnackbarHostState = remember { SnackbarHostState() },
    grande: Boolean = onAtras == null,
    acciones: @Composable RowScope.() -> Unit = {},
    botonFlotante: @Composable () -> Unit = {},
    contenido: @Composable (PaddingValues) -> Unit,
) {
    val scroll = if (grande) {
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    } else {
        TopAppBarDefaults.pinnedScrollBehavior()
    }
    val colores = TopAppBarDefaults.topAppBarColors(
        containerColor = MaterialTheme.colorScheme.background,
        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
    )
    val atras: @Composable () -> Unit = {
        if (onAtras != null) {
            IconButton(onClick = onAtras) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
            }
        }
    }
    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        // Los márgenes del sistema (barra de estado y de navegación) ya los gestiona AppPrincipal
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            if (grande) {
                LargeTopAppBar(
                    title = { Text(titulo, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold) },
                    navigationIcon = atras,
                    actions = acciones,
                    colors = colores,
                    scrollBehavior = scroll,
                )
            } else {
                TopAppBar(
                    title = { Text(titulo, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    navigationIcon = atras,
                    actions = acciones,
                    colors = colores,
                    scrollBehavior = scroll,
                )
            }
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
    iconoVacio: ImageVector = Icons.Outlined.Inbox,
    contenido: @Composable () -> Unit,
) {
    when {
        error != null && vacia -> Centrado(modifier) {
            IconoRedondo(Icons.Outlined.CloudOff, MaterialTheme.colorScheme.error, tamano = 72.dp)
            Text("No se pudo cargar", style = MaterialTheme.typography.titleMedium)
            Text(error, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FilledTonalButton(onClick = onReintentar) { Text("Reintentar") }
        }
        cargando && vacia -> Centrado(modifier) { CircularProgressIndicator() }
        vacia -> Centrado(modifier) {
            IconoRedondo(iconoVacio, MaterialTheme.colorScheme.primary, tamano = 72.dp)
            Text(textoVacio, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        else -> contenido()
    }
}

@Composable
private fun Centrado(modifier: Modifier, contenido: @Composable () -> Unit) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) { contenido() }
}

// ---------- Piezas visuales ----------

/** Logo de la app (el mismo dibujo que el icono del móvil). */
@Composable
fun Logo(tamano: Dp, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.logo_stockpyme),
        contentDescription = "Logo de StockPyme",
        modifier = modifier.size(tamano),
    )
}

/** Círculo con iniciales y un color que depende del texto. */
@Composable
fun Avatar(texto: String, modifier: Modifier = Modifier, tamano: Dp = 44.dp, clave: String = texto) {
    val color = colorPara(clave)
    Box(
        modifier = modifier.size(tamano).background(color.copy(alpha = 0.14f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            iniciales(texto),
            color = color,
            style = if (tamano >= 56.dp) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
        )
    }
}

/** Icono dentro de un círculo de su mismo color, más claro. */
@Composable
fun IconoRedondo(icono: ImageVector, color: Color, modifier: Modifier = Modifier, tamano: Dp = 40.dp) {
    Box(
        modifier = modifier.size(tamano).background(color.copy(alpha = 0.14f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icono, contentDescription = null, tint = color, modifier = Modifier.size(tamano * 0.5f))
    }
}

/** Etiqueta redondeada de color, por ejemplo para el estado del stock o el rol. */
@Composable
fun Pildora(texto: String, color: Color, fondo: Color, modifier: Modifier = Modifier, punto: Boolean = true) {
    Row(
        modifier = modifier.background(fondo, CircleShape).padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (punto) Box(Modifier.size(7.dp).background(color, CircleShape))
        Text(texto, color = color, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
    }
}

/** Fila con forma de tarjeta para las listas. */
@Composable
fun FilaTarjeta(
    titulo: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    subtitulo: (@Composable () -> Unit)? = null,
    inicio: (@Composable () -> Unit)? = null,
    fin: (@Composable () -> Unit)? = null,
) {
    val forma = MaterialTheme.shapes.large
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = forma,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 0.dp,
        shadowElevation = 0.5.dp,
    ) {
        ListItem(
            modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
            headlineContent = titulo,
            supportingContent = subtitulo,
            leadingContent = inicio,
            trailingContent = fin,
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        )
    }
}

/** Título de un bloque dentro de una pantalla. */
@Composable
fun TituloSeccion(texto: String, modifier: Modifier = Modifier) {
    Text(
        texto.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(top = 12.dp, bottom = 4.dp, start = 4.dp),
    )
}

/** Tarjeta de un indicador (KPI): icono, valor grande y descripción. */
@Composable
fun TarjetaDato(
    titulo: String,
    valor: String,
    icono: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shadowElevation = 0.5.dp,
    ) {
        Column(Modifier.padding(16.dp)) {
            IconoRedondo(icono, color, tamano = 36.dp)
            Spacer(Modifier.size(12.dp))
            Text(valor, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(titulo, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
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
            shape = MaterialTheme.shapes.medium,
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
