package com.stockpyme.app.ui.admin

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stockpyme.app.datos.Categoria
import com.stockpyme.app.datos.Proveedor
import com.stockpyme.app.datos.ProveedorRequest
import com.stockpyme.app.datos.Repositorio
import com.stockpyme.app.datos.Usuario
import com.stockpyme.app.datos.UsuarioRequest
import com.stockpyme.app.ui.comun.EstadoLista
import com.stockpyme.app.ui.comun.MostrarMensaje
import com.stockpyme.app.ui.comun.Pantalla
import com.stockpyme.app.ui.comun.repositorio
import kotlinx.coroutines.launch

/**
 * ViewModel genérico para las pantallas de administración: una lista que se carga
 * y operaciones que, si salen bien, cierran el diálogo y recargan la lista.
 */
open class ListaViewModel<T>(private val cargarDatos: suspend () -> Result<List<T>>) : ViewModel() {
    var elementos by mutableStateOf<List<T>>(emptyList())
        private set
    var cargando by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var guardando by mutableStateOf(false)
        private set
    var mensaje by mutableStateOf<String?>(null)

    fun cargar() {
        viewModelScope.launch {
            cargando = true
            cargarDatos()
                .onSuccess {
                    elementos = it
                    error = null
                }
                .onFailure { error = it.message }
            cargando = false
        }
    }

    /** Ejecuta [operacion]; si va bien muestra [exito], recarga y llama a [alTerminar]. */
    fun ejecutar(exito: String, operacion: suspend () -> Result<*>, alTerminar: () -> Unit) {
        guardando = true
        viewModelScope.launch {
            operacion()
                .onSuccess {
                    mensaje = exito
                    alTerminar()
                    cargar()
                }
                .onFailure { mensaje = it.message }
            guardando = false
        }
    }
}

/** Esqueleto común: lista con botón de añadir y mensajes. */
@Composable
private fun <T> PantallaLista(
    titulo: String,
    vm: ListaViewModel<T>,
    textoVacio: String,
    onAtras: () -> Unit,
    onNuevo: () -> Unit,
    clave: (T) -> Any,
    fila: @Composable (T) -> Unit,
) {
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(Unit) { vm.cargar() }
    MostrarMensaje(vm.mensaje, snackbar) { vm.mensaje = null }

    Pantalla(
        titulo = titulo,
        onAtras = onAtras,
        snackbar = snackbar,
        botonFlotante = {
            FloatingActionButton(onClick = onNuevo) { Icon(Icons.Filled.Add, contentDescription = "Añadir") }
        },
    ) { margenes ->
        EstadoLista(
            cargando = vm.cargando,
            error = vm.error,
            vacia = vm.elementos.isEmpty(),
            textoVacio = textoVacio,
            onReintentar = vm::cargar,
            modifier = Modifier.padding(margenes),
        ) {
            LazyColumn(Modifier.padding(margenes).fillMaxSize()) {
                items(vm.elementos, key = clave) { elemento ->
                    fila(elemento)
                    HorizontalDivider()
                }
            }
        }
    }
}

/** Diálogo de formulario con botones Guardar / Cancelar. */
@Composable
private fun DialogoFormulario(
    titulo: String,
    guardando: Boolean,
    onCancelar: () -> Unit,
    onGuardar: () -> Unit,
    botonExtra: (@Composable () -> Unit)? = null,
    contenido: @Composable () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!guardando) onCancelar() },
        title = { Text(titulo) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) { contenido() }
        },
        confirmButton = { TextButton(onClick = onGuardar, enabled = !guardando) { Text("Guardar") } },
        dismissButton = {
            Row {
                botonExtra?.invoke()
                TextButton(onClick = onCancelar, enabled = !guardando) { Text("Cancelar") }
            }
        },
    )
}

@Composable
private fun Interruptor(texto: String, valor: Boolean, onCambio: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(texto, Modifier.weight(1f))
        Switch(checked = valor, onCheckedChange = onCambio)
    }
}

// ---------- CU-06 Categorías ----------

