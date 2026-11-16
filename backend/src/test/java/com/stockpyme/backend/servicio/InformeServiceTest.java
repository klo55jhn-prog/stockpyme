package com.stockpyme.backend.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.stockpyme.backend.dto.ActividadDto;
import com.stockpyme.backend.dto.LineaReposicionDto;
import com.stockpyme.backend.modelo.Categoria;
import com.stockpyme.backend.modelo.Movimiento;
import com.stockpyme.backend.modelo.Producto;
import com.stockpyme.backend.modelo.Proveedor;
import com.stockpyme.backend.modelo.TipoMovimiento;
import com.stockpyme.backend.modelo.Usuario;
import com.stockpyme.backend.repositorio.MovimientoRepository;
import com.stockpyme.backend.repositorio.ProductoRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/** Pruebas de los informes de la V2: actividad por día, CSV y lista de reposición. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InformeServiceTest {

    private static final ZoneId ZONA = ZoneId.of("Europe/Madrid");

    @Mock
    private ProductoRepository productoRepository;
    @Mock
    private MovimientoRepository movimientoRepository;

    private InformeService servicio;
    private Producto producto;
    private Usuario usuario;

    @BeforeEach
    void preparar() {
        servicio = new InformeService(productoRepository, movimientoRepository, "Europe/Madrid");
        producto = producto("Martillo", 5, 2);
        usuario = new Usuario();
        usuario.setNombre("Ana");
    }

    private static Producto producto(String nombre, int stock, int minimo) {
        Producto p = new Producto();
        p.setId(1L);
        p.setNombre(nombre);
        p.setPrecioCompra(new BigDecimal("6.50"));
        p.setPrecioVenta(new BigDecimal("12.95"));
        p.setStockActual(stock);
        p.setStockMinimo(minimo);
        p.setCategoria(new Categoria("Ferretería"));
        return p;
    }

    private Movimiento salida(int unidades, String motivo, BigDecimal precio, LocalDate dia) {
        Instant fecha = dia.atTime(12, 0).atZone(ZONA).toInstant();
        return new Movimiento(TipoMovimiento.SALIDA, -unidades, motivo, producto, usuario, fecha, precio);
    }

    @Test
    void laActividadAgrupaPorDiaYSoloValoraLasVentas() {
        LocalDate hoy = LocalDate.now(ZONA);
        when(movimientoRepository.desde(any())).thenReturn(List.of(
                salida(2, "Venta", new BigDecimal("10.00"), hoy),
                salida(1, null, new BigDecimal("10.00"), hoy),               // sin motivo: cuenta como venta
                salida(4, "Merma", new BigDecimal("10.00"), hoy),            // merma: no son ingresos
                salida(3, "Venta", new BigDecimal("8.00"), hoy.minusDays(1))));

        ActividadDto actividad = servicio.actividad(7);

        assertThat(actividad.dias()).hasSize(7);
        assertThat(actividad.dias().get(6).fecha()).isEqualTo(hoy);
        assertThat(actividad.dias().get(6).salidas()).isEqualTo(7);          // 2 + 1 + 4
        assertThat(actividad.dias().get(6).ventas()).isEqualByComparingTo("30.00");
        assertThat(actividad.dias().get(5).ventas()).isEqualByComparingTo("24.00");
        assertThat(actividad.ventasHoy()).isEqualByComparingTo("30.00");
        assertThat(actividad.ventasPeriodo()).isEqualByComparingTo("54.00");
        assertThat(actividad.unidadesVendidasPeriodo()).isEqualTo(6);
        assertThat(actividad.movimientosHoy()).isEqualTo(3);
    }

    @Test
    void sinPrecioGuardadoUsaElPrecioActualDelProducto() {
        LocalDate hoy = LocalDate.now(ZONA);
        when(movimientoRepository.desde(any())).thenReturn(List.of(salida(2, "Venta", null, hoy)));

        assertThat(servicio.actividad(3).ventasHoy()).isEqualByComparingTo("25.90");
    }

    @Test
    void losDiasSinMovimientosSalenConCeros() {
        when(movimientoRepository.desde(any())).thenReturn(List.of());

        ActividadDto actividad = servicio.actividad(5);

        assertThat(actividad.dias()).hasSize(5).allSatisfy(d -> {
            assertThat(d.entradas()).isZero();
            assertThat(d.ventas()).isEqualByComparingTo("0");
        });
    }

    @Test
    void elNumeroDeDiasSeLimita() {
        when(movimientoRepository.desde(any())).thenReturn(List.of());

        assertThat(servicio.actividad(0).dias()).hasSize(1);
        assertThat(servicio.actividad(5000).dias()).hasSize(90);
    }

    @Test
    void elCsvNeutralizaFormulasYEscapaLosSeparadores() {
        assertThat(InformeService.celda("=CMD()")).isEqualTo("'=CMD()");
        assertThat(InformeService.celda("@SUMA(A1)")).isEqualTo("'@SUMA(A1)");
        assertThat(InformeService.celda("Tornillos; caja")).isEqualTo("\"Tornillos; caja\"");
        assertThat(InformeService.celda("Pintura \"blanca\"")).isEqualTo("\"Pintura \"\"blanca\"\"\"");
        assertThat(InformeService.celda(null)).isEmpty();
    }

    @Test
    void elCsvDelInventarioUsaComaDecimalYMarcaElEstado() {
        producto.setStockActual(0);
        when(productoRepository.buscar(eq(""), anyBoolean())).thenReturn(List.of(producto));

        String csv = servicio.inventarioCsv();

        assertThat(csv).startsWith("﻿Código;Producto");
        assertThat(csv).contains("Martillo;Ferretería;;0;2;6,50;12,95;0,00;0,00;Agotado");
    }

    @Test
    void laReposicionSugiereLlegarAlDobleDelMinimo() {
        Proveedor proveedor = new Proveedor();
        proveedor.setId(7L);
        proveedor.setNombre("Suministros");
        Producto p = producto("Cinta", 1, 3);
        p.setProveedor(proveedor);

        LineaReposicionDto linea = LineaReposicionDto.de(p);

        assertThat(linea.sugerido()).isEqualTo(5);                           // objetivo 6, hay 1
        assertThat(linea.coste()).isEqualByComparingTo("32.50");             // 5 x 6,50
        assertThat(linea.proveedorNombre()).isEqualTo("Suministros");
    }

    @Test
    void laReposicionPideAlMenosUnaUnidadSiElMinimoEsCero() {
        Producto p = producto("Rodillo", 0, 0);

        LineaReposicionDto linea = LineaReposicionDto.de(p);

        assertThat(linea.sugerido()).isEqualTo(1);
        assertThat(linea.proveedorNombre()).isNull();
    }
}
