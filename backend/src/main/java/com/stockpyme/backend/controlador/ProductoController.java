package com.stockpyme.backend.controlador;

import com.stockpyme.backend.dto.MovimientoDto;
import com.stockpyme.backend.dto.MovimientoRequest;
import com.stockpyme.backend.dto.ProductoDto;
import com.stockpyme.backend.dto.ProductoRequest;
import com.stockpyme.backend.seguridad.UsuarioAutenticado;
import com.stockpyme.backend.servicio.MovimientoService;
import com.stockpyme.backend.servicio.ProductoService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/productos")
public class ProductoController {

    private static final int LIMITE_HISTORIAL = 200;

    private final ProductoService productoService;
    private final MovimientoService movimientoService;

    public ProductoController(ProductoService productoService, MovimientoService movimientoService) {
        this.productoService = productoService;
        this.movimientoService = movimientoService;
    }

    /** CU-05. Busca por nombre o código. Sin texto devuelve todos los activos. */
    @GetMapping
    public List<ProductoDto> buscar(@RequestParam(defaultValue = "") String buscar,
                                    @RequestParam(defaultValue = "false") boolean incluirInactivos) {
        return productoService.buscar(buscar, incluirInactivos);
    }

    /** CU-11. Productos activos con el stock en el mínimo o por debajo. */
    @GetMapping("/bajo-minimo")
    public List<ProductoDto> bajoMinimo() {
        return productoService.bajoMinimo();
    }

    @GetMapping("/{id}")
    public ProductoDto obtener(@PathVariable Long id) {
        return productoService.obtener(id);
    }

    /** RF-12. Lo usa el escáner de códigos de barras de la app. */
    @GetMapping("/codigo/{codigo}")
    public ProductoDto porCodigo(@PathVariable String codigo) {
        return productoService.porCodigo(codigo);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('PROPIETARIO')")
    public ProductoDto crear(@Valid @RequestBody ProductoRequest datos) {
        return productoService.crear(datos);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('PROPIETARIO')")
    public ProductoDto editar(@PathVariable Long id, @Valid @RequestBody ProductoRequest datos) {
        return productoService.editar(id, datos);
    }

    /** CU-10. */
    @GetMapping("/{id}/movimientos")
    public List<MovimientoDto> historial(@PathVariable Long id) {
        return movimientoService.historial(id, LIMITE_HISTORIAL);
    }

    /** CU-07, CU-08 y CU-09. Devuelve el producto con el stock ya actualizado. */
    @PostMapping("/{id}/movimientos")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductoDto registrarMovimiento(@PathVariable Long id,
                                           @Valid @RequestBody MovimientoRequest datos,
                                           @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return movimientoService.registrar(id, datos, usuario);
    }
}
