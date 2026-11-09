package com.stockpyme.backend.config;

import com.stockpyme.backend.dto.MovimientoRequest;
import com.stockpyme.backend.dto.ProductoDto;
import com.stockpyme.backend.dto.ProductoRequest;
import com.stockpyme.backend.modelo.Categoria;
import com.stockpyme.backend.modelo.Proveedor;
import com.stockpyme.backend.modelo.Rol;
import com.stockpyme.backend.modelo.TipoMovimiento;
import com.stockpyme.backend.modelo.Usuario;
import com.stockpyme.backend.repositorio.CategoriaRepository;
import com.stockpyme.backend.repositorio.ProveedorRepository;
import com.stockpyme.backend.repositorio.UsuarioRepository;
import com.stockpyme.backend.seguridad.UsuarioAutenticado;
import com.stockpyme.backend.servicio.MovimientoService;
import com.stockpyme.backend.servicio.ProductoService;
import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Si la base de datos está vacía, crea dos usuarios de prueba y un pequeño
 * inventario de ferretería para poder probar la app nada más arrancarla.
 * El stock inicial entra con movimientos ENTRADA, igual que en el uso real.
 */
@Component
public class DatosIniciales implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatosIniciales.class);

    private final boolean activado;
    private final UsuarioRepository usuarioRepository;
    private final CategoriaRepository categoriaRepository;
    private final ProveedorRepository proveedorRepository;
    private final ProductoService productoService;
    private final MovimientoService movimientoService;
    private final PasswordEncoder passwordEncoder;

    public DatosIniciales(@Value("${stockpyme.datos-demo}") boolean activado,
                          UsuarioRepository usuarioRepository,
                          CategoriaRepository categoriaRepository,
                          ProveedorRepository proveedorRepository,
                          ProductoService productoService,
                          MovimientoService movimientoService,
                          PasswordEncoder passwordEncoder) {
        this.activado = activado;
        this.usuarioRepository = usuarioRepository;
        this.categoriaRepository = categoriaRepository;
        this.proveedorRepository = proveedorRepository;
        this.productoService = productoService;
        this.movimientoService = movimientoService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (!activado || usuarioRepository.count() > 0) {
            return;
        }
        log.info("Base de datos vacía: cargando datos de ejemplo");

        Usuario propietario = usuario("Ana Propietaria", "admin@stockpyme.local", "admin1234", Rol.PROPIETARIO);
        usuario("Luis Empleado", "empleado@stockpyme.local", "empleado1234", Rol.EMPLEADO);
        var quien = new UsuarioAutenticado(propietario.getId(), propietario.getNombre(), propietario.getRol());

        Long ferreteria = categoriaRepository.save(new Categoria("Ferretería")).getId();
        Long pintura = categoriaRepository.save(new Categoria("Pintura")).getId();
        Long electricidad = categoriaRepository.save(new Categoria("Electricidad")).getId();
        Long limpieza = categoriaRepository.save(new Categoria("Limpieza")).getId();

        Proveedor suministros = new Proveedor();
        suministros.setNombre("Suministros Levante S.L.");
        suministros.setTelefono("960 000 000");
        suministros.setEmail("pedidos@suministros.example");
        Long proveedor = proveedorRepository.save(suministros).getId();

        producto("Martillo de carpintero 500 g", "8410000000017", "6.50", "12.95", 3, ferreteria, proveedor, 12, quien);
        producto("Caja de tornillos 4x40 (100 u.)", "8410000000024", "2.10", "4.50", 10, ferreteria, proveedor, 25, quien);
        producto("Cinta métrica 5 m", "8410000000031", "3.20", "6.99", 4, ferreteria, null, 3, quien);
        producto("Pintura plástica blanca 4 L", "8410000000048", "11.00", "21.90", 5, pintura, proveedor, 8, quien);
        producto("Rodillo antigoteo 22 cm", null, "2.40", "5.25", 4, pintura, null, 2, quien);
        producto("Bombilla LED E27 10 W", "8410000000062", "1.30", "3.49", 20, electricidad, proveedor, 40, quien);
        producto("Regleta 4 tomas con interruptor", "8410000000079", "4.80", "9.95", 5, electricidad, null, 0, quien);
        producto("Lejía 2 L", "8410000000086", "0.85", "1.75", 12, limpieza, null, 30, quien);
    }

    private Usuario usuario(String nombre, String email, String password, Rol rol) {
        Usuario u = new Usuario();
        u.setNombre(nombre);
        u.setEmail(email);
        u.setPasswordHash(passwordEncoder.encode(password));
        u.setRol(rol);
        return usuarioRepository.save(u);
    }

    private void producto(String nombre, String codigo, String compra, String venta, int minimo,
                          Long categoria, Long proveedor, int stockInicial, UsuarioAutenticado quien) {
        ProductoDto creado = productoService.crear(new ProductoRequest(
                nombre, codigo, new BigDecimal(compra), new BigDecimal(venta), minimo, categoria, proveedor, null));
        if (stockInicial > 0) {
            movimientoService.registrar(creado.id(),
                    new MovimientoRequest(TipoMovimiento.ENTRADA, stockInicial, null, "Stock inicial"), quien);
        }
    }
}
