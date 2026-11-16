package com.stockpyme.backend.dto;

import com.stockpyme.backend.modelo.Producto;
import com.stockpyme.backend.modelo.Proveedor;
import java.math.BigDecimal;

/**
 * Una línea de la lista de reposición: qué producto hay que pedir y cuánto.
 * {@code sugerido} lleva el stock hasta el doble del mínimo (como mínimo, una unidad).
 */
public record LineaReposicionDto(
        Long productoId,
        String nombre,
        int stockActual,
        int stockMinimo,
        int sugerido,
        BigDecimal precioCompra,
        BigDecimal coste,
        Long proveedorId,
        String proveedorNombre,
        String proveedorTelefono,
        String proveedorEmail) {

    public static LineaReposicionDto de(Producto p) {
        int objetivo = Math.max(p.getStockMinimo() * 2, 1);
        int sugerido = objetivo - p.getStockActual();
        Proveedor prov = p.getProveedor();
        return new LineaReposicionDto(
                p.getId(),
                p.getNombre(),
                p.getStockActual(),
                p.getStockMinimo(),
                sugerido,
                p.getPrecioCompra(),
                p.getPrecioCompra().multiply(BigDecimal.valueOf(sugerido)),
                prov == null ? null : prov.getId(),
                prov == null ? null : prov.getNombre(),
                prov == null ? null : prov.getTelefono(),
                prov == null ? null : prov.getEmail());
    }
}
