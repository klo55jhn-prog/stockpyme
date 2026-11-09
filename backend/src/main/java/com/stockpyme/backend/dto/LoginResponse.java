package com.stockpyme.backend.dto;

public record LoginResponse(String token, UsuarioDto usuario) {
}
