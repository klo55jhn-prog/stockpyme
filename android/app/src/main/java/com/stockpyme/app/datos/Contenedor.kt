package com.stockpyme.app.datos

import android.content.Context
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Crea y guarda los objetos compartidos de la app (inyección de dependencias manual).
 * Si cambia la dirección del servidor, el cliente Retrofit se vuelve a crear.
 */
class Contenedor(context: Context) {

    val sesion = Sesion(context)
    val repositorio = Repositorio(this)

    private var urlActual: String? = null
    private var apiActual: ApiService? = null

    val api: ApiService
        @Synchronized get() {
            val url = sesion.urlServidor
            val existente = apiActual
            if (existente != null && url == urlActual) return existente
            return crearApi(url).also {
                apiActual = it
                urlActual = url
            }
        }

    private fun crearApi(url: String): ApiService {
        val cliente = OkHttpClient.Builder()
            // Añade el token JWT a todas las peticiones
            .addInterceptor { cadena ->
                val token = sesion.token
                val peticion = if (token != null) {
                    cadena.request().newBuilder().header("Authorization", "Bearer $token").build()
                } else {
                    cadena.request()
                }
                cadena.proceed(peticion)
            }
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
        return Retrofit.Builder()
            .baseUrl(url)
            .client(cliente)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
