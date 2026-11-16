# StockPyme

App de gestión de inventario para pequeños comercios. Proyecto de práctica para el TFG de 2.º de DAM.

Con StockPyme un comercio sabe en todo momento qué tiene, registra cada entrada y salida de mercancía y ve qué productos están a punto de agotarse.

## Tecnologías

| Parte | Tecnología |
|---|---|
| App móvil | Android, Kotlin, Jetpack Compose, MVVM, Retrofit |
| Backend | API REST con Spring Boot 3.5 (Java 21), Spring Security con JWT, JPA |
| Base de datos | MySQL 8.4 en Docker, con restricciones `CHECK` para las reglas de stock |
| Despliegue | Docker Compose (base de datos + API con un solo comando) |

## Novedades de la versión 2.0

- **Modo venta**: carrito con buscador y escáner. Cobrar descuenta todas las líneas en una sola transacción (si una no tiene stock, no se descuenta ninguna).
- **Panel de inicio**: ventas de hoy y de ayer, gráfica de 7, 14 o 30 días, alertas y actividad reciente. El empleado ve solo el número de movimientos, sin importes.
- **Reposición**: lista de la compra por proveedor con cantidades sugeridas y editables, coste estimado y pedido listo para compartir.
- **Exportar a CSV** (inventario y movimientos de 30 días), pensado para abrir en Excel.
- **Tema claro/oscuro**, cambio de contraseña, filtros por categoría y orden en Productos, filtros por tipo en Actividad y gráfica de ventas en la ficha de cada producto.
- Cada movimiento guarda el **precio del momento**, así los informes no cambian si luego cambias los precios.
- Web de presentación rehecha con las novedades, un visor de pantallas, tema oscuro y hoja de ruta.

> **Si actualizas desde la V1:** la API añade sola la columna nueva (`precio_unitario`). Si prefieres empezar con los datos de demostración nuevos (14 días de historial para ver las gráficas), ejecuta `docker compose down -v` y vuelve a levantarlo. Ojo: borra los datos actuales.

## Qué hace

- Inicio de sesión con dos roles: **propietario** y **empleado**.
- Productos con categoría, proveedor, precios, código de barras y stock mínimo.
- Entradas, salidas y ajustes de stock. Cada cambio queda registrado: quién, cuándo y por qué.
- Búsqueda por nombre o **escaneando el código de barras** con la cámara.
- Alertas de productos en el stock mínimo y pestaña de actividad reciente.
- Para el propietario: resumen con el valor del inventario, categorías, proveedores y cuentas de empleados.
- El stock nunca queda negativo, ni siquiera con dos móviles registrando salidas a la vez (bloqueo de fila con `SELECT ... FOR UPDATE`).

## Cómo arrancarlo

### 1. Base de datos y API

Necesitas Docker Desktop abierto. Desde la carpeta del proyecto:

```bash
docker compose up -d --build
```

La primera vez tarda unos minutos. Cuando termine, comprueba que responde en http://localhost:8080/api/salud. La documentación de la API (Swagger) está en http://localhost:8080/swagger-ui.html.

La base de datos se crea sola con datos de ejemplo de una ferretería y estos usuarios de prueba:

| Rol | Email | Contraseña |
|---|---|---|
| Propietario | `admin@stockpyme.local` | `admin1234` |
| Empleado | `empleado@stockpyme.local` | `empleado1234` |

MySQL queda accesible desde el PC en el puerto **3310** (usuario `stockpyme`, contraseña `stockpyme`), por ejemplo para abrirla con MySQL Workbench o DBeaver.

Para pararlo: `docker compose down`. Para borrar también los datos y empezar de cero: `docker compose down -v`.

### 2. App Android

1. En Android Studio, **File → Open** y elige la carpeta `android`.
2. Espera a que termine la sincronización de Gradle.
3. Pulsa **Run** con el emulador o con tu móvil conectado.

**En el emulador** la dirección del servidor ya viene puesta: `http://10.0.2.2:8080`.

**En un móvil real:**

1. El móvil y el PC tienen que estar en la **misma red WiFi**.
2. En la pantalla de login, cambia la dirección del servidor a la IP de tu PC, por ejemplo `http://192.168.1.50:8080`. Para ver tu IP, ejecuta `ipconfig` en el PC y busca "Dirección IPv4" en el adaptador WiFi.
3. Pulsa **Probar conexión**. Si falla, revisa que el Firewall de Windows permita conexiones entrantes al puerto 8080.
4. En Android 17 o superior, acepta el permiso de **dispositivos cercanos**. Es el que deja a la app conectar con equipos de tu red local.

### 3. Web de presentación

La carpeta `web` tiene la página para promocionar la app: funciones, capturas reales, precios y preguntas frecuentes. Es HTML y CSS sin dependencias: basta con abrir `web/index.html` en el navegador o subir la carpeta a GitHub Pages, Netlify o cualquier hosting estático.

El formulario de contacto es de demostración y no envía datos a ningún sitio.

### 4. Pruebas automáticas

```bash
cd backend
mvn test
```

Son 20 pruebas: lógica de stock (entradas, salidas, stock insuficiente, ajustes y permisos), ventas con carrito, informes y exportación CSV.

## Estructura

```
backend/        API REST (Spring Boot)
  modelo/         Entidades JPA (tablas)
  repositorio/    Acceso a datos
  servicio/       Reglas de negocio y transacciones
  controlador/    Endpoints REST
  seguridad/      JWT
  resources/schema.sql  Esquema de la base de datos con sus CHECK
android/        App móvil (Kotlin + Compose)
  datos/          Retrofit, sesión y repositorio
  ui/             Pantallas y ViewModels (panel, venta, reposición, productos, actividad, más)
  res/drawable/   Logo e icono adaptativo de la app
web/            Página web de presentación
docs/           Análisis, diseño y contrato de la API
docker-compose.yml
```

## Documentación

1. [Plan del proyecto](docs/00-plan-proyecto.md): fases y entregables
2. [Análisis y requisitos](docs/01-analisis.md): qué hace la app y qué queda fuera
3. [Diseño](docs/02-diseno.md): modelo de datos, casos de uso y arquitectura
4. [Contrato de la API](docs/03-api.md): endpoints, formato de los movimientos y errores

## Estado

Versión 2.0: backend, base de datos y app Android con todos los requisitos de prioridad Alta y Media, y la exportación a CSV (RF-15). Queda fuera, por ser de prioridad Baja, las notificaciones push (RF-14).

Para producción faltaría servir la API por HTTPS (RNF-04) y cambiar las contraseñas y el secreto JWT con un archivo `.env`.
