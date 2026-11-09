package com.stockpyme.app.ui.login

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stockpyme.app.datos.Repositorio
import com.stockpyme.app.ui.comun.Logo
import com.stockpyme.app.ui.comun.coloresEstadoMensaje
import com.stockpyme.app.ui.comun.repositorio
import com.stockpyme.app.ui.comun.sesion
import com.stockpyme.app.ui.tema.MarcaClara
import com.stockpyme.app.ui.tema.MarcaOscura
import kotlinx.coroutines.launch

/** CU-01 Iniciar sesión. */
class LoginViewModel(private val repo: Repositorio, urlGuardada: String) : ViewModel() {

    var servidor by mutableStateOf(urlGuardada)
    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var ocupado by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var aviso by mutableStateOf<String?>(null)
        private set

    fun probarConexion(guardarUrl: (String) -> Unit) {
        guardarUrl(servidor)
        ocupado = true
        error = null
        aviso = null
        viewModelScope.launch {
            repo.probarConexion()
                .onSuccess { aviso = "Conexión correcta con el servidor" }
                .onFailure { error = it.message }
            ocupado = false
        }
    }

    fun entrar(guardarUrl: (String) -> Unit) {
        if (email.isBlank() || password.isBlank()) {
            error = "Escribe tu email y tu contraseña"
            return
        }
        guardarUrl(servidor)
        ocupado = true
        error = null
        aviso = null
        viewModelScope.launch {
            // Si va bien, la sesión se guarda y MainActivity cambia sola a la app.
            repo.login(email, password).onFailure { error = it.message }
            ocupado = false
        }
    }

    fun rellenarDemo(correo: String, clave: String) {
        email = correo
        password = clave
        error = null
    }
}

@Composable
fun PantallaLogin() {
    val repo = repositorio()
    val datosSesion = sesion()
    val vm: LoginViewModel = viewModel { LoginViewModel(repo, datosSesion.urlServidor) }
    var verPassword by remember { mutableStateOf(false) }
    var verServidor by remember { mutableStateOf(false) }
    val guardarUrl: (String) -> Unit = { datosSesion.urlServidor = it }

    // La cabecera es oscura: iconos de la barra de estado en blanco mientras se ve el login.
    val vista = LocalView.current
    DisposableEffect(Unit) {
        val ventana = (vista.context as? Activity)?.window
        val control = ventana?.let { WindowCompat.getInsetsController(it, vista) }
        val antes = control?.isAppearanceLightStatusBars
        control?.isAppearanceLightStatusBars = false
        onDispose { if (antes != null) control.isAppearanceLightStatusBars = antes }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // Fondo de la cabecera con el degradado de la marca
        Box(
            Modifier
                .fillMaxWidth()
                .height(340.dp)
                .background(Brush.linearGradient(listOf(MarcaClara, MarcaOscura))),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(36.dp))
            Logo(84.dp)
            Spacer(Modifier.height(16.dp))
            Text(
                "StockPyme",
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White,
            )
            Text(
                "El inventario de tu comercio, siempre al día",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f),
            )
            Spacer(Modifier.height(32.dp))

            // Tarjeta del formulario
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Inicia sesión", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "Entra con la cuenta que te ha dado el propietario del comercio.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = vm.email,
                        onValueChange = { vm.email = it },
                        label = { Text("Email") },
                        leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null) },
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = vm.password,
                        onValueChange = { vm.password = it },
                        label = { Text("Contraseña") },
                        leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                        visualTransformation = if (verPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { vm.entrar(guardarUrl) }),
                        trailingIcon = {
                            IconButton(onClick = { verPassword = !verPassword }) {
                                Icon(
                                    if (verPassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                    contentDescription = if (verPassword) "Ocultar contraseña" else "Mostrar contraseña",
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    vm.error?.let { Aviso(it, esError = true) }
                    vm.aviso?.let { Aviso(it, esError = false) }

                    Button(
                        onClick = { vm.entrar(guardarUrl) },
                        enabled = !vm.ocupado,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                    ) {
                        if (vm.ocupado) {
                            CircularProgressIndicator(
                                Modifier.size(22.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        } else {
                            Text("Entrar", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }

            // Cuentas de demostración: un toque rellena el formulario
            Spacer(Modifier.height(20.dp))
            Text(
                "Probar con una cuenta de demostración",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(
                    onClick = { vm.rellenarDemo("admin@stockpyme.local", "admin1234") },
                    label = { Text("Propietario") },
                    leadingIcon = { Icon(Icons.Outlined.Storefront, contentDescription = null, Modifier.size(18.dp)) },
                )
                AssistChip(
                    onClick = { vm.rellenarDemo("empleado@stockpyme.local", "empleado1234") },
                    label = { Text("Empleado") },
                    leadingIcon = { Icon(Icons.Outlined.Badge, contentDescription = null, Modifier.size(18.dp)) },
                )
            }

            // Servidor: oculto por defecto para no liar a quien solo quiere entrar
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = { verServidor = !verServidor }) {
                Icon(Icons.Outlined.Dns, contentDescription = null, Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text(if (verServidor) "Ocultar servidor" else "Servidor: ${vm.servidor}")
            }
            AnimatedVisibility(verServidor) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = vm.servidor,
                        onValueChange = { vm.servidor = it },
                        label = { Text("Dirección del servidor") },
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                        supportingText = {
                            Text("Emulador: http://10.0.2.2:8080 · Móvil: http://IP-de-tu-PC:8080")
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedButton(
                        onClick = { vm.probarConexion(guardarUrl) },
                        enabled = !vm.ocupado,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Probar conexión") }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Aviso(texto: String, esError: Boolean) {
    val (color, fondo) = coloresEstadoMensaje(esError)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(fondo, MaterialTheme.shapes.small)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (esError) Icons.Filled.ErrorOutline else Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp),
        )
        Text(texto, color = color, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}
