package com.stockpyme.backend.repositorio;

import com.stockpyme.backend.modelo.Producto;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    /**
     * Lee el producto y bloquea su fila hasta que termine la transacción
     * (SELECT ... FOR UPDATE). Es la regla de integridad 6 del diseño:
     * dos salidas a la vez sobre el mismo producto se ejecutan una detrás de otra.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Producto p where p.id = :id")
    Optional<Producto> findByIdParaActualizar(@Param("id") Long id);

    /** Búsqueda por nombre (contiene) o por código de barras exacto. Texto vacío = todos. */
    @Query("""
            select p from Producto p
            where (:incluirInactivos = true or p.activo = true)
              and (:texto = ''
                   or lower(p.nombre) like lower(concat('%', :texto, '%'))
                   or p.codigoBarras = :texto)
            order by p.nombre
            """)
    List<Producto> buscar(@Param("texto") String texto, @Param("incluirInactivos") boolean incluirInactivos);

    @Query("""
            select p from Producto p
            where p.activo = true and p.stockActual <= p.stockMinimo
            order by p.stockActual, p.nombre
            """)
    List<Producto> findBajoMinimo();

    List<Producto> findByActivoTrue();

    Optional<Producto> findByCodigoBarras(String codigoBarras);

    boolean existsByCategoriaId(Long categoriaId);
}
