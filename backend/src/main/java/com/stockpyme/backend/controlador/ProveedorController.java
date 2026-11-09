package com.stockpyme.backend.controlador;

import com.stockpyme.backend.dto.ProveedorDto;
import com.stockpyme.backend.dto.ProveedorRequest;
import com.stockpyme.backend.servicio.ProveedorService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/proveedores")
public class ProveedorController {

    private final ProveedorService proveedorService;

    public ProveedorController(ProveedorService proveedorService) {
        this.proveedorService = proveedorService;
    }

    @GetMapping
    public List<ProveedorDto> listar(@RequestParam(defaultValue = "false") boolean incluirInactivos) {
        return proveedorService.listar(incluirInactivos);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('PROPIETARIO')")
    public ProveedorDto crear(@Valid @RequestBody ProveedorRequest datos) {
        return proveedorService.crear(datos);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('PROPIETARIO')")
    public ProveedorDto editar(@PathVariable Long id, @Valid @RequestBody ProveedorRequest datos) {
        return proveedorService.editar(id, datos);
    }
}
