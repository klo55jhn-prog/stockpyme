package com.stockpyme.backend.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stockpyme.backend.dto.MovimientoRequest;
import com.stockpyme.backend.dto.ProductoDto;
import com.stockpyme.backend.error.ApiException;
import com.stockpyme.backend.modelo.Categoria;
import com.stockpyme.backend.modelo.Movimiento;
import com.stockpyme.backend.modelo.Producto;
import com.stockpyme.backend.modelo.Rol;
import com.stockpyme.backend.modelo.TipoMovimiento;
import com.stockpyme.backend.modelo.Usuario;
import com.stockpyme.backend.repositorio.MovimientoRepository;
import com.stockpyme.backend.repositorio.ProductoRepository;
import com.stockpyme.backend.repositorio.UsuarioRepository;
import com.stockpyme.backend.seguridad.UsuarioAutenticado;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;

/**
 * Pruebas de la lógica de stock (RNF-07). No necesitan base de datos:
 * los repositorios se sustituyen por objetos simulados (mocks).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MovimientoServiceTest {

    private static final UsuarioAutenticado PROPIETARIO = new UsuarioAutenticado(1L, "Ana", Rol.PROPIETARIO);
    private static final UsuarioAutenticado EMPLEADO = new UsuarioAutenticado(2L, "Luis", Rol.EMPLEADO);

    @Mock
    private ProductoRepository productoRepository;
    @Mock
    private MovimientoRepository movimientoRepository;
    @Mock
    private UsuarioRepository usuarioRepository;

    private MovimientoService servicio;
    private Producto producto;

    @BeforeEach
    void preparar() {
        servicio = new MovimientoService(productoRepository, movimientoRepository, usuarioRepository);
        producto = new Producto();
        producto.setId(10L);
        producto.setNombre("Martillo");
        producto.setPrecioCompra(new BigDecimal("6.50"));
        producto.setPrecioVenta(new BigDecimal("12.95"));
        producto.setStockActual(5);
        producto.setStockMinimo(2);
        producto.setCategoria(new Categoria("Ferretería"));
        when(productoRepository.findByIdParaActualizar(10L)).thenReturn(Optional.of(producto));
        when(usuarioRepository.getReferenceById(any())).thenReturn(new Usuario());
    }

    @Test
    void entradaSumaStockYGuardaCantidadPositiva() {
        ProductoDto resultado = servicio.registrar(10L, peticion(TipoMovimiento.ENTRADA, 3), EMPLEADO);

        assertThat(resultado.stockActual()).isEqualTo(8);
        assertThat(movimientoGuardado().getCantidad()).isEqualTo(3);
    }

    @Test
    void salidaRestaStockYGuardaCantidadNegativa() {
        ProductoDto resultado = servicio.registrar(10L, peticion(TipoMovimiento.SALIDA, 4), EMPLEADO);

        assertThat(resultado.stockActual()).isEqualTo(1);
        assertThat(resultado.bajoMinimo()).isTrue();
        assertThat(movimientoGuardado().getCantidad()).isEqualTo(-4);
    }

    @Test
    void salidaConStockInsuficienteSeRechazaSinGuardarNada() {
        assertThatThrownBy(() -> servicio.registrar(10L, peticion(TipoMovimiento.SALIDA, 6), EMPLEADO))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("quedan 5")
                .extracting(e -> ((ApiException) e).getEstado()).isEqualTo(HttpStatus.CONFLICT);

        assertThat(producto.getStockActual()).isEqualTo(5);
        verify(movimientoRepository, never()).save(any());
    }

    @Test
    void salidaDeTodoElStockDejaCero() {
        ProductoDto resultado = servicio.registrar(10L, peticion(TipoMovimiento.SALIDA, 5), EMPLEADO);

        assertThat(resultado.stockActual()).isZero();
    }

    @Test
    void cantidadCeroONegativaSeRechaza() {
        assertThatThrownBy(() -> servicio.registrar(10L, peticion(TipoMovimiento.ENTRADA, 0), EMPLEADO))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> servicio.registrar(10L, peticion(TipoMovimiento.SALIDA, -2), EMPLEADO))
                .isInstanceOf(ApiException.class);
        verify(movimientoRepository, never()).save(any());
    }

    @Test
    void ajusteCalculaLaDiferenciaConElStockContado() {
        var ajuste = new MovimientoRequest(TipoMovimiento.AJUSTE, null, 2, "Recuento mensual");

        ProductoDto resultado = servicio.registrar(10L, ajuste, PROPIETARIO);

        assertThat(resultado.stockActual()).isEqualTo(2);
        Movimiento guardado = movimientoGuardado();
        assertThat(guardado.getCantidad()).isEqualTo(-3);
        assertThat(guardado.getMotivo()).isEqualTo("Recuento mensual");
    }

    @Test
    void empleadoNoPuedeHacerAjustes() {
        var ajuste = new MovimientoRequest(TipoMovimiento.AJUSTE, null, 2, "Recuento");

        assertThatThrownBy(() -> servicio.registrar(10L, ajuste, EMPLEADO))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getEstado()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void ajusteSinMotivoSeRechaza() {
        var ajuste = new MovimientoRequest(TipoMovimiento.AJUSTE, null, 2, "   ");

        assertThatThrownBy(() -> servicio.registrar(10L, ajuste, PROPIETARIO))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("motivo");
    }

    @Test
    void productoDesactivadoNoAdmiteMovimientos() {
        producto.setActivo(false);

        assertThatThrownBy(() -> servicio.registrar(10L, peticion(TipoMovimiento.ENTRADA, 1), PROPIETARIO))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("desactivado");
    }

    private static MovimientoRequest peticion(TipoMovimiento tipo, int cantidad) {
        return new MovimientoRequest(tipo, cantidad, null, null);
    }

    private Movimiento movimientoGuardado() {
        ArgumentCaptor<Movimiento> captor = ArgumentCaptor.forClass(Movimiento.class);
        verify(movimientoRepository).save(captor.capture());
        return captor.getValue();
    }
}
