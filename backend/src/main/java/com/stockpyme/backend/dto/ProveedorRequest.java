package com.stockpyme.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProveedorRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 120, message = "El nombre admite como máximo 120 caracteres")
        String nombre,

        @Size(max = 30, message = "El teléfono admite como máximo 30 caracteres")
        String telefono,

        @Email(message = "El email no es válido")
        @Size(max = 150, message = "El email admite como máximo 150 caracteres")
        String email,

        /** Solo se usa al editar. Null = no cambia. */
        Boolean activo) {
}
