package com.stockpyme.app.ui.productos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stockpyme.app.datos.Categoria
import com.stockpyme.app.datos.ProductoRequest
import com.stockpyme.app.datos.Proveedor
import com.stockpyme.app.datos.Repositorio
import com.stockpyme.app.ui.comun.EstadoLista
import com.stockpyme.app.ui.comun.MostrarMensaje
import com.stockpyme.app.ui.comun.Pantalla
import com.stockpyme.app.ui.comun.Selector
import com.stockpyme.app.ui.comun.leerDecimal
import com.stockpyme.app.ui.comun.rememberEscaner
import com.stockpyme.app.ui.comun.repositorio
import java.math.BigDecimal
import kotlinx.coroutines.launch

/** CU-03 Crear producto y CU-04 Editar o desactivar producto. */
class FormularioViewModel(
    private val repo: Repositorio,
    private val productoId: Long?,
    codigoInicial: String?,
) : ViewModel() {

    var nombre by mutableStateOf("")
    var codigo by mutableStateOf(codigoInicial ?: "")
    var precioCompra by mutableStateOf("")
    var precioVenta by mutableStateOf("")
    var stockMinimo by mutableStateOf("0")
    var categoria by mutableStateOf<Categoria?>(null)
    var proveedor by mutableStateOf<Proveedor?>(null)
    var activo by mutableStateOf(true)

    var categorias by mutableStateOf<List<Categoria>>(emptyList())
        private set
    var proveedores by mutableStateOf<List<Proveedor>>(emptyList())
        private set
    var cargando by mutableStateOf(true)
        private set
    var errorCarga by mutableStateOf<String?>(null)
        private set
    var guardando by mutableStateOf(false)
        private set
    var mensaje by mutableStateOf<String?>(null)

    val esNuevo get() = productoId == null
    private var cargado = false

    fun cargar() {
        viewModelScope.launch {
            cargando = true
            val cats = repo.categorias()
            val provs = repo.proveedores(incluirInactivos = true)
            categorias = cats.getOrDefault(emptyList())
            proveedores = provs.getOrDefault(emptyList())
            errorCarga = cats.exceptionOrNull()?.message ?: provs.exceptionOrNull()?.message

            // Al editar, rellena el formulario solo la primera vez (no pisa lo que se esté escribiendo).
            if (productoId != null && !cargado && errorCarga == null) {
                repo.producto(productoId)
                    .onSuccess { p ->
                        nombre = p.nombre
                        codigo = p.codigoBarras ?: ""
                        precioCompra = p.precioCompra.toPlainString()
                        precioVenta = p.precioVenta.toPlainString()
                        stockMinimo = p.stockMinimo.toString()
                        categoria = categorias.find { it.id == p.categoriaId }
                        proveedor = proveedores.find { it.id == p.proveedorId }
                        activo = p.activo
                        cargado = true
                    }
                    .onFailure { errorCarga = it.message }
            } else if (productoId == null && categoria == null && categorias.size == 1) {
                categoria = categorias.first()
            }
            cargando = false
        }
    }

    /** Valida en el móvil antes de enviar. El servidor vuelve a validar todo igualmente. */
    fun guardar(onGuardado: (Long) -> Unit) {
        val compra = leerDecimal(precioCompra)
        val venta = leerDecimal(precioVenta)
        val minimo = stockMinimo.trim().toIntOrNull()
        val cat = categoria
        mensaje = when {
            nombre.isBlank() -> "El nombre es obligatorio"
            compra == null || compra < BigDecimal.ZERO -> "El precio de compra no es válido"
            venta == null || venta < BigDecimal.ZERO -> "El precio de venta no es válido"
            minimo == null || minimo < 0 -> "El stock mínimo debe ser 0 o más"
            cat == null -> "Elige una categoría"
            else -> null
        }
        if (mensaje != null || compra == null || venta == null || minimo == null || cat == null) return

        val datos = ProductoRequest(
            nombre = nombre.trim(),
            codigoBarras = codigo.trim().ifBlank { null },
            precioCompra = compra,
            precioVenta = venta,
            stockMinimo = minimo,
            categoriaId = cat.id,
            proveedorId = proveedor?.id,
            activo = if (esNuevo) null else activo,
        )
        guardando = true
        viewModelScope.launch {
            val resultado = if (productoId == null) repo.crearProducto(datos) else repo.editarProducto(productoId, datos)
            resultado
                .onSuccess { onGuardado(it.id) }
                .onFailure { mensaje = it.message }
            guardando = false
        }
    }
}

