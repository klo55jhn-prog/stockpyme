package com.stockpyme.backend.controlador;

import com.stockpyme.backend.dto.CategoriaDto;
import com.stockpyme.backend.dto.CategoriaRequest;
import com.stockpyme.backend.servicio.CategoriaService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/categorias")
public class CategoriaController {

    private final CategoriaService categoriaService;

    public CategoriaController(CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
    }

    @GetMapping
    public List<CategoriaDto> listar() {
        return categoriaService.listar();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('PROPIETARIO')")
    public CategoriaDto crear(@Valid @RequestBody CategoriaRequest datos) {
        return categoriaService.crear(datos);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('PROPIETARIO')")
    public CategoriaDto renombrar(@PathVariable Long id, @Valid @RequestBody CategoriaRequest datos) {
        return categoriaService.renombrar(id, datos);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('PROPIETARIO')")
    public void borrar(@PathVariable Long id) {
        categoriaService.borrar(id);
    }
}
