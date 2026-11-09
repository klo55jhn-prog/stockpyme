package com.stockpyme.backend.repositorio;

import com.stockpyme.backend.modelo.Movimiento;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MovimientoRepository extends JpaRepository<Movimiento, Long> {

    List<Movimiento> findByProductoIdOrderByFechaDescIdDesc(Long productoId, Pageable pagina);

    List<Movimiento> findAllByOrderByFechaDescIdDesc(Pageable pagina);

    /** Productos con más unidades movidas desde una fecha. Cada fila: [id, nombre, unidades]. */
    @Query("""
            select m.producto.id, m.producto.nombre, sum(abs(m.cantidad))
            from Movimiento m
            where m.fecha >= :desde
            group by m.producto.id, m.producto.nombre
            order by sum(abs(m.cantidad)) desc
            """)
    List<Object[]> masMovidosDesde(@Param("desde") Instant desde, Pageable pagina);
}
