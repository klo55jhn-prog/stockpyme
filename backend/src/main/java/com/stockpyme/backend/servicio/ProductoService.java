package com.stockpyme.backend.servicio;

import com.stockpyme.backend.dto.ProductoDto;
import com.stockpyme.backend.dto.ProductoRequest;
import com.stockpyme.backend.error.ApiException;
import com.stockpyme.backend.modelo.Categoria;
import com.stockpyme.backend.modelo.Producto;
import com.stockpyme.backend.modelo.Proveedor;
import com.stockpyme.backend.repositorio.CategoriaRepository;
import com.stockpyme.backend.repositorio.ProductoRepository;
import com.stockpyme.backend.repositorio.ProveedorRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Alta, edición, desactivación y consulta de productos (CU-03, CU-04, CU-05, CU-11). */
@Service
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;
    private final ProveedorRepository proveedorRepository;

    public ProductoService(ProductoRepository productoRepository,
                           CategoriaRepository categoriaRepository,
                           ProveedorRepository proveedorRepository) {
        this.productoRepository = productoRepository;
        this.categoriaRepository = categoriaRepository;
        this.proveedorRepository = proveedorRepository;
    }

    @Transactional(readOnly = true)
    public List<ProductoDto> buscar(String texto, boolean incluirInactivos) {
        String limpio = texto == null ? "" : texto.trim();
        return productoRepository.buscar(limpio, incluirInactivos).stream().map(ProductoDto::de).toList();
    }

    @Transactional(readOnly = true)
    public List<ProductoDto> bajoMinimo() {
        return productoRepository.findBajoMinimo().stream().map(ProductoDto::de).toList();
    }

    @Transactional(readOnly = true)
    public ProductoDto obtener(Long id) {
        return ProductoDto.de(buscarEntidad(id));
    }

    @Transactional(readOnly = true)
    public ProductoDto porCodigo(String codigo) {
        return productoRepository.findByCodigoBarras(codigo.trim())
                .map(ProductoDto::de)
                .orElseThrow(() -> ApiException.noEncontrado("No hay ningún producto con el código " + codigo));
    }

    /** CU-03. El producto nace con stock 0: el stock inicial se registra como entrada (CU-07). */
    @Transactional
    public ProductoDto crear(ProductoRequest datos) {
        Producto producto = new Producto();
        producto.setStockActual(0);
        aplicar(producto, datos);
        return ProductoDto.de(productoRepository.save(producto));
    }

    /** CU-04. Editar no cambia el stock. Desactivar es activo = false (D5). */
    @Transactional
    public ProductoDto editar(Long id, ProductoRequest datos) {
        Producto producto = buscarEntidad(id);
        aplicar(producto, datos);
        if (datos.activo() != null) {
            producto.setActivo(datos.activo());
        }
        return ProductoDto.de(productoRepository.save(producto));
    }

    private void aplicar(Producto producto, ProductoRequest datos) {
        String codigo = datos.codigoBarras() == null || datos.codigoBarras().isBlank()
                ? null : datos.codigoBarras().trim();
        if (codigo != null) {
            productoRepository.findByCodigoBarras(codigo)
                    .filter(otro -> !otro.getId().equals(producto.getId()))
                    .ifPresent(otro -> {
                        throw ApiException.conflicto(
                                "El código " + codigo + " ya pertenece a \"" + otro.getNombre() + "\"");
                    });
        }
        Categoria categoria = categoriaRepository.findById(datos.categoriaId())
                .orElseThrow(() -> ApiException.peticionIncorrecta("La categoría no existe"));
        Proveedor proveedor = null;
        if (datos.proveedorId() != null) {
            proveedor = proveedorRepository.findById(datos.proveedorId())
                    .orElseThrow(() -> ApiException.peticionIncorrecta("El proveedor no existe"));
        }
        producto.setNombre(datos.nombre().trim());
        producto.setCodigoBarras(codigo);
        producto.setPrecioCompra(datos.precioCompra());
        producto.setPrecioVenta(datos.precioVenta());
        producto.setStockMinimo(datos.stockMinimo());
        producto.setCategoria(categoria);
        producto.setProveedor(proveedor);
    }

    private Producto buscarEntidad(Long id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> ApiException.noEncontrado("El producto no existe"));
    }
}
