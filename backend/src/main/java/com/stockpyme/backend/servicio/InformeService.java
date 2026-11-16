package com.stockpyme.backend.servicio;

import com.stockpyme.backend.dto.ActividadDto;
import com.stockpyme.backend.dto.ResumenDto;
import com.stockpyme.backend.modelo.Movimiento;
import com.stockpyme.backend.modelo.Producto;
import com.stockpyme.backend.modelo.TipoMovimiento;
import com.stockpyme.backend.repositorio.MovimientoRepository;
import com.stockpyme.backend.repositorio.ProductoRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Informes para el propietario: resumen (RF-13), actividad por día y exportación a CSV (RF-15). */
@Service
public class InformeService {

    private static final int MAXIMO_DIAS = 90;
    private static final String SALTO = "\r\n";
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ProductoRepository productoRepository;
    private final MovimientoRepository movimientoRepository;
    private final ZoneId zona;

    public InformeService(ProductoRepository productoRepository,
                          MovimientoRepository movimientoRepository,
                          @Value("${stockpyme.zona:Europe/Madrid}") String zona) {
        this.productoRepository = productoRepository;
        this.movimientoRepository = movimientoRepository;
        this.zona = ZoneId.of(zona);
    }

    @Transactional(readOnly = true)
    public ResumenDto resumen() {
        List<Producto> activos = productoRepository.findByActivoTrue();
        long unidades = 0;
        int bajoMinimo = 0;
        BigDecimal valorCompra = BigDecimal.ZERO;
        BigDecimal valorVenta = BigDecimal.ZERO;
        for (Producto p : activos) {
            BigDecimal stock = BigDecimal.valueOf(p.getStockActual());
            unidades += p.getStockActual();
            valorCompra = valorCompra.add(p.getPrecioCompra().multiply(stock));
            valorVenta = valorVenta.add(p.getPrecioVenta().multiply(stock));
            if (p.isBajoMinimo()) {
                bajoMinimo++;
            }
        }

        Instant hace30Dias = Instant.now().minus(Duration.ofDays(30));
        List<ResumenDto.ProductoMovido> masMovidos = movimientoRepository
                .masMovidosDesde(hace30Dias, PageRequest.of(0, 5)).stream()
                .map(fila -> new ResumenDto.ProductoMovido(
                        (Long) fila[0], (String) fila[1], ((Number) fila[2]).longValue()))
                .toList();

        return new ResumenDto(activos.size(), unidades, valorCompra, valorVenta, bajoMinimo, masMovidos);
    }

    /**
     * Entradas, salidas y ventas de cada uno de los últimos {@code dias} días (el último es hoy).
     * Los días sin movimientos salen con ceros, para que la gráfica no tenga huecos.
     */
    @Transactional(readOnly = true)
    public ActividadDto actividad(int dias) {
        int n = Math.max(1, Math.min(dias, MAXIMO_DIAS));
        LocalDate hoy = LocalDate.now(zona);
        LocalDate primero = hoy.minusDays(n - 1L);

        Map<LocalDate, long[]> unidades = new TreeMap<>();   // [entradas, salidas]
        Map<LocalDate, BigDecimal> ventas = new TreeMap<>();
        for (int i = 0; i < n; i++) {
            LocalDate dia = primero.plusDays(i);
            unidades.put(dia, new long[2]);
            ventas.put(dia, BigDecimal.ZERO);
        }

        long unidadesVendidas = 0;
        long movimientosHoy = 0;
        for (Movimiento m : movimientoRepository.desde(primero.atStartOfDay(zona).toInstant())) {
            LocalDate dia = m.getFecha().atZone(zona).toLocalDate();
            if (!unidades.containsKey(dia)) {
                continue;
            }
            if (dia.equals(hoy)) {
                movimientosHoy++;
            }
            if (m.getTipo() == TipoMovimiento.ENTRADA) {
                unidades.get(dia)[0] += m.getCantidad();
            } else if (m.getTipo() == TipoMovimiento.SALIDA) {
                long salidas = -(long) m.getCantidad();
                unidades.get(dia)[1] += salidas;
                if (esVenta(m)) {
                    unidadesVendidas += salidas;
                    ventas.merge(dia, importe(m, salidas), BigDecimal::add);
                }
            }
        }

        List<ActividadDto.Dia> lista = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (var entrada : unidades.entrySet()) {
            BigDecimal ventaDia = ventas.get(entrada.getKey());
            total = total.add(ventaDia);
            lista.add(new ActividadDto.Dia(entrada.getKey(), entrada.getValue()[0], entrada.getValue()[1], ventaDia));
        }
        return new ActividadDto(lista, ventas.get(hoy), total, unidadesVendidas, movimientosHoy);
    }

