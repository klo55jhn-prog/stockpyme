package com.stockpyme.backend.controlador;

import com.stockpyme.backend.dto.ResumenDto;
import com.stockpyme.backend.servicio.InformeService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/informes")
@PreAuthorize("hasRole('PROPIETARIO')")
public class InformeController {

    private final InformeService informeService;

    public InformeController(InformeService informeService) {
        this.informeService = informeService;
    }

    @GetMapping("/resumen")
    public ResumenDto resumen() {
        return informeService.resumen();
    }
}
