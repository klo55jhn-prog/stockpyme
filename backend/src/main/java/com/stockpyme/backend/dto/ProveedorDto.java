package com.stockpyme.backend.dto;

import com.stockpyme.backend.modelo.Proveedor;

public record ProveedorDto(Long id, String nombre, String telefono, String email, boolean activo) {

    public static ProveedorDto de(Proveedor p) {
        return new ProveedorDto(p.getId(), p.getNombre(), p.getTelefono(), p.getEmail(), p.isActivo());
    }
}