    /** Una salida sin motivo, o con el motivo "Venta", es una venta. Una merma o una rotura, no. */
    private static boolean esVenta(Movimiento m) {
        return m.getMotivo() == null || m.getMotivo().trim().equalsIgnoreCase("venta");
    }

    /** Precio de ese momento; en los movimientos de la V1 (sin precio guardado), el precio actual. */
    private static BigDecimal importe(Movimiento m, long unidades) {
        BigDecimal precio = m.getPrecioUnitario() != null ? m.getPrecioUnitario() : m.getProducto().getPrecioVenta();
        return precio.multiply(BigDecimal.valueOf(unidades));
    }

    // ---------- CSV (RF-15) ----------

    /** Inventario completo. Separador ";" y coma decimal, para que Excel en español lo abra bien. */
    @Transactional(readOnly = true)
    public String inventarioCsv() {
        StringBuilder csv = new StringBuilder("﻿");
        csv.append("Código;Producto;Categoría;Proveedor;Stock;Mínimo;Precio compra;Precio venta;")
                .append("Valor a coste;Valor a venta;Estado").append(SALTO);
        for (Producto p : productoRepository.buscar("", true)) {
            BigDecimal stock = BigDecimal.valueOf(p.getStockActual());
            csv.append(celda(p.getCodigoBarras())).append(';')
                    .append(celda(p.getNombre())).append(';')
                    .append(celda(p.getCategoria().getNombre())).append(';')
                    .append(celda(p.getProveedor() == null ? null : p.getProveedor().getNombre())).append(';')
                    .append(p.getStockActual()).append(';')
                    .append(p.getStockMinimo()).append(';')
                    .append(decimal(p.getPrecioCompra())).append(';')
                    .append(decimal(p.getPrecioVenta())).append(';')
                    .append(decimal(p.getPrecioCompra().multiply(stock))).append(';')
                    .append(decimal(p.getPrecioVenta().multiply(stock))).append(';')
                    .append(estado(p)).append(SALTO);
        }
        return csv.toString();
    }

    /** Movimientos de los últimos {@code dias} días, del más reciente al más antiguo. */
    @Transactional(readOnly = true)
    public String movimientosCsv(int dias) {
        int n = Math.max(1, Math.min(dias, 365));
        Instant desde = LocalDate.now(zona).minusDays(n - 1L).atStartOfDay(zona).toInstant();
        StringBuilder csv = new StringBuilder("﻿");
        csv.append("Fecha;Tipo;Producto;Cantidad;Precio unitario;Motivo;Usuario").append(SALTO);
        for (Movimiento m : movimientoRepository.desde(desde)) {
            csv.append(m.getFecha().atZone(zona).format(FECHA_HORA)).append(';')
                    .append(m.getTipo()).append(';')
                    .append(celda(m.getProducto().getNombre())).append(';')
                    .append(m.getCantidad()).append(';')
                    .append(m.getPrecioUnitario() == null ? "" : decimal(m.getPrecioUnitario())).append(';')
                    .append(celda(m.getMotivo())).append(';')
                    .append(celda(m.getUsuario().getNombre())).append(SALTO);
        }
        return csv.toString();
    }

    private static String estado(Producto p) {
        if (!p.isActivo()) {
            return "Desactivado";
        }
        if (p.getStockActual() == 0) {
            return "Agotado";
        }
        return p.isBajoMinimo() ? "Stock bajo" : "OK";
    }

    private static String decimal(BigDecimal valor) {
        return valor.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString().replace('.', ',');
    }

    /**
     * Texto seguro para una celda CSV. Si empieza por = + - @ Excel lo interpretaría como una fórmula
     * (inyección CSV), así que se le antepone un apóstrofo. Las comillas y los ";" se escapan.
     */
    static String celda(String valor) {
        if (valor == null) {
            return "";
        }
        String v = valor;
        if (!v.isEmpty() && "=+-@\t\r".indexOf(v.charAt(0)) >= 0) {
            v = "'" + v;
        }
        if (v.contains(";") || v.contains("\"") || v.contains("\n") || v.contains("\r")) {
            v = "\"" + v.replace("\"", "\"\"") + "\"";
        }
        return v;
    }
}
