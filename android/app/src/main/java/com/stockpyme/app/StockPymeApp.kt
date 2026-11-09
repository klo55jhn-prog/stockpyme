package com.stockpyme.app

import android.app.Application
import com.stockpyme.app.datos.Contenedor

class StockPymeApp : Application() {

    lateinit var contenedor: Contenedor
        private set

    override fun onCreate() {
        super.onCreate()
        contenedor = Contenedor(this)
    }
}
