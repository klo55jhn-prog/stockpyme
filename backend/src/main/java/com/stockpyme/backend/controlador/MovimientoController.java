package com.stockpyme.backend.controlador;

import com.stockpyme.backend.dto.MovimientoDto;
import com.stockpyme.backend.servicio.MovimientoService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/movimientos")
public class MovimientoController {

    private final MovimientoService movimientoService;

    public MovimientoController(MovimientoService movimientoService) {
        this.movimientoService = movimientoService;
    }

    /** Últimos movimientos de todo el comercio (actividad del día). */
    @GetMapping("/recientes")
    public List<MovimientoDto> recientes(@RequestParam(defaultValue = "50") int limite) {
        return movimientoService.recientes(Math.clamp(limite, 1, 200));
    }
}
