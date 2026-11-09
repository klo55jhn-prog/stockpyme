package com.stockpyme.backend.seguridad;

import com.stockpyme.backend.modelo.Rol;

/** Quién hace la petición. Lo rellena JwtFilter a partir del token. */
public record UsuarioAutenticado(Long id, String nombre, Rol rol) {

    public boolean esPropietario() {
        return rol == Rol.PROPIETARIO;
    }
}
