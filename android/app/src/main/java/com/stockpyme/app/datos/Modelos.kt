package com.stockpyme.app.datos

import java.math.BigDecimal

// Objetos que viajan entre la app y la API (los DTO del backend, en Kotlin).
// Los campos que la API puede devolver vacíos son nullable (String?).

data class Usuario(
    val id: Long,
    val nombre: String,
    val email: String,
    val rol: String,
    val activo: Boolean,
) {
    val esPropietario: Boolean get() = rol == ROL_PROPIETARIO

    companion object {
        const val ROL_PROPIETARIO = "PROPIETARIO"
        const val ROL_EMPLEADO = "EMPLEADO"
    }
}

data class LoginRequest(val email: String, val password: String)

data class LoginResponse(val token: String, val usuario: Usuario)

data class Producto(
    val id: Long,
    val nombre: String,
    val codigoBarras: String?,
    val precioCompra: BigDecimal,
    val precioVenta: BigDecimal,
    val stockActual: Int,
    val stockMinimo: Int,
    val bajoMinimo: Boolean,
    val activo: Boolean,
    val categoriaId: Long,
    val categoriaNombre: String,
    val proveedorId: Long?,
    val proveedorNombre: String?,
)

data class ProductoRequest(
    val nombre: String,
    val codigoBarras: String?,
    val precioCompra: BigDecimal,
    val precioVenta: BigDecimal,
    val stockMinimo: Int,
    val categoriaId: Long,
    val proveedorId: Long?,
    val activo: Boolean?,
)

object TipoMovimiento {
    const val ENTRADA = "ENTRADA"
    const val SALIDA = "SALIDA"
    const val AJUSTE = "AJUSTE"
}

data class Movimiento(
    val id: Long,
    val tipo: String,
    val cantidad: Int,
    val motivo: String?,
    val fecha: String,
    val productoId: Long,
    val productoNombre: String,
    val usuarioNombre: String,
)

/** ENTRADA/SALIDA usan [cantidad]; AJUSTE usa [stockNuevo] (el stock real contado). */
data class MovimientoRequest(
    val tipo: String,
    val cantidad: Int? = null,
    val stockNuevo: Int? = null,
    val motivo: String? = null,
)

data class Categoria(val id: Long, val nombre: String)

data class CategoriaRequest(val nombre: String)

data class Proveedor(
    val id: Long,
    val nombre: String,
    val telefono: String?,
    val email: String?,
    val activo: Boolean,
)

data class ProveedorRequest(
    val nombre: String,
    val telefono: String?,
    val email: String?,
    val activo: Boolean?,
)

data class UsuarioRequest(
    val nombre: String,
    val email: String,
    val rol: String,
    val password: String?,
    val activo: Boolean?,
)

data class ProductoMovido(val productoId: Long, val nombre: String, val unidades: Long)

data class Resumen(
    val productosActivos: Int,
    val unidadesEnStock: Long,
    val valorCompra: BigDecimal,
    val valorVenta: BigDecimal,
    val productosBajoMinimo: Int,
    val masMovidos30Dias: List<ProductoMovido>,
)

/** Formato de los errores del backend: {"estado": 409, "mensaje": "..."} */
data class ErrorApi(val estado: Int, val mensaje: String?)
