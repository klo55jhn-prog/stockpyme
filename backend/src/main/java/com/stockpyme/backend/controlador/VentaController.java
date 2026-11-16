package com.stockpyme.backend.controlador;

import com.stockpyme.backend.dto.VentaDto;
import com.stockpyme.backend.dto.VentaRequest;
import com.stockpyme.backend.seguridad.UsuarioAutenticado;
import com.stockpyme.backend.servicio.VentaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ventas")
public class VentaController {

    private final VentaService ventaService;

    public VentaController(VentaService ventaService) {
        this.ventaService = ventaService;
    }

    /** V2. Registra el carrito entero como una venta. Todo o nada. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VentaDto registrar(@Valid @RequestBody VentaRequest datos,
                              @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return ventaService.registrar(datos, usuario);
    }
}
