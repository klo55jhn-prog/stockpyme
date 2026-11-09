package com.stockpyme.app.datos

import com.google.gson.Gson
import java.io.IOException
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException

/**
 * Punto único de acceso a los datos para los ViewModel.
 * Convierte cualquier fallo (sin red, error HTTP, sesión caducada) en un
 * Result con un mensaje que se puede enseñar directamente al usuario.
 */
class Repositorio(private val contenedor: Contenedor) {

    private val gson = Gson()
    private val sesion get() = contenedor.sesion

    private suspend fun <T> llamar(bloque: suspend ApiService.() -> T): Result<T> =
        try {
            Result.success(contenedor.api.bloque())
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpException) {
            if (e.code() == 401 && sesion.token != null) {
                sesion.cerrar() // token caducado o usuario desactivado: vuelve al login
            }
            Result.failure(Exception(mensajeDe(e)))
        } catch (e: IOException) {
            Result.failure(Exception("No se puede conectar con el servidor. Revisa la dirección, que el móvil esté en la misma red y que la app tenga permiso de red local."))
        } catch (e: IllegalArgumentException) {
            Result.failure(Exception("La dirección del servidor no es válida"))
        } catch (e: Exception) {
            Result.failure(Exception("Error inesperado: ${e.message}"))
        }

    private fun mensajeDe(e: HttpException): String {
        val cuerpo = try {
            e.response()?.errorBody()?.string()
        } catch (ex: IOException) {
            null
        }
        val mensaje = try {
            gson.fromJson(cuerpo, ErrorApi::class.java)?.mensaje
        } catch (ex: Exception) {
            null
        }
        return mensaje ?: "Error del servidor (${e.code()})"
    }

    // Sesión
    suspend fun probarConexion() = llamar { salud() }

    suspend fun login(email: String, password: String): Result<Usuario> =
        llamar { login(LoginRequest(email.trim(), password)) }.map { respuesta ->
            sesion.iniciar(respuesta.token, respuesta.usuario)
            respuesta.usuario
        }

    suspend fun refrescarUsuario() = llamar { yo() }.onSuccess { sesion.actualizarUsuario(it) }

    fun cerrarSesion() = sesion.cerrar()

    // Productos y movimientos
    suspend fun buscarProductos(texto: String, incluirInactivos: Boolean) =
        llamar { buscarProductos(texto.trim(), incluirInactivos) }

    suspend fun bajoMinimo() = llamar { bajoMinimo() }
    suspend fun producto(id: Long) = llamar { producto(id) }
    suspend fun productoPorCodigo(codigo: String) = llamar { productoPorCodigo(codigo) }
    suspend fun crearProducto(datos: ProductoRequest) = llamar { crearProducto(datos) }
    suspend fun editarProducto(id: Long, datos: ProductoRequest) = llamar { editarProducto(id, datos) }
    suspend fun historial(productoId: Long) = llamar { historial(productoId) }
    suspend fun registrarMovimiento(productoId: Long, datos: MovimientoRequest) =
        llamar { registrarMovimiento(productoId, datos) }

    suspend fun recientes() = llamar { recientes(100) }

    // Administración
    suspend fun categorias() = llamar { categorias() }
    suspend fun crearCategoria(nombre: String) = llamar { crearCategoria(CategoriaRequest(nombre.trim())) }
    suspend fun renombrarCategoria(id: Long, nombre: String) =
        llamar { renombrarCategoria(id, CategoriaRequest(nombre.trim())) }

    suspend fun borrarCategoria(id: Long) = llamar { borrarCategoria(id) }

    suspend fun proveedores(incluirInactivos: Boolean) = llamar { proveedores(incluirInactivos) }
    suspend fun crearProveedor(datos: ProveedorRequest) = llamar { crearProveedor(datos) }
    suspend fun editarProveedor(id: Long, datos: ProveedorRequest) = llamar { editarProveedor(id, datos) }

    suspend fun usuarios() = llamar { usuarios() }
    suspend fun crearUsuario(datos: UsuarioRequest) = llamar { crearUsuario(datos) }
    suspend fun editarUsuario(id: Long, datos: UsuarioRequest) = llamar { editarUsuario(id, datos) }

    suspend fun resumen() = llamar { resumen() }
}
