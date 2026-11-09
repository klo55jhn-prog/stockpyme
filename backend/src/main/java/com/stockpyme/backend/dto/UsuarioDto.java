package com.stockpyme.backend.dto;

import com.stockpyme.backend.modelo.Rol;
import com.stockpyme.backend.modelo.Usuario;

/** Lo que la API devuelve de un usuario. Nunca incluye el hash de la contraseña. */
public record UsuarioDto(Long id, String nombre, String email, Rol rol, boolean activo) {

    public static UsuarioDto de(Usuario u) {
        return new UsuarioDto(u.getId(), u.getNombre(), u.getEmail(), u.getRol(), u.isActivo());
    }
}
