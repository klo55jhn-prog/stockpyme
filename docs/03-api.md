# Contrato de la API REST

Base: `http://<servidor>:8080/api`. Todas las rutas, salvo el login y `/salud`, necesitan la cabecera `Authorization: Bearer <token>`.

La documentación interactiva (Swagger) está en `http://localhost:8080/swagger-ui.html` con el backend arrancado.

## Endpoints

| Método | Ruta | Quién | Caso de uso |
|---|---|---|---|
| GET | `/salud` | Público | Comprobar que el servidor responde |
| POST | `/auth/login` | Público | CU-01 Iniciar sesión. Devuelve `{token, usuario}` |
| GET | `/auth/yo` | Todos | Datos del usuario conectado |
| GET | `/productos?buscar=&incluirInactivos=false` | Todos | CU-05 Buscar por nombre o código |
| GET | `/productos/bajo-minimo` | Todos | CU-11 Productos en el stock mínimo o por debajo |
| GET | `/productos/{id}` | Todos | Ficha de un producto |
| GET | `/productos/codigo/{codigo}` | Todos | RF-12 Buscar por código de barras escaneado |
| POST | `/productos` | Propietario | CU-03 Crear producto (nace con stock 0) |
| PUT | `/productos/{id}` | Propietario | CU-04 Editar o desactivar (`activo: false`) |
| GET | `/productos/{id}/movimientos` | Todos | CU-10 Historial del producto |
| POST | `/productos/{id}/movimientos` | Todos (AJUSTE solo propietario) | CU-07, CU-08, CU-09 |
| GET | `/movimientos/recientes?limite=50` | Todos | Actividad reciente del comercio |
| GET, POST | `/categorias` | GET todos, POST propietario | CU-06 |
| PUT, DELETE | `/categorias/{id}` | Propietario | CU-06. No se borra si tiene productos |
| GET, POST | `/proveedores` | GET todos, POST propietario | CU-12 |
| PUT | `/proveedores/{id}` | Propietario | CU-12. Se desactivan, no se borran |
| GET, POST | `/usuarios` | Propietario | CU-13 |
| PUT | `/usuarios/{id}` | Propietario | CU-13. Nadie puede quitarse a sí mismo el rol de propietario |
| GET | `/informes/resumen` | Propietario | RF-13 Valor del inventario y productos más movidos |

## Registrar un movimiento

```json
POST /api/productos/1/movimientos
{ "tipo": "SALIDA", "cantidad": 2, "motivo": "Venta" }
```

- **ENTRADA y SALIDA:** `cantidad` es el número de unidades, siempre positivo. El servidor guarda la salida con signo negativo (D1).
- **AJUSTE:** se envía `stockNuevo` (el stock real contado) y un `motivo` obligatorio. El servidor calcula la diferencia con el stock bloqueado, así no hay errores si otra persona cambia el stock a la vez.

La respuesta es el producto con el stock ya actualizado.

## Errores

Todos los errores tienen el mismo formato, con un mensaje que la app enseña tal cual:

```json
{ "estado": 409, "mensaje": "Stock insuficiente: quedan 3 unidades" }
```

| Código | Cuándo |
|---|---|
| 400 | Datos no válidos (cantidad 0, precio negativo, ajuste sin motivo...) |
| 401 | Sin token, token caducado o usuario desactivado. La app vuelve al login |
| 403 | El rol no tiene permiso (por ejemplo, un empleado que intenta un ajuste) |
| 404 | El recurso no existe |
| 409 | Conflicto con los datos: stock insuficiente, código de barras repetido, categoría con productos |
