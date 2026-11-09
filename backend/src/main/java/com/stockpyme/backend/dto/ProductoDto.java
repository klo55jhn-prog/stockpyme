package com.stockpyme.backend.dto;

import com.stockpyme.backend.modelo.Producto;
import java.math.BigDecimal;

public record ProductoDto(
        Long id,
        String nombre,
        String codigoBarras,
        BigDecimal precioCompra,
        BigDecimal precioVenta,
        int stockActual,
        int stockMinimo,
        boolean bajoMinimo,
        boolean activo,
        Long categoriaId,
        String categoriaNombre,
        Long proveedorId,
        String proveedorNombre) {

    public static ProductoDto de(Producto p) {
        return new ProductoDto(
                p.getId(),
                p.getNombre(),
                p.getCodigoBarras(),
                p.getPrecioCompra(),
                p.getPrecioVenta(),
                p.getStockActual(),
                p.getStockMinimo(),
                p.isBajoMinimo(),
                p.isActivo(),
                p.getCategoria().getId(),
                p.getCategoria().getNombre(),
                p.getProveedor() == null ? null : p.getProveedor().getId(),
                p.getProveedor() == null ? null : p.getProveedor().getNombre());
    }
}
