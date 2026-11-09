package com.stockpyme.app.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stockpyme.app.datos.Repositorio
import com.stockpyme.app.ui.comun.repositorio
import com.stockpyme.app.ui.comun.sesion
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
}

@Composable
fun PantallaLogin() {
    val repo = repositorio()
    val datosSesion = sesion()
    val vm: LoginViewModel = viewModel { LoginViewModel(repo, datosSesion.urlServidor) }
    var verPassword by remember { mutableStateOf(false) }
    val guardarUrl: (String) -> Unit = { datosSesion.urlServidor = it }

    Surface(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(Modifier.height(24.dp))
            Icon(
                Icons.Filled.Inventory2,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(72.dp),
            )
            Text("StockPyme", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Text(
                "Inventario para pequeños comercios",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = vm.email,
                onValueChange = { vm.email = it },
                label = { Text("Email") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = vm.password,
                onValueChange = { vm.password = it },
                label = { Text("Contraseña") },
                singleLine = true,
                visualTransformation = if (verPassword) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                trailingIcon = {
                    IconButton(onClick = { verPassword = !verPassword }) {
                        Icon(
                            if (verPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (verPassword) "Ocultar contraseña" else "Mostrar contraseña",
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )

            vm.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            vm.aviso?.let { Text(it, color = MaterialTheme.colorScheme.primary) }

            Button(
                onClick = { vm.entrar(guardarUrl) },
                enabled = !vm.ocupado,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                if (vm.ocupado) {
                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Text("Entrar")
                }
            }

            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = vm.servidor,
                onValueChange = { vm.servidor = it },
                label = { Text("Dirección del servidor") },
                singleLine = true,
                supportingText = {
                    Text("Emulador: http://10.0.2.2:8080 · Móvil: http://IP-de-tu-PC:8080")
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth(),
            )
            TextButton(onClick = { vm.probarConexion(guardarUrl) }, enabled = !vm.ocupado) {
                Text("Probar conexión")
            }

            Text(
                "Usuarios de prueba:\nadmin@stockpyme.local / admin1234\nempleado@stockpyme.local / empleado1234",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
