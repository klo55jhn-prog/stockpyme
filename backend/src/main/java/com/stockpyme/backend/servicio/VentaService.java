package com.stockpyme.backend.servicio;

import com.stockpyme.backend.dto.MovimientoRequest;
import com.stockpyme.backend.dto.ProductoDto;
import com.stockpyme.backend.dto.VentaDto;
import com.stockpyme.backend.dto.VentaRequest;
import com.stockpyme.backend.error.ApiException;
import com.stockpyme.backend.modelo.Producto;
import com.stockpyme.backend.modelo.TipoMovimiento;
import com.stockpyme.backend.repositorio.ProductoRepository;
import com.stockpyme.backend.seguridad.UsuarioAutenticado;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * V2. Registra una venta de varios productos de golpe, en UNA transacción:
 * o se registran todas las líneas, o no se registra ninguna.
 *
 * <p>Cada línea reutiliza {@link MovimientoService#registrar}, así que se aplican las mismas
 * reglas de integridad (stock nunca negativo, bloqueo de fila, historial) que en una salida suelta.
 */
@Service
public class VentaService {

    static final String MOTIVO = "Venta";

    private final MovimientoService movimientoService;
    private final ProductoRepository productoRepository;

    public VentaService(MovimientoService movimientoService, ProductoRepository productoRepository) {
        this.movimientoService = movimientoService;
        this.productoRepository = productoRepository;
    }

    @Transactional
    public VentaDto registrar(VentaRequest peticion, UsuarioAutenticado usuario) {
        // Si el carrito repite un producto se suman las cantidades. Y se procesan siempre en orden de id:
        // con un orden fijo, dos ventas simultáneas con los mismos productos no se bloquean entre sí.
        Map<Long, Integer> porProducto = new TreeMap<>();
        for (VentaRequest.Linea linea : peticion.lineas()) {
            porProducto.merge(linea.productoId(), linea.cantidad(), Integer::sum);
        }

        List<ProductoDto> productos = new ArrayList<>();
        int unidades = 0;
        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<Long, Integer> linea : porProducto.entrySet()) {
            try {
                ProductoDto actualizado = movimientoService.registrar(linea.getKey(),
                        new MovimientoRequest(TipoMovimiento.SALIDA, linea.getValue(), null, MOTIVO), usuario);
                productos.add(actualizado);
                unidades += linea.getValue();
                total = total.add(actualizado.precioVenta().multiply(BigDecimal.valueOf(linea.getValue())));
            } catch (ApiException e) {
                // Se avisa de qué producto falló. La excepción deshace toda la transacción.
                throw new ApiException(e.getEstado(), nombre(linea.getKey()) + ": " + e.getMessage());
            }
        }
        return new VentaDto(unidades, total, productos);
    }

    private String nombre(Long productoId) {
        return productoRepository.findById(productoId).map(Producto::getNombre).orElse("Producto " + productoId);
    }
}
