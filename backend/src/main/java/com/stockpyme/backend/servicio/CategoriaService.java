package com.stockpyme.backend.servicio;

import com.stockpyme.backend.dto.CategoriaDto;
import com.stockpyme.backend.dto.CategoriaRequest;
import com.stockpyme.backend.error.ApiException;
import com.stockpyme.backend.modelo.Categoria;
import com.stockpyme.backend.repositorio.CategoriaRepository;
import com.stockpyme.backend.repositorio.ProductoRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** CU-06 Gestionar categorías. */
@Service
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final ProductoRepository productoRepository;

    public CategoriaService(CategoriaRepository categoriaRepository, ProductoRepository productoRepository) {
        this.categoriaRepository = categoriaRepository;
        this.productoRepository = productoRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoriaDto> listar() {
        return categoriaRepository.findAllByOrderByNombreAsc().stream().map(CategoriaDto::de).toList();
    }

    @Transactional
    public CategoriaDto crear(CategoriaRequest datos) {
        String nombre = datos.nombre().trim();
        comprobarNombreLibre(nombre, null);
        return CategoriaDto.de(categoriaRepository.save(new Categoria(nombre)));
    }

    @Transactional
    public CategoriaDto renombrar(Long id, CategoriaRequest datos) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> ApiException.noEncontrado("La categoría no existe"));
        String nombre = datos.nombre().trim();
        comprobarNombreLibre(nombre, id);
        categoria.setNombre(nombre);
        return CategoriaDto.de(categoriaRepository.save(categoria));
    }

    /** Solo se puede borrar una categoría vacía: si tuviera productos, quedarían sin categoría. */
    @Transactional
    public void borrar(Long id) {
        if (!categoriaRepository.existsById(id)) {
            throw ApiException.noEncontrado("La categoría no existe");
        }
        if (productoRepository.existsByCategoriaId(id)) {
            throw ApiException.conflicto("No se puede borrar: la categoría tiene productos");
        }
        categoriaRepository.deleteById(id);
    }

    private void comprobarNombreLibre(String nombre, Long idActual) {
        categoriaRepository.findByNombreIgnoreCase(nombre)
                .filter(otra -> !otra.getId().equals(idActual))
                .ifPresent(otra -> {
                    throw ApiException.conflicto("Ya existe la categoría \"" + otra.getNombre() + "\"");
                });
    }
}
