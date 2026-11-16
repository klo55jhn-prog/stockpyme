package com.stockpyme.backend.controlador;

import com.stockpyme.backend.dto.ActividadDto;
import com.stockpyme.backend.dto.ResumenDto;
import com.stockpyme.backend.servicio.InformeService;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/informes")
@PreAuthorize("hasRole('PROPIETARIO')")
public class InformeController {

    private static final MediaType CSV = new MediaType("text", "csv", StandardCharsets.UTF_8);

    private final InformeService informeService;

    public InformeController(InformeService informeService) {
        this.informeService = informeService;
    }

    @GetMapping("/resumen")
    public ResumenDto resumen() {
        return informeService.resumen();
    }

    /** V2. Entradas, salidas y ventas por día para la gráfica del panel. */
    @GetMapping("/actividad")
    public ActividadDto actividad(@RequestParam(defaultValue = "7") int dias) {
        return informeService.actividad(dias);
    }

    /** RF-15. Inventario completo en CSV (se abre con Excel). */
    @GetMapping("/inventario.csv")
    public ResponseEntity<byte[]> inventarioCsv() {
        return csv(informeService.inventarioCsv(), "stockpyme-inventario");
    }

    /** RF-15. Movimientos de los últimos días en CSV. */
    @GetMapping("/movimientos.csv")
    public ResponseEntity<byte[]> movimientosCsv(@RequestParam(defaultValue = "30") int dias) {
        return csv(informeService.movimientosCsv(dias), "stockpyme-movimientos");
    }

    private static ResponseEntity<byte[]> csv(String contenido, String prefijo) {
        String nombre = prefijo + "-" + LocalDate.now() + ".csv";
        return ResponseEntity.ok()
                .contentType(CSV)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(nombre).build().toString())
                .body(contenido.getBytes(StandardCharsets.UTF_8));
    }
}
