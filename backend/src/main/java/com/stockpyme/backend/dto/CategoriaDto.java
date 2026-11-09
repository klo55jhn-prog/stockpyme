package com.stockpyme.backend.dto;

import com.stockpyme.backend.modelo.Categoria;

public record CategoriaDto(Long id, String nombre) {

    public static CategoriaDto de(Categoria c) {
        return new CategoriaDto(c.getId(), c.getNombre());
    }
}