@Composable
fun PantallaCategorias(onAtras: () -> Unit) {
    val repo = repositorio()
    val vm: ListaViewModel<Categoria> = viewModel { ListaViewModel { repo.categorias() } }
    // null = cerrado; id 0 = categoría nueva
    var editando by remember { mutableStateOf<Categoria?>(null) }

    PantallaLista(
        titulo = "Categorías",
        vm = vm,
        textoVacio = "No hay categorías. Pulsa + para crear la primera.",
        onAtras = onAtras,
        onNuevo = { editando = Categoria(0, "") },
        clave = { it.id },
    ) { c ->
        ListItem(modifier = Modifier.clickable { editando = c }, headlineContent = { Text(c.nombre) })
    }

    editando?.let { c ->
        var nombre by remember(c.id) { mutableStateOf(c.nombre) }
        val cerrar = { editando = null }
        DialogoFormulario(
            titulo = if (c.id == 0L) "Nueva categoría" else "Editar categoría",
            guardando = vm.guardando,
            onCancelar = cerrar,
            onGuardar = {
                if (nombre.isBlank()) {
                    vm.mensaje = "El nombre es obligatorio"
                } else if (c.id == 0L) {
                    vm.ejecutar("Categoría creada", { repo.crearCategoria(nombre) }, cerrar)
                } else {
                    vm.ejecutar("Categoría guardada", { repo.renombrarCategoria(c.id, nombre) }, cerrar)
                }
            },
            botonExtra = if (c.id != 0L) {
                {
                    TextButton(onClick = { vm.ejecutar("Categoría borrada", { repo.borrarCategoria(c.id) }, cerrar) }) {
                        Text("Borrar", color = MaterialTheme.colorScheme.error)
                    }
                }
            } else {
                null
            },
        ) {
            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it.take(80) },
                label = { Text("Nombre") },
                singleLine = true,
            )
        }
    }
}

// ---------- CU-12 Proveedores ----------

