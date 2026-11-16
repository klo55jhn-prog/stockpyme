package com.stockpyme.app

import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import com.stockpyme.app.ui.AppPrincipal
import com.stockpyme.app.ui.login.PantallaLogin
import com.stockpyme.app.ui.tema.LocalOscuro
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
            val modo by contenedor.sesion.tema.collectAsState()
            TemaStockPyme(modo) {
                // Los iconos de la barra de estado y de navegación siguen el tema elegido en la app,
                // que puede ser distinto del del sistema.
                val oscuro = LocalOscuro.current
                DisposableEffect(oscuro) {
                    enableEdgeToEdge(
                        statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { oscuro },
                        navigationBarStyle = SystemBarStyle.auto(
                            Color.argb(0xe6, 0xff, 0xff, 0xff),
                            Color.argb(0x80, 0x1b, 0x1b, 0x1b),
                        ) { oscuro },
                    )
                    onDispose { }
                }

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
