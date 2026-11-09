package com.stockpyme.app

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import com.stockpyme.app.ui.AppPrincipal
import com.stockpyme.app.ui.login.PantallaLogin
import com.stockpyme.app.ui.tema.TemaStockPyme

class MainActivity : ComponentActivity() {

    private val pedirPermisoRedLocal =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* si se deniega, el login avisa */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pedirRedLocalSiHaceFalta()
        val contenedor = (application as StockPymeApp).contenedor
        setContent {
            TemaStockPyme {
                // Sin usuario se muestra el login; con usuario, la app. Cerrar sesión
                // o un token caducado ponen el usuario a null y se vuelve al login solo.
                val usuario by contenedor.sesion.usuario.collectAsState()
                val actual = usuario
                if (actual == null) {
                    PantallaLogin()
                } else {
                    AppPrincipal(actual)
                }
            }
        }
    }

    /** En Android 17+ hace falta permiso para conectar con el servidor de la red local (tu PC). */
    private fun pedirRedLocalSiHaceFalta() {
        if (Build.VERSION.SDK_INT < ANDROID_17) return
        val permiso = PERMISO_RED_LOCAL
        if (ContextCompat.checkSelfPermission(this, permiso) != PackageManager.PERMISSION_GRANTED) {
            pedirPermisoRedLocal.launch(permiso)
        }
    }

    private companion object {
        const val ANDROID_17 = 37
        const val PERMISO_RED_LOCAL = "android.permission.ACCESS_LOCAL_NETWORK"
    }
}
