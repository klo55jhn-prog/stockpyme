package com.stockpyme.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Pequeña migración para las bases de datos creadas con la V1.
 *
 * <p>schema.sql solo crea tablas que faltan: no añade columnas a una tabla que ya existe.
 * Esta clase añade a mano la columna {@code precio_unitario} de {@code movimiento} (V2) si no está,
 * para que quien actualice no tenga que borrar sus datos. En una base de datos nueva no hace nada.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MigracionEsquema implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(MigracionEsquema.class);

    private final JdbcTemplate jdbc;

    public MigracionEsquema(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(String... args) {
        Integer existe = jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = 'movimiento' AND column_name = 'precio_unitario'
                """, Integer.class);
        if (existe != null && existe == 0) {
            jdbc.execute("ALTER TABLE movimiento ADD COLUMN precio_unitario DECIMAL(10,2) NULL");
            log.info("Migración V2: añadida la columna movimiento.precio_unitario");
        }
    }
}
