package com.stockpyme.app.datos

import android.content.Context
import androidx.core.content.edit
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Guarda en el móvil la dirección del servidor, el token JWT y el usuario conectado.
 * La interfaz observa [usuario]: cuando pasa a null (cerrar sesión o token caducado),
 * la app vuelve sola a la pantalla de login.
 */
class Sesion(context: Context) {

    private val prefs = context.getSharedPreferences("stockpyme", Context.MODE_PRIVATE)
    private val gson = Gson()

    var urlServidor: String
        get() = prefs.getString(CLAVE_URL, null) ?: URL_POR_DEFECTO
        set(valor) = prefs.edit { putString(CLAVE_URL, normalizarUrl(valor)) }

    val token: String? get() = prefs.getString(CLAVE_TOKEN, null)

    private val _usuario = MutableStateFlow(leerUsuario())
    val usuario: StateFlow<Usuario?> = _usuario.asStateFlow()

    fun iniciar(token: String, usuario: Usuario) {
        prefs.edit {
            putString(CLAVE_TOKEN, token)
            putString(CLAVE_USUARIO, gson.toJson(usuario))
        }
        _usuario.value = usuario
    }

    /** Actualiza los datos del usuario (por ejemplo, si el propietario le cambió el rol). */
    fun actualizarUsuario(usuario: Usuario) {
        if (token == null) return
        prefs.edit { putString(CLAVE_USUARIO, gson.toJson(usuario)) }
        _usuario.value = usuario
    }

    fun cerrar() {
        prefs.edit {
            remove(CLAVE_TOKEN)
            remove(CLAVE_USUARIO)
        }
        _usuario.value = null
    }

    private fun leerUsuario(): Usuario? {
        if (token == null) return null
        return try {
            gson.fromJson(prefs.getString(CLAVE_USUARIO, null), Usuario::class.java)
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        /** 10.0.2.2 es la dirección del PC visto desde el emulador de Android Studio. */
        const val URL_POR_DEFECTO = "http://10.0.2.2:8080/"

        private const val CLAVE_URL = "url"
        private const val CLAVE_TOKEN = "token"
        private const val CLAVE_USUARIO = "usuario"

        /** Acepta "192.168.1.10:8080" y lo convierte en "http://192.168.1.10:8080/". */
        fun normalizarUrl(texto: String): String {
            var url = texto.trim()
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "http://$url"
            }
            if (!url.endsWith("/")) {
                url = "$url/"
            }
            return url
        }
    }
}
