package com.stockpyme.backend.repositorio;

import com.stockpyme.backend.modelo.Proveedor;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {

    List<Proveedor> findAllByOrderByNombreAsc();

    List<Proveedor> findByActivoTrueOrderByNombreAsc();
}
