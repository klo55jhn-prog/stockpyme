package com.stockpyme.backend.servicio;

import com.stockpyme.backend.dto.ResumenDto;
import com.stockpyme.backend.modelo.Producto;
import com.stockpyme.backend.repositorio.MovimientoRepository;
import com.stockpyme.backend.repositorio.ProductoRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** RF-13: valor del inventario y productos con más movimiento. */
@Service
public class InformeService {

    private final ProductoRepository productoRepository;
    private final MovimientoRepository movimientoRepository;

    public InformeService(ProductoRepository productoRepository, MovimientoRepository movimientoRepository) {
        this.productoRepository = productoRepository;
        this.movimientoRepository = movimientoRepository;
    }

    @Transactional(readOnly = true)
    public ResumenDto resumen() {
        List<Producto> activos = productoRepository.findByActivoTrue();
        long unidades = 0;
        int bajoMinimo = 0;
        BigDecimal valorCompra = BigDecimal.ZERO;
        BigDecimal valorVenta = BigDecimal.ZERO;
        for (Producto p : activos) {
            BigDecimal stock = BigDecimal.valueOf(p.getStockActual());
            unidades += p.getStockActual();
            valorCompra = valorCompra.add(p.getPrecioCompra().multiply(stock));
            valorVenta = valorVenta.add(p.getPrecioVenta().multiply(stock));
            if (p.isBajoMinimo()) {
                bajoMinimo++;
            }
        }

        Instant hace30Dias = Instant.now().minus(Duration.ofDays(30));
        List<ResumenDto.ProductoMovido> masMovidos = movimientoRepository
                .masMovidosDesde(hace30Dias, PageRequest.of(0, 5)).stream()
                .map(fila -> new ResumenDto.ProductoMovido(
                        (Long) fila[0], (String) fila[1], ((Number) fila[2]).longValue()))
                .toList();

        return new ResumenDto(activos.size(), unidades, valorCompra, valorVenta, bajoMinimo, masMovidos);
    }
}