@Composable
fun PantallaFormulario(
    productoId: Long?,
    codigoInicial: String?,
    onAtras: () -> Unit,
    onIrACategorias: () -> Unit,
    onGuardado: (Long) -> Unit,
) {
    val repo = repositorio()
    val vm: FormularioViewModel = viewModel { FormularioViewModel(repo, productoId, codigoInicial) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { vm.cargar() }
    MostrarMensaje(vm.mensaje, snackbar) { vm.mensaje = null }

    val escanear = rememberEscaner(onCodigo = { vm.codigo = it }, onError = { vm.mensaje = it })

    Pantalla(
        titulo = if (vm.esNuevo) "Nuevo producto" else "Editar producto",
        onAtras = onAtras,
        snackbar = snackbar,
    ) { margenes ->
        EstadoLista(
            cargando = vm.cargando,
            error = vm.errorCarga,
            vacia = vm.cargando || vm.errorCarga != null,
            textoVacio = "",
            onReintentar = vm::cargar,
            modifier = Modifier.padding(margenes),
        ) {
            Column(
                modifier = Modifier
                    .padding(margenes)
                    .fillMaxSize()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (vm.categorias.isEmpty()) {
                    Text(
                        "Antes de crear productos hace falta al menos una categoría.",
                        color = MaterialTheme.colorScheme.error,
                    )
                    OutlinedButton(onClick = onIrACategorias) { Text("Crear categorías") }
                }

                OutlinedTextField(
                    value = vm.nombre,
                    onValueChange = { vm.nombre = it.take(150) },
                    label = { Text("Nombre *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = vm.codigo,
                    onValueChange = { vm.codigo = it.trim().take(50) },
                    label = { Text("Código de barras (opcional)") },
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = escanear) {
                            Icon(Icons.Filled.QrCodeScanner, contentDescription = "Escanear código")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = vm.precioCompra,
                        onValueChange = { vm.precioCompra = it.take(12) },
                        label = { Text("Precio compra *") },
                        suffix = { Text("€") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = vm.precioVenta,
                        onValueChange = { vm.precioVenta = it.take(12) },
                        label = { Text("Precio venta *") },
                        suffix = { Text("€") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                    )
                }
                OutlinedTextField(
                    value = vm.stockMinimo,
                    onValueChange = { vm.stockMinimo = it.filter(Char::isDigit).take(7) },
                    label = { Text("Stock mínimo (aviso) *") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                Selector(
                    etiqueta = "Categoría *",
                    opciones = vm.categorias,
                    seleccionado = vm.categoria,
                    texto = { it.nombre },
                    onSeleccion = { vm.categoria = it },
                    modifier = Modifier.fillMaxWidth(),
                )
                Selector(
                    etiqueta = "Proveedor",
                    opciones = vm.proveedores.filter { it.activo || it.id == vm.proveedor?.id },
                    seleccionado = vm.proveedor,
                    texto = { if (it.activo) it.nombre else "${it.nombre} (desactivado)" },
                    onSeleccion = { vm.proveedor = it },
                    textoNinguno = "Sin proveedor",
                    modifier = Modifier.fillMaxWidth(),
                )
                if (!vm.esNuevo) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Producto activo")
                            Text(
                                "Desactivado no aparece en las búsquedas ni admite movimientos, pero conserva su historial.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(checked = vm.activo, onCheckedChange = { vm.activo = it })
                    }
                } else {
                    Text(
                        "El producto se crea con stock 0. Después registra una entrada con el stock inicial: " +
                            "así queda constancia de quién lo añadió y cuándo.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Button(
                    onClick = { vm.guardar(onGuardado) },
                    enabled = !vm.guardando,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) {
                    if (vm.guardando) {
                        CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else {
                        Text(if (vm.esNuevo) "Crear producto" else "Guardar cambios")
                    }
                }
            }
        }
    }
}
