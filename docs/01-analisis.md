# Fase 1: Análisis y requisitos (borrador)

## 1. El problema

Muchos comercios pequeños (ferreterías, tiendas de barrio, papelerías) controlan el stock con papel, con hojas de cálculo o de memoria. Esto provoca:

- Roturas de stock que se descubren cuando un cliente pide el producto.
- Compras de más, por no saber qué hay realmente en la tienda.
- Falta de datos sobre el valor del inventario o sobre qué productos se mueven más.

Los programas de gestión completos (ERP) suelen ser demasiado caros y complejos para estos negocios.

## 2. El objetivo

Una app móvil sencilla con la que un comercio pueda saber en todo momento qué tiene, registrar cada entrada y salida de mercancía, y recibir un aviso antes de quedarse sin un producto.

## 3. Usuarios

| Rol | Quién es | Qué puede hacer |
|---|---|---|
| **Propietario** | Dueño o encargado del comercio | Todo: productos, proveedores, empleados e informes |
| **Empleado** | Dependiente | Consultar productos y registrar movimientos de stock |

## 4. Requisitos funcionales

Prioridad: **Alta** = imprescindible para el mínimo viable, **Media** = se hace después, si hay tiempo, **Baja** = extra.

| ID | Requisito | Rol | Prioridad |
|---|---|---|---|
| RF-01 | Iniciar y cerrar sesión | Todos | Alta |
| RF-02 | Crear, editar y desactivar productos (nombre, código de barras, precios, stock mínimo) | Propietario | Alta |
| RF-03 | Consultar y buscar productos por nombre o código | Todos | Alta |
| RF-04 | Organizar los productos por categorías | Propietario | Alta |
| RF-05 | Registrar entradas de stock (compras a proveedor) | Todos | Alta |
| RF-06 | Registrar salidas de stock (ventas, mermas) | Todos | Alta |
| RF-07 | Registrar ajustes con un motivo obligatorio (recuento, corrección) | Propietario | Alta |
| RF-08 | Ver el historial de movimientos de cada producto | Todos | Alta |
| RF-09 | Ver la lista de productos por debajo del stock mínimo | Todos | Alta |
| RF-10 | Gestionar proveedores y asociarlos a los productos | Propietario | Media |
| RF-11 | Gestionar las cuentas de los empleados | Propietario | Media |
| RF-12 | Buscar un producto escaneando su código de barras con la cámara | Todos | Media |
| RF-13 | Ver informes: valor del inventario y productos con más movimiento | Propietario | Media |
| RF-14 | Recibir una notificación push cuando un producto baje del mínimo | Propietario | Baja |
| RF-15 | Exportar el inventario a CSV | Propietario | Baja |

## 5. Requisitos no funcionales

| ID | Requisito |
|---|---|
| RNF-01 | **Seguridad:** contraseñas guardadas con hash (BCrypt), autenticación con JWT y permisos comprobados en el servidor |
| RNF-02 | **Integridad:** el stock nunca queda negativo y cada cambio deja un movimiento registrado |
| RNF-03 | **Compatibilidad:** Android 8.0 (API 26) o superior |
| RNF-04 | **Comunicación:** toda la comunicación con la API en producción va por HTTPS |
| RNF-05 | **Usabilidad:** registrar un movimiento habitual lleva pocos toques, porque se hace con el cliente delante |
| RNF-06 | **Portabilidad:** el backend y la base de datos arrancan con Docker Compose |
| RNF-07 | **Mantenibilidad:** arquitectura por capas y pruebas automáticas de la lógica de stock |
| RNF-08 | **Protección de datos:** solo se guardan los datos personales necesarios de los usuarios (RGPD) |

## 6. Fuera del alcance

Dejarlo por escrito evita que el proyecto crezca sin control, y además es un apartado que se valora en la memoria.

- Punto de venta (TPV), facturación y cobros
- Varias tiendas o almacenes
- Funcionamiento sin conexión
- App para iOS

## 7. Decisiones de análisis

| Decisión | Motivo |
|---|---|
| El stock solo cambia mediante movimientos, nunca se edita directamente | Todo cambio queda registrado: quién, cuándo y por qué. Es la regla central de la app y la mejor para explicar en la defensa |
| Los productos se desactivan en lugar de borrarse | Si se borraran, su historial de movimientos quedaría incompleto |
| Cada instalación da servicio a un solo comercio | Atender a varios comercios en el mismo sistema complica mucho el modelo de datos y no aporta a los objetivos del proyecto |

## 8. Riesgos

| Riesgo | Cómo se reduce |
|---|---|
| Quedarse sin tiempo | Terminar primero todos los requisitos de prioridad Alta |
| Tecnología nueva (Spring Boot, JWT, Retrofit) | Pruebas pequeñas antes de integrarla en el proyecto |
| Dos móviles registran una salida a la vez y el stock queda mal | Resolverlo en el servidor con una transacción. Se diseña en la fase 2 |
| Perder trabajo | Git con copia en un repositorio remoto |

## 9. Preguntas abiertas

1. ¿Conoces un comercio real al que puedas preguntar? Una entrevista corta aporta mucho a la memoria.
2. ¿Es suficiente con estos dos roles?
3. ¿Falta algún requisito importante, o sobra alguno?
4. ¿Te gusta el nombre StockPyme, o prefieres otro?
