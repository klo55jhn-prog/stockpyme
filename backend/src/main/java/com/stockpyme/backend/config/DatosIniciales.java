package com.stockpyme.backend.config;

import com.stockpyme.backend.dto.ProductoDto;
import com.stockpyme.backend.dto.ProductoRequest;
import com.stockpyme.backend.modelo.Categoria;
import com.stockpyme.backend.modelo.Movimiento;
import com.stockpyme.backend.modelo.Producto;
import com.stockpyme.backend.modelo.Proveedor;
import com.stockpyme.backend.modelo.Rol;
import com.stockpyme.backend.modelo.TipoMovimiento;
import com.stockpyme.backend.modelo.Usuario;
import com.stockpyme.backend.repositorio.CategoriaRepository;
import com.stockpyme.backend.repositorio.MovimientoRepository;
import com.stockpyme.backend.repositorio.ProductoRepository;
import com.stockpyme.backend.repositorio.ProveedorRepository;
import com.stockpyme.backend.repositorio.UsuarioRepository;
import com.stockpyme.backend.servicio.ProductoService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Si la base de datos está vacía, crea dos usuarios de prueba y el inventario de una ferretería
 * con 14 días de historial (entradas, reposiciones y ventas), para que el panel y las gráficas
 * tengan datos desde el primer arranque.
 *
 * <p>El historial se genera con una semilla fija: siempre sale lo mismo. Cada producto empieza
 * con una entrada, vende un poco cada día y termina con el stock final indicado, así que el stock
 * de cada producto cuadra con la suma de sus movimientos.
 */