@Composable
fun PantallaProveedores(onAtras: () -> Unit) {
    val repo = repositorio()
    val vm: ListaViewModel<Proveedor> = viewModel { ListaViewModel { repo.proveedores(incluirInactivos = true) } }
    var editando by remember { mutableStateOf<Proveedor?>(null) }

    PantallaLista(
        titulo = "Proveedores",
        vm = vm,
        textoVacio = "No hay proveedores. Pulsa + para añadir uno.",
        onAtras = onAtras,
        onNuevo = { editando = Proveedor(0, "", null, null, true) },
        clave = { it.id },
    ) { p ->
        ListItem(
            modifier = Modifier.clickable { editando = p },
            headlineContent = { Text(if (p.activo) p.nombre else "${p.nombre} (desactivado)") },
            supportingContent = {
                Text(listOfNotNull(p.telefono, p.email).joinToString(" · ").ifBlank { "Sin datos de contacto" })
            },
        )
    }

    editando?.let { p ->
        var nombre by remember(p.id) { mutableStateOf(p.nombre) }
        var telefono by remember(p.id) { mutableStateOf(p.telefono ?: "") }
        var email by remember(p.id) { mutableStateOf(p.email ?: "") }
        var activo by remember(p.id) { mutableStateOf(p.activo) }
        val cerrar = { editando = null }
        DialogoFormulario(
            titulo = if (p.id == 0L) "Nuevo proveedor" else "Editar proveedor",
            guardando = vm.guardando,
            onCancelar = cerrar,
            onGuardar = {
                if (nombre.isBlank()) {
                    vm.mensaje = "El nombre es obligatorio"
                } else {
                    val datos = ProveedorRequest(
                        nombre.trim(),
                        telefono.trim().ifBlank { null },
                        email.trim().ifBlank { null },
                        if (p.id == 0L) null else activo,
                    )
                    if (p.id == 0L) {
                        vm.ejecutar("Proveedor creado", { repo.crearProveedor(datos) }, cerrar)
                    } else {
                        vm.ejecutar("Proveedor guardado", { repo.editarProveedor(p.id, datos) }, cerrar)
                    }
                }
            },
        ) {
            OutlinedTextField(nombre, { nombre = it.take(120) }, label = { Text("Nombre *") }, singleLine = true)
            OutlinedTextField(
                telefono, { telefono = it.take(30) }, label = { Text("Teléfono") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            )
            OutlinedTextField(
                email, { email = it.take(150) }, label = { Text("Email") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            )
            if (p.id != 0L) Interruptor("Activo", activo) { activo = it }
        }
    }
}

// ---------- CU-13 Usuarios ----------

@Composable
fun PantallaUsuarios(actual: Usuario, onAtras: () -> Unit) {
    val repo = repositorio()
    val vm: ListaViewModel<Usuario> = viewModel { ListaViewModel { repo.usuarios() } }
    var editando by remember { mutableStateOf<Usuario?>(null) }

    PantallaLista(
        titulo = "Usuarios",
        vm = vm,
        textoVacio = "No hay usuarios",
        onAtras = onAtras,
        onNuevo = { editando = Usuario(0, "", "", Usuario.ROL_EMPLEADO, true) },
        clave = { it.id },
    ) { u ->
        ListItem(
            modifier = Modifier.clickable { editando = u },
            headlineContent = {
                Text(u.nombre + (if (u.id == actual.id) " (tú)" else "") + (if (!u.activo) " (desactivado)" else ""))
            },
            supportingContent = { Text("${u.email} · ${if (u.esPropietario) "Propietario" else "Empleado"}") },
        )
    }

    editando?.let { u ->
        val nuevo = u.id == 0L
        var nombre by remember(u.id) { mutableStateOf(u.nombre) }
        var email by remember(u.id) { mutableStateOf(u.email) }
        var rol by remember(u.id) { mutableStateOf(u.rol) }
        var password by remember(u.id) { mutableStateOf("") }
        var activo by remember(u.id) { mutableStateOf(u.activo) }
        val cerrar = { editando = null }
        DialogoFormulario(
            titulo = if (nuevo) "Nuevo usuario" else "Editar usuario",
            guardando = vm.guardando,
            onCancelar = cerrar,
            onGuardar = {
                when {
                    nombre.isBlank() || email.isBlank() -> vm.mensaje = "Nombre y email son obligatorios"
                    nuevo && password.length < 8 -> vm.mensaje = "La contraseña debe tener al menos 8 caracteres"
                    !nuevo && password.isNotEmpty() && password.length < 8 ->
                        vm.mensaje = "La contraseña debe tener al menos 8 caracteres"
                    else -> {
                        val datos = UsuarioRequest(
                            nombre.trim(),
                            email.trim(),
                            rol,
                            password.ifEmpty { null },
                            if (nuevo) null else activo,
                        )
                        if (nuevo) {
                            vm.ejecutar("Usuario creado", { repo.crearUsuario(datos) }, cerrar)
                        } else {
                            vm.ejecutar("Usuario guardado", { repo.editarUsuario(u.id, datos) }, cerrar)
                        }
                    }
                }
            },
        ) {
            OutlinedTextField(nombre, { nombre = it.take(100) }, label = { Text("Nombre *") }, singleLine = true)
            OutlinedTextField(
                email, { email = it.trim().take(150) }, label = { Text("Email *") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            )
            OutlinedTextField(
                password, { password = it.take(72) },
                label = { Text(if (nuevo) "Contraseña * (mín. 8)" else "Nueva contraseña (opcional)") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                FilterChip(
                    selected = rol == Usuario.ROL_EMPLEADO,
                    onClick = { rol = Usuario.ROL_EMPLEADO },
                    label = { Text("Empleado") },
                )
                FilterChip(
                    selected = rol == Usuario.ROL_PROPIETARIO,
                    onClick = { rol = Usuario.ROL_PROPIETARIO },
                    label = { Text("Propietario") },
                )
            }
            if (!nuevo) Interruptor("Cuenta activa", activo) { activo = it }
        }
    }
}
