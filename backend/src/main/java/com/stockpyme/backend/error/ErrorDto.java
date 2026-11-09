package com.stockpyme.backend.error;

/** Formato único de los errores de la API: {"estado": 409, "mensaje": "..."} */
public record ErrorDto(int estado, String mensaje) {
}
