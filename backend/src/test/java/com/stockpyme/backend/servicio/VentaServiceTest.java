package com.stockpyme.backend.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

import com.stockpyme.backend.dto.MovimientoRequest;
import com.stockpyme.backend.dto.ProductoDto;
import com.stockpyme.backend.dto.VentaDto;
import com.stockpyme.backend.dto.VentaRequest;
import com.stockpyme.backend.error.ApiException;
import com.stockpyme.backend.modelo.Categoria;
import com.stockpyme.backend.modelo.Producto;
import com.stockpyme.backend.modelo.Rol;
import com.stockpyme.backend.repositorio.ProductoRepository;
import com.stockpyme.backend.seguridad.UsuarioAutenticado;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;

/** Pruebas de la venta con carrito (V2). */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class VentaServiceTest {

    private static final UsuarioAutenticado EMPLEADO = new UsuarioAutenticado(2L, "Luis", Rol.EMPLEADO);

    @Mock
    private MovimientoService movimientoService;
    @Mock
    private ProductoRepository productoRepository;

    private VentaService servicio;

    @BeforeEach
    void preparar() {
        servicio = new VentaService(movimientoService, productoRepository);
        when(movimientoService.registrar(eq(1L), any(), any())).thenReturn(dto(1L, "Martillo", "12.95", 4));
        when(movimientoService.registrar(eq(3L), any(), any())).thenReturn(dto(3L, "Cinta", "6.99", 7));
    }

    private static ProductoDto dto(Long id, String nombre, String precio, int stock) {
        Producto p = new Producto();
        p.setId(id);
        p.setNombre(nombre);
        p.setPrecioCompra(BigDecimal.ONE);
        p.setPrecioVenta(new BigDecimal(precio));
        p.setStockActual(stock);
        p.setStockMinimo(2);
        p.setCategoria(new Categoria("Ferretería"));
        return ProductoDto.de(p);
    }

    private static VentaRequest venta(VentaRequest.Linea... lineas) {
        return new VentaRequest(List.of(lineas));
    }

    @Test
    void sumaLasLineasRepetidasYLasProcesaEnOrdenDeId() {
        VentaDto resultado = servicio.registrar(venta(
                new VentaRequest.Linea(3L, 2),
                new VentaRequest.Linea(1L, 1),
                new VentaRequest.Linea(3L, 1)), EMPLEADO);

        InOrder orden = inOrder(movimientoService);
        ArgumentCaptor<MovimientoRequest> primera = ArgumentCaptor.forClass(MovimientoRequest.class);
        ArgumentCaptor<MovimientoRequest> segunda = ArgumentCaptor.forClass(MovimientoRequest.class);
        orden.verify(movimientoService).registrar(eq(1L), primera.capture(), eq(EMPLEADO));
        orden.verify(movimientoService).registrar(eq(3L), segunda.capture(), eq(EMPLEADO));

        assertThat(primera.getValue().cantidad()).isEqualTo(1);
        assertThat(segunda.getValue().cantidad()).isEqualTo(3);              // 2 + 1
        assertThat(segunda.getValue().motivo()).isEqualTo("Venta");
        assertThat(resultado.unidades()).isEqualTo(4);
        assertThat(resultado.productos()).hasSize(2);
    }

    @Test
    void elTotalEsPrecioPorCantidadDeCadaLinea() {
        VentaDto resultado = servicio.registrar(venta(
                new VentaRequest.Linea(1L, 2),      // 2 x 12,95 = 25,90
                new VentaRequest.Linea(3L, 3)), EMPLEADO);   // 3 x 6,99 = 20,97

        assertThat(resultado.total()).isEqualByComparingTo("46.87");
    }

    @Test
    void siUnaLineaFallaElErrorDiceDeQueProductoSeTrata() {
        Producto martillo = new Producto();
        martillo.setNombre("Martillo de carpintero");
        when(productoRepository.findById(1L)).thenReturn(Optional.of(martillo));
        when(movimientoService.registrar(eq(1L), any(), any()))
                .thenThrow(ApiException.conflicto("Stock insuficiente: quedan 1 unidades"));

        assertThatThrownBy(() -> servicio.registrar(venta(
                new VentaRequest.Linea(1L, 5),
                new VentaRequest.Linea(3L, 1)), EMPLEADO))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getEstado()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(e.getMessage())
                            .isEqualTo("Martillo de carpintero: Stock insuficiente: quedan 1 unidades");
                });
    }
}
