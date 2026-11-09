package com.stockpyme.backend.dto;

import com.stockpyme.backend.modelo.Rol;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Crear o editar un usuario. Al editar, una contraseña vacía o null deja la que había.
 */
public record UsuarioRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre admite como máximo 100 caracteres")
        String nombre,

        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email no es válido")
        @Size(max = 150, message = "El email admite como máximo 150 caracteres")
        String email,

        @NotNull(message = "El rol es obligatorio")
        Rol rol,

        @Size(max = 72, message = "La contraseña admite como máximo 72 caracteres")
        String password,

        /** Solo se usa al editar. Null = no cambia. */
        Boolean activo) {
}
