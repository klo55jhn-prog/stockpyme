package com.stockpyme.backend.servicio;

import com.stockpyme.backend.dto.ProveedorDto;
import com.stockpyme.backend.dto.ProveedorRequest;
import com.stockpyme.backend.error.ApiException;
import com.stockpyme.backend.modelo.Proveedor;
import com.stockpyme.backend.repositorio.ProveedorRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** CU-12 Gestionar proveedores. Los proveedores se desactivan, no se borran (D5). */
@Service
public class ProveedorService {

    private final ProveedorRepository proveedorRepository;

    public ProveedorService(ProveedorRepository proveedorRepository) {
        this.proveedorRepository = proveedorRepository;
    }

    @Transactional(readOnly = true)
    public List<ProveedorDto> listar(boolean incluirInactivos) {
        var lista = incluirInactivos
                ? proveedorRepository.findAllByOrderByNombreAsc()
                : proveedorRepository.findByActivoTrueOrderByNombreAsc();
        return lista.stream().map(ProveedorDto::de).toList();
    }

    @Transactional
    public ProveedorDto crear(ProveedorRequest datos) {
        Proveedor proveedor = new Proveedor();
        aplicar(proveedor, datos);
        return ProveedorDto.de(proveedorRepository.save(proveedor));
    }

    @Transactional
    public ProveedorDto editar(Long id, ProveedorRequest datos) {
        Proveedor proveedor = proveedorRepository.findById(id)
                .orElseThrow(() -> ApiException.noEncontrado("El proveedor no existe"));
        aplicar(proveedor, datos);
        if (datos.activo() != null) {
            proveedor.setActivo(datos.activo());
        }
        return ProveedorDto.de(proveedorRepository.save(proveedor));
    }

    private static void aplicar(Proveedor proveedor, ProveedorRequest datos) {
        proveedor.setNombre(datos.nombre().trim());
        proveedor.setTelefono(vacioANull(datos.telefono()));
        proveedor.setEmail(vacioANull(datos.email()));
    }

    private static String vacioANull(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
