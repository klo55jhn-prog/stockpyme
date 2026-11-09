package com.stockpyme.backend.dto;

import java.math.BigDecimal;
import java.util.List;

/** Informe para el propietario (RF-13). */
public record ResumenDto(
        int productosActivos,
        long unidadesEnStock,
        BigDecimal valorCompra,
        BigDecimal valorVenta,
        int productosBajoMinimo,
        List<ProductoMovido> masMovidos30Dias) {

    public record ProductoMovido(Long productoId, String nombre, long unidades) {
    }
}
