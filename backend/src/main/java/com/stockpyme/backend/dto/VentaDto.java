package com.stockpyme.backend.dto;

import java.math.BigDecimal;
import java.util.List;

/** Resultado de una venta: unidades, importe total y cada producto con su stock ya actualizado. */
public record VentaDto(int unidades, BigDecimal total, List<ProductoDto> productos) {
}
