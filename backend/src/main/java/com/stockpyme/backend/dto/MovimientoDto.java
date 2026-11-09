package com.stockpyme.backend.dto;

import com.stockpyme.backend.modelo.Movimiento;
import com.stockpyme.backend.modelo.TipoMovimiento;
import java.time.Instant;

public record MovimientoDto(
        Long id,
        TipoMovimiento tipo,
        int cantidad,
        String motivo,
        Instant fecha,
        Long productoId,
        String productoNombre,
        String usuarioNombre) {

    public static MovimientoDto de(Movimiento m) {
        return new MovimientoDto(
                m.getId(),
                m.getTipo(),
                m.getCantidad(),
                m.getMotivo(),
                m.getFecha(),
                m.getProducto().getId(),
                m.getProducto().getNombre(),
                m.getUsuario().getNombre());
    }
}
