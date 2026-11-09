package com.stockpyme.backend.dto;

import com.stockpyme.backend.modelo.TipoMovimiento;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Petición para cambiar el stock de un producto.
 * <ul>
 *   <li>ENTRADA y SALIDA: {@code cantidad} es el número de unidades (siempre positivo).
 *       El servidor le pone el signo.</li>
 *   <li>AJUSTE: {@code stockNuevo} es el stock real contado. El servidor calcula la diferencia
 *       con el stock bloqueado, así no hay errores si otro usuario cambia el stock a la vez.</li>
 * </ul>
 */
public record MovimientoRequest(
        @NotNull(message = "El tipo de movimiento es obligatorio")
        TipoMovimiento tipo,

        Integer cantidad,

        Integer stockNuevo,

        @Size(max = 255, message = "El motivo admite como máximo 255 caracteres")
        String motivo) {
}
