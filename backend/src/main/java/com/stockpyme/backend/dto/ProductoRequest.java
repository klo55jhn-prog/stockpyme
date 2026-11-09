package com.stockpyme.backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Datos para crear o editar un producto. No incluye el stock: el stock solo
 * cambia con movimientos (análisis, apartado 7).
 */
public record ProductoRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 150, message = "El nombre admite como máximo 150 caracteres")
        String nombre,

        @Size(max = 50, message = "El código de barras admite como máximo 50 caracteres")
        String codigoBarras,

        @NotNull(message = "El precio de compra es obligatorio")
        @DecimalMin(value = "0.00", message = "El precio de compra no puede ser negativo")
        @Digits(integer = 8, fraction = 2, message = "El precio de compra admite 2 decimales")
        BigDecimal precioCompra,

        @NotNull(message = "El precio de venta es obligatorio")
        @DecimalMin(value = "0.00", message = "El precio de venta no puede ser negativo")
        @Digits(integer = 8, fraction = 2, message = "El precio de venta admite 2 decimales")
        BigDecimal precioVenta,

        @NotNull(message = "El stock mínimo es obligatorio")
        @Min(value = 0, message = "El stock mínimo no puede ser negativo")
        Integer stockMinimo,

        @NotNull(message = "La categoría es obligatoria")
        Long categoriaId,

        Long proveedorId,

        /** Solo se usa al editar. Null = no cambia. */
        Boolean activo) {
}
