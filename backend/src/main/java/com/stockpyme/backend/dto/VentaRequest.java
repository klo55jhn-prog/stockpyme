package com.stockpyme.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** V2. Una venta completa: el carrito de la app, con varios productos. */
public record VentaRequest(
        @NotEmpty(message = "La venta no tiene ningún artículo")
        @Size(max = 100, message = "Una venta admite como máximo 100 líneas")
        List<@Valid Linea> lineas) {

    public record Linea(
            @NotNull(message = "Falta el producto") Long productoId,
            @NotNull(message = "Falta la cantidad")
            @Min(value = 1, message = "La cantidad debe ser mayor que 0")
            @Max(value = 100000, message = "La cantidad es demasiado grande") Integer cantidad) {
    }
}
