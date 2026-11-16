package com.stockpyme.app.datos

import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/** Contrato de la API REST. Retrofit genera el código de cada llamada HTTP. */
interface ApiService {

    @GET("api/salud")
    suspend fun salud(): Map<String, String>

    @POST("api/auth/login")
    suspend fun login(@Body datos: LoginRequest): LoginResponse

    @GET("api/auth/yo")
    suspend fun yo(): Usuario

    // Productos
    @GET("api/productos")
    suspend fun buscarProductos(
        @Query("buscar") texto: String,
        @Query("incluirInactivos") incluirInactivos: Boolean,
    ): List<Producto>

    @GET("api/productos/bajo-minimo")
    suspend fun bajoMinimo(): List<Producto>

    @GET("api/productos/{id}")
    suspend fun producto(@Path("id") id: Long): Producto

    @GET("api/productos/codigo/{codigo}")
    suspend fun productoPorCodigo(@Path("codigo") codigo: String): Producto

    @POST("api/productos")
    suspend fun crearProducto(@Body datos: ProductoRequest): Producto

    @PUT("api/productos/{id}")
    suspend fun editarProducto(@Path("id") id: Long, @Body datos: ProductoRequest): Producto

    // Movimientos
    @GET("api/productos/{id}/movimientos")
    suspend fun historial(@Path("id") productoId: Long): List<Movimiento>

    @POST("api/productos/{id}/movimientos")
    suspend fun registrarMovimiento(@Path("id") productoId: Long, @Body datos: MovimientoRequest): Producto

    @GET("api/movimientos/recientes")
    suspend fun recientes(@Query("limite") limite: Int): List<Movimiento>

    // Categorías
    @GET("api/categorias")
    suspend fun categorias(): List<Categoria>

    @POST("api/categorias")
    suspend fun crearCategoria(@Body datos: CategoriaRequest): Categoria

    @PUT("api/categorias/{id}")
    suspend fun renombrarCategoria(@Path("id") id: Long, @Body datos: CategoriaRequest): Categoria

    @DELETE("api/categorias/{id}")
    suspend fun borrarCategoria(@Path("id") id: Long)

    // Proveedores
    @GET("api/proveedores")
    suspend fun proveedores(@Query("incluirInactivos") incluirInactivos: Boolean): List<Proveedor>

    @POST("api/proveedores")
    suspend fun crearProveedor(@Body datos: ProveedorRequest): Proveedor

    @PUT("api/proveedores/{id}")
    suspend fun editarProveedor(@Path("id") id: Long, @Body datos: ProveedorRequest): Proveedor

    // Usuarios
    @GET("api/usuarios")
    suspend fun usuarios(): List<Usuario>

    @POST("api/usuarios")
    suspend fun crearUsuario(@Body datos: UsuarioRequest): Usuario

    @PUT("api/usuarios/{id}")
    suspend fun editarUsuario(@Path("id") id: Long, @Body datos: UsuarioRequest): Usuario

    // Informes
    @GET("api/informes/resumen")
    suspend fun resumen(): Resumen

    // V2
    @GET("api/informes/actividad")
    suspend fun actividad(@Query("dias") dias: Int): Actividad

    @GET("api/informes/inventario.csv")
    suspend fun inventarioCsv(): ResponseBody

    @GET("api/informes/movimientos.csv")
    suspend fun movimientosCsv(@Query("dias") dias: Int): ResponseBody

    @GET("api/productos/reposicion")
    suspend fun reposicion(): List<LineaReposicion>

    @POST("api/ventas")
    suspend fun registrarVenta(@Body datos: VentaRequest): VentaRespuesta

    @PUT("api/auth/password")
    suspend fun cambiarPassword(@Body datos: CambioPasswordRequest): Map<String, String>
}
