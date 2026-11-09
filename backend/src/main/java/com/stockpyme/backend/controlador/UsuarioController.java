package com.stockpyme.backend.controlador;

import com.stockpyme.backend.dto.UsuarioDto;
import com.stockpyme.backend.dto.UsuarioRequest;
import com.stockpyme.backend.seguridad.UsuarioAutenticado;
import com.stockpyme.backend.servicio.UsuarioService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** CU-13. Todo el controlador es solo para el propietario. */
@RestController
@RequestMapping("/api/usuarios")
@PreAuthorize("hasRole('PROPIETARIO')")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public List<UsuarioDto> listar() {
        return usuarioService.listar();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioDto crear(@Valid @RequestBody UsuarioRequest datos) {
        return usuarioService.crear(datos);
    }

    @PutMapping("/{id}")
    public UsuarioDto editar(@PathVariable Long id, @Valid @RequestBody UsuarioRequest datos,
                             @AuthenticationPrincipal UsuarioAutenticado actual) {
        return usuarioService.editar(id, datos, actual);
    }
}
