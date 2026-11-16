package com.stockpyme.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Actividad del comercio en los últimos días (V2, para el panel del propietario).
 *
 * <p>"Venta" es una salida con motivo vacío o "Venta". Las mermas y roturas son salidas
 * de stock, pero no se cuentan como ingresos.
 */
public record ActividadDto(
        List<Dia> dias,
        BigDecimal ventasHoy,
        BigDecimal ventasPeriodo,
        long unidadesVendidasPeriodo,
        long movimientosHoy) {

    /** Un día, en la zona horaria del comercio. {@code ventas} está en euros. */
    public record Dia(LocalDate fecha, long entradas, long salidas, BigDecimal ventas) {
    }
}