@Component
public class DatosIniciales implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatosIniciales.class);
    private static final int DIAS_HISTORIAL = 14;

    private final boolean activado;
    private final ZoneId zona;
    private final UsuarioRepository usuarioRepository;
    private final CategoriaRepository categoriaRepository;
    private final ProveedorRepository proveedorRepository;
    private final ProductoRepository productoRepository;
    private final MovimientoRepository movimientoRepository;
    private final ProductoService productoService;
    private final PasswordEncoder passwordEncoder;
    private final Random azar = new Random(2026);

    private Usuario propietario;
    private Usuario empleado;

    public DatosIniciales(@Value("${stockpyme.datos-demo}") boolean activado,
                          @Value("${stockpyme.zona:Europe/Madrid}") String zona,
                          UsuarioRepository usuarioRepository,
                          CategoriaRepository categoriaRepository,
                          ProveedorRepository proveedorRepository,
                          ProductoRepository productoRepository,
                          MovimientoRepository movimientoRepository,
                          ProductoService productoService,
                          PasswordEncoder passwordEncoder) {
        this.activado = activado;
        this.zona = ZoneId.of(zona);
        this.usuarioRepository = usuarioRepository;
        this.categoriaRepository = categoriaRepository;
        this.proveedorRepository = proveedorRepository;
        this.productoRepository = productoRepository;
        this.movimientoRepository = movimientoRepository;
        this.productoService = productoService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (!activado || usuarioRepository.count() > 0) {
            return;
        }
        log.info("Base de datos vacía: cargando datos de ejemplo con {} días de historial", DIAS_HISTORIAL);

        propietario = usuario("Ana Propietaria", "admin@stockpyme.local", "admin1234", Rol.PROPIETARIO);
        empleado = usuario("Luis Empleado", "empleado@stockpyme.local", "empleado1234", Rol.EMPLEADO);

        Long ferreteria = categoriaRepository.save(new Categoria("Ferretería")).getId();
        Long pintura = categoriaRepository.save(new Categoria("Pintura")).getId();
        Long electricidad = categoriaRepository.save(new Categoria("Electricidad")).getId();
        Long limpieza = categoriaRepository.save(new Categoria("Limpieza")).getId();

        Long levante = proveedor("Suministros Levante S.L.", "960 000 000", "pedidos@suministros.example");
        Long mediterraneo = proveedor("Herramientas del Mediterráneo", "961 111 222", "ventas@hmediterraneo.example");

        //       nombre                              código           compra  venta   mín categoría      proveedor     final venta/día
        producto("Martillo de carpintero 500 g",    "8410000000017", "6.50",  "12.95", 3,  ferreteria,   levante,      0,  1.4);
        producto("Caja de tornillos 4x40 (100 u.)", "8410000000024", "2.10",  "4.50",  10, ferreteria,   levante,      25, 3.0);
        producto("Cinta métrica 5 m",               "8410000000031", "3.20",  "6.99",  4,  ferreteria,   null,         3,  0.9);
        producto("Destornillador plano 6 mm",       "8410000000093", "2.80",  "5.95",  6,  ferreteria,   levante,      14, 1.2);
        producto("Llave inglesa 250 mm",            "8410000000109", "7.40",  "15.50", 3,  ferreteria,   mediterraneo, 9,  0.6);
        producto("Taladro percutor 650 W",          "8410000000116", "38.00", "74.90", 2,  ferreteria,   mediterraneo, 3,  0.3);
        producto("Pintura plástica blanca 4 L",    "8410000000048", "11.00", "21.90", 5,  pintura,      levante,      8,  1.0);
        producto("Rodillo antigoteo 22 cm",         null,            "2.40",  "5.25",  4,  pintura,      null,         2,  0.8);
        producto("Brocha 50 mm",                    "8410000000123", "1.90",  "4.25",  6,  pintura,      levante,      11, 1.1);
        producto("Cinta de carrocero 50 m",         "8410000000130", "1.10",  "2.60",  10, pintura,      levante,      18, 1.6);
        producto("Bombilla LED E27 10 W",           "8410000000062", "1.30",  "3.49",  20, electricidad, levante,      40, 4.0);
        producto("Regleta 4 tomas con interruptor", "8410000000079", "4.80",  "9.95",  5,  electricidad, null,         0,  1.0);
        producto("Cable 2,5 mm² rollo 25 m",        "8410000000147", "19.50", "38.00", 3,  electricidad, mediterraneo, 2,  0.7);
        producto("Lejía 2 L",                       "8410000000086", "0.85",  "1.75",  12, limpieza,     null,         30, 3.5);
        producto("Detergente multiusos 1 L",        "8410000000154", "1.45",  "3.20",  8,  limpieza,     null,         21, 1.8);
        producto("Fregona microfibra",              "8410000000161", "3.10",  "6.90",  5,  limpieza,     levante,      7,  0.9);
    }

    private Usuario usuario(String nombre, String email, String password, Rol rol) {
        Usuario u = new Usuario();
        u.setNombre(nombre);
        u.setEmail(email);
        u.setPasswordHash(passwordEncoder.encode(password));
        u.setRol(rol);
        return usuarioRepository.save(u);
    }

    private Long proveedor(String nombre, String telefono, String email) {
        Proveedor p = new Proveedor();
        p.setNombre(nombre);
        p.setTelefono(telefono);
        p.setEmail(email);
        return proveedorRepository.save(p).getId();
    }

    private void producto(String nombre, String codigo, String compra, String venta, int minimo,
                          Long categoria, Long proveedor, int stockFinal, double ventaMediaDiaria) {
        ProductoDto creado = productoService.crear(new ProductoRequest(
                nombre, codigo, new BigDecimal(compra), new BigDecimal(venta), minimo, categoria, proveedor, null));
        Producto p = productoRepository.findById(creado.id()).orElseThrow();
        generarHistorial(p, stockFinal, ventaMediaDiaria);
    }

    /**
     * Inventa el historial de un producto. El índice 0 es hace 13 días y el último es hoy.
     * Los productos que se venden bien reciben una reposición a mitad del periodo, y nunca
     * se queda el stock en negativo.
     */
    private void generarHistorial(Producto p, int stockFinal, double media) {
        int[] ventas = new int[DIAS_HISTORIAL];
        int vendidas = 0;
        int primeraMitad = 0;
        int mitad = DIAS_HISTORIAL / 2;
        for (int i = 0; i < DIAS_HISTORIAL; i++) {
            ventas[i] = (int) Math.round(azar.nextDouble() * 2 * media);
            vendidas += ventas[i];
            if (i < mitad) {
                primeraMitad += ventas[i];
            }
        }

        int totalEntradas = stockFinal + vendidas;
        int inicial = totalEntradas;
        int reposicion = 0;
        if (media >= 1) {
            inicial = Math.min(totalEntradas, primeraMitad + p.getStockMinimo() + 2);
            reposicion = totalEntradas - inicial;
        }

        if (inicial > 0) {
            guardar(p, TipoMovimiento.ENTRADA, inicial, "Stock inicial", propietario,
                    momento(DIAS_HISTORIAL, 9), p.getPrecioCompra());
        }
        if (reposicion > 0) {
            guardar(p, TipoMovimiento.ENTRADA, reposicion, "Reposición de proveedor", propietario,
                    momento(DIAS_HISTORIAL - 1 - mitad, 9), p.getPrecioCompra());
        }
        for (int i = 0; i < DIAS_HISTORIAL; i++) {
            if (ventas[i] > 0) {
                Usuario quien = azar.nextBoolean() ? propietario : empleado;
                guardar(p, TipoMovimiento.SALIDA, -ventas[i], "Venta", quien,
                        momento(DIAS_HISTORIAL - 1 - i, 10 + azar.nextInt(10)), p.getPrecioVenta());
            }
        }

        p.setStockActual(stockFinal);
        productoRepository.save(p);
    }

    private void guardar(Producto p, TipoMovimiento tipo, int cantidad, String motivo, Usuario quien,
                         Instant fecha, BigDecimal precio) {
        movimientoRepository.save(new Movimiento(tipo, cantidad, motivo, p, quien, fecha, precio));
    }

    /** Un instante de hace {@code diasAtras} días a la hora indicada. Nunca en el futuro. */
    private Instant momento(int diasAtras, int hora) {
        Instant t = LocalDate.now(zona).minusDays(diasAtras).atTime(hora, azar.nextInt(60)).atZone(zona).toInstant();
        return t.isAfter(Instant.now()) ? Instant.now().minusSeconds(60L * (1 + azar.nextInt(120))) : t;
    }
}
