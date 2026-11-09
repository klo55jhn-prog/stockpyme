package com.stockpyme.backend.servicio;

import com.stockpyme.backend.dto.MovimientoDto;
import com.stockpyme.backend.dto.MovimientoRequest;
import com.stockpyme.backend.dto.ProductoDto;
import com.stockpyme.backend.error.ApiException;
import com.stockpyme.backend.modelo.Movimiento;
import com.stockpyme.backend.modelo.Producto;
import com.stockpyme.backend.modelo.TipoMovimiento;
import com.stockpyme.backend.repositorio.MovimientoRepository;
import com.stockpyme.backend.repositorio.ProductoRepository;
import com.stockpyme.backend.repositorio.UsuarioRepository;
import com.stockpyme.backend.seguridad.UsuarioAutenticado;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lógica central de la app: cambiar el stock (CU-07, CU-08 y CU-09).
 * Aplica las reglas de integridad 1 a 6 del diseño.
 */
@Service
public class MovimientoService {

    private final ProductoRepository productoRepository;
    private final MovimientoRepository movimientoRepository;
    private final UsuarioRepository usuarioRepository;

    public MovimientoService(ProductoRepository productoRepository,
                             MovimientoRepository movimientoRepository,
                             UsuarioRepository usuarioRepository) {
        this.productoRepository = productoRepository;
        this.movimientoRepository = movimientoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Registra un movimiento y actualiza el stock en UNA transacción (regla 5).
     * Si algo falla, no se guarda ni el movimiento ni el cambio de stock.
     */
    @Transactional
    public ProductoDto registrar(Long productoId, MovimientoRequest peticion, UsuarioAutenticado usuario) {
        // Regla 6: bloquea la fila del producto. Otra transacción sobre el mismo producto
        // espera aquí hasta que esta termine, y después lee el stock ya actualizado.
        Producto producto = productoRepository.findByIdParaActualizar(productoId)
                .orElseThrow(() -> ApiException.noEncontrado("El producto no existe"));

        if (!producto.isActivo()) {
            throw ApiException.conflicto("El producto está desactivado");
        }

        int cambio = calcularCambio(producto, peticion, usuario);
        int stockNuevo = producto.getStockActual() + cambio;

        // Regla 4: el stock nunca queda negativo.
        if (stockNuevo < 0) {
            throw ApiException.conflicto(
                    "Stock insuficiente: quedan " + producto.getStockActual() + " unidades");
        }

        String motivo = limpiar(peticion.motivo());
        producto.setStockActual(stockNuevo);
        Movimiento movimiento = new Movimiento(peticion.tipo(), cambio, motivo, producto,
                usuarioRepository.getReferenceById(usuario.id()));
        movimientoRepository.save(movimiento);
        productoRepository.save(producto);
        return ProductoDto.de(producto);
    }

    /** Devuelve el cambio de stock con signo (D1) según el tipo de movimiento. */
    private int calcularCambio(Producto producto, MovimientoRequest peticion, UsuarioAutenticado usuario) {
        TipoMovimiento tipo = peticion.tipo();
        switch (tipo) {
            case ENTRADA -> {
                return unidadesPositivas(peticion);               // Regla 1: ENTRADA > 0
            }
            case SALIDA -> {
                return -unidadesPositivas(peticion);              // Regla 2: SALIDA < 0
            }
            case AJUSTE -> {
                if (!usuario.esPropietario()) {                   // D9 y RF-07
                    throw ApiException.prohibido("Solo el propietario puede hacer ajustes");
                }
                if (limpiar(peticion.motivo()) == null) {          // Regla 3
                    throw ApiException.peticionIncorrecta("El ajuste necesita un motivo");
                }
                Integer contado = peticion.stockNuevo();
                if (contado == null || contado < 0) {
                    throw ApiException.peticionIncorrecta("Indica el stock real contado (0 o más)");
                }
                int cambio = contado - producto.getStockActual();
                if (cambio == 0) {
                    throw ApiException.peticionIncorrecta(
                            "El stock ya es " + contado + ": no hay nada que ajustar");
                }
                return cambio;
            }
            default -> throw ApiException.peticionIncorrecta("Tipo de movimiento no válido");
        }
    }

    private static int unidadesPositivas(MovimientoRequest peticion) {
        Integer cantidad = peticion.cantidad();
        if (cantidad == null || cantidad <= 0) {
            throw ApiException.peticionIncorrecta("La cantidad debe ser un número mayor que 0");
        }
        return cantidad;
    }

    private static String limpiar(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    @Transactional(readOnly = true)
    public List<MovimientoDto> historial(Long productoId, int limite) {
        if (!productoRepository.existsById(productoId)) {
            throw ApiException.noEncontrado("El producto no existe");
        }
        return movimientoRepository.findByProductoIdOrderByFechaDescIdDesc(productoId, PageRequest.of(0, limite))
                .stream().map(MovimientoDto::de).toList();
    }

    @Transactional(readOnly = true)
    public List<MovimientoDto> recientes(int limite) {
        return movimientoRepository.findAllByOrderByFechaDescIdDesc(PageRequest.of(0, limite))
                .stream().map(MovimientoDto::de).toList();
    }
}
