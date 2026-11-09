-- Esquema de StockPyme (MySQL 8). Se ejecuta en cada arranque: solo crea lo que falta.
-- Las restricciones CHECK garantizan las reglas de integridad aunque falle la aplicación.

CREATE TABLE IF NOT EXISTS usuario (
    id            BIGINT       AUTO_INCREMENT PRIMARY KEY,
    nombre        VARCHAR(100) NOT NULL,
    email         VARCHAR(150) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    rol           VARCHAR(20)  NOT NULL,
    activo        BOOLEAN      NOT NULL DEFAULT TRUE,
    creado_en     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_usuario_email UNIQUE (email),
    CONSTRAINT ck_usuario_rol CHECK (rol IN ('PROPIETARIO', 'EMPLEADO'))
);

CREATE TABLE IF NOT EXISTS categoria (
    id     BIGINT      AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(80) NOT NULL,
    CONSTRAINT uk_categoria_nombre UNIQUE (nombre)
);

CREATE TABLE IF NOT EXISTS proveedor (
    id       BIGINT       AUTO_INCREMENT PRIMARY KEY,
    nombre   VARCHAR(120) NOT NULL,
    telefono VARCHAR(30),
    email    VARCHAR(150),
    activo   BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS producto (
    id            BIGINT        AUTO_INCREMENT PRIMARY KEY,
    nombre        VARCHAR(150)  NOT NULL,
    codigo_barras VARCHAR(50),
    precio_compra DECIMAL(10,2) NOT NULL,
    precio_venta  DECIMAL(10,2) NOT NULL,
    stock_actual  INT           NOT NULL DEFAULT 0,
    stock_minimo  INT           NOT NULL DEFAULT 0,
    activo        BOOLEAN       NOT NULL DEFAULT TRUE,
    categoria_id  BIGINT        NOT NULL,
    proveedor_id  BIGINT,
    -- UNIQUE admite varios NULL: los productos sin código no chocan entre sí (D8)
    CONSTRAINT uk_producto_codigo UNIQUE (codigo_barras),
    CONSTRAINT fk_producto_categoria FOREIGN KEY (categoria_id) REFERENCES categoria (id),
    CONSTRAINT fk_producto_proveedor FOREIGN KEY (proveedor_id) REFERENCES proveedor (id),
    CONSTRAINT ck_producto_stock CHECK (stock_actual >= 0),
    CONSTRAINT ck_producto_minimo CHECK (stock_minimo >= 0),
    CONSTRAINT ck_producto_precios CHECK (precio_compra >= 0 AND precio_venta >= 0)
);

CREATE TABLE IF NOT EXISTS movimiento (
    id          BIGINT       AUTO_INCREMENT PRIMARY KEY,
    tipo        VARCHAR(10)  NOT NULL,
    cantidad    INT          NOT NULL,
    motivo      VARCHAR(255),
    fecha       TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    producto_id BIGINT       NOT NULL,
    usuario_id  BIGINT       NOT NULL,
    CONSTRAINT fk_movimiento_producto FOREIGN KEY (producto_id) REFERENCES producto (id),
    CONSTRAINT fk_movimiento_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id),
    -- Reglas de integridad 1, 2 y 3 del diseño
    CONSTRAINT ck_movimiento_tipo CHECK (
        (tipo = 'ENTRADA' AND cantidad > 0)
        OR (tipo = 'SALIDA' AND cantidad < 0)
        OR (tipo = 'AJUSTE' AND cantidad <> 0 AND motivo IS NOT NULL AND CHAR_LENGTH(TRIM(motivo)) > 0)
    ),
    INDEX idx_movimiento_producto_fecha (producto_id, fecha),
    INDEX idx_movimiento_fecha (fecha)
);
