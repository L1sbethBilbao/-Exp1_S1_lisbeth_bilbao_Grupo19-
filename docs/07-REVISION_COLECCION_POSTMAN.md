# Revisión de la colección Postman — Minimarket API

**Fecha de revisión:** mayo 2026  
**Colección:** [Minimarket.postman_collection.json](Minimarket.postman_collection.json)  
**Guía de uso:** [04-GUIA_PRUEBAS_POSTMAN.md](04-GUIA_PRUEBAS_POSTMAN.md)  
**App:** `http://localhost:8080` (Spring Boot 3.4 + sesión HTTP `JSESSIONID`)

---

## Resumen ejecutivo

La colección Postman está **alineada con la seguridad del backend** y cubre autenticación, RBAC por rol, validaciones y pruebas de IDOR. Las pruebas automáticas JUnit (`SecurityRolesIntegrationTest`) también pasan.

| Área | Veredicto |
|------|-----------|
| Autenticación y sesión | OK |
| RBAC (401 / 403) | OK |
| CRUD productos y categorías | OK |
| Carrito y ventas (IDOR) | OK (ver prueba IDOR en carpeta 09) |
| Inventario y detalle-ventas | OK (EMPLEADO / GERENTE) |
| Usuarios (solo GERENTE) | OK |
| Tests JUnit de roles | OK |

**Puntos de atención:** 3 casos frecuentes en Postman (IDs dinámicos, prueba IDOR mal configurada, DELETE producto con FK). Detalle en [§5](#5-puntos-de-atención-errores-frecuentes-en-postman).

---

## 1. Regla de oro en Postman

Postman guarda **una sola sesión** (`JSESSIONID`). El último login reemplaza al anterior.

| Objetivo | Qué hacer |
|----------|-----------|
| Cambiar de usuario | **Logout** (`POST /logout`) o borrar cookies de `localhost` |
| Probar **401** | Sin cookie activa (borrar cookies antes de la petición) |
| Probar **403** | Login con el rol que **no** debe tener acceso |
| Login exitoso con redirects | Cuerpo final: `¡Hola Mundo!` en `/public/hola` (no JSON ni token) |

**Login:** `POST /login` con body **x-www-form-urlencoded** (`username`, `password`). No usar JSON.

---

## 2. Qué probar con cada usuario

### `cliente1` / `cliente123` (ROLE_CLIENTE)

| Debe funcionar (200) | Debe fallar (403) |
|----------------------|-------------------|
| GET productos, categorías | POST / PUT / DELETE productos y categorías |
| GET / POST / PUT / DELETE **su** carrito | GET inventario, detalle-ventas, usuarios |
| GET / POST **sus** ventas | Acceder al carrito o venta de otro usuario (IDOR) |

### `empleado1` / `empleado123` (ROLE_EMPLEADO)

| Debe funcionar (200) | Debe fallar (403) |
|----------------------|-------------------|
| GET productos, categorías | GET / POST / PUT / DELETE usuarios |
| POST / PUT / DELETE productos y categorías | — |
| GET / POST carrito (todos los usuarios) | — |
| GET / POST ventas (todas) | — |
| CRUD inventario y detalle-ventas | — |

### `gerente1` / `gerente123` (ROLE_GERENTE)

Acceso total a la API, **incluido** `/api/usuarios`. Es el único rol que puede gestionar usuarios.

---

## 3. Revisión carpeta por carpeta

### 00 — Autenticación

| Request | Respuesta esperada | Estado |
|---------|-------------------|--------|
| GET Login page (HTML) | 200 | OK |
| Login CLIENTE / EMPLEADO / GERENTE | 302 + cookie `JSESSIONID` | OK |
| Login credenciales inválidas | 302 → `/login?error` o 200 HTML con error | OK |
| Bloqueo 3 intentos (anti fuerza bruta) | 4.º intento: cuenta bloqueada | OK — reiniciar app para desbloquear |
| Logout | 302 → `/public/hola` | OK |

### 01 — Público

| Request | Respuesta esperada | Estado |
|---------|-------------------|--------|
| GET `/public/hola` | 200 — texto `¡Hola Mundo!` | OK |

### 02 — Productos

| Request | Rol | Esperado | Notas |
|---------|-----|----------|-------|
| GET listar / por ID | Autenticado | 200 | OK |
| POST crear | EMPLEADO / GERENTE | 200 | OK |
| POST crear | CLIENTE | 403 | Carpeta 09 |
| PUT actualizar | EMPLEADO / GERENTE | 200 | OK |
| DELETE | EMPLEADO / GERENTE | 204 | OK si el producto no está en carrito |
| DELETE producto id **1** | GERENTE | **500** | FK en carrito/ventas (comportamiento conocido) |
| POST precio negativo | EMPLEADO / GERENTE | 400 | Validación `@Valid` OK |

### 03 — Categorías

| Request | Rol | Esperado | Notas |
|---------|-----|----------|-------|
| GET listar / por ID | Autenticado | 200 | OK |
| POST / PUT / DELETE | EMPLEADO / GERENTE | 200 / 204 | OK |
| POST | CLIENTE | 403 | — |
| DELETE categoría con productos (ej. id 1) | EMPLEADO / GERENTE | **400** | Mensaje claro (corregido en backend) |
| DELETE categoría vacía | EMPLEADO / GERENTE | 204 | OK |
| POST nombre duplicado (ej. "Lacteos") | EMPLEADO / GERENTE | 500 | Restricción única en BD |

### 04 — Carrito

| Request | Rol | Esperado | Notas |
|---------|-----|----------|-------|
| GET listar | CLIENTE | 200 — solo sus ítems | Seed: 2 ítems |
| GET listar | EMPLEADO / GERENTE | 200 — todos | OK |
| GET / PUT / DELETE por ID | CLIENTE | 200 propio / **403** ajeno | IDOR en servicio |
| POST agregar | CLIENTE | 200 | Usuario forzado al logueado |
| POST agregar | EMPLEADO | 200 | Body puede usar `usuario.id` de otro |

**Nota:** el body de POST usa `"producto": { "id": 3 }`. Si el producto 3 fue eliminado en pruebas previas → **500** (FK). Tras reiniciar la app el id 3 vuelve (seed). Usar id **1** o **2** si hace falta.

**Nota:** si se borró un ítem del carrito, actualizar variable `carritoId` o aparecerá **404**.

### 05 — Ventas

| Request | Rol | Esperado | Notas |
|---------|-----|----------|-------|
| GET listar | CLIENTE | 200 — solo sus ventas | IDOR en servicio |
| GET listar | EMPLEADO / GERENTE | 200 — todas | OK |
| GET por ID | CLIENTE | 200 propia / **403** ajena | — |
| POST simple / con detalles | Autenticado | 200 | CLIENTE: usuario forzado al logueado |

### 06 — Inventario

| Request | Rol | Esperado |
|---------|-----|----------|
| GET / POST / PUT / DELETE | EMPLEADO / GERENTE | 200 / 204 |
| GET | CLIENTE | **403** |

### 07 — Detalle ventas

| Request | Rol | Esperado |
|---------|-----|----------|
| GET / POST / PUT / DELETE | EMPLEADO / GERENTE | 200 / 204 |
| GET | CLIENTE | **403** |

### 08 — Usuarios

| Request | Rol | Esperado | Notas |
|---------|-----|----------|-------|
| GET / POST / PUT / DELETE | GERENTE | 200 / 204 | OK |
| GET | CLIENTE / EMPLEADO | **403** | — |
| GET listar | GERENTE | 200 | JSON **sin** campo `password` |

### 09 — Pruebas de seguridad

| Request | Login previo | Esperado | Estado |
|---------|--------------|----------|--------|
| 401 — Productos sin autenticación | Ninguno (sin cookie) | **401** | OK |
| 403 — CLIENTE accede usuarios | cliente1 | **403** | OK |
| 403 — CLIENTE accede inventario | cliente1 | **403** | OK |
| 403 — CLIENTE crea producto | cliente1 | **403** | OK |
| 403 — CLIENTE carrito de otro (IDOR) | cliente1 | **403** | **Ver §5.1** — URL actual puede dar 200 |
| 200 — EMPLEADO accede inventario | empleado1 | **200** | OK |
| 200 — GERENTE accede usuarios | gerente1 | **200** | OK |

---

## 4. Matriz rápida por recurso

| Recurso | CLIENTE | EMPLEADO | GERENTE |
|---------|---------|----------|---------|
| `/public/**`, `/login` | Público | Público | Público |
| GET productos / categorías | Sí | Sí | Sí |
| POST/PUT/DELETE productos / categorías | No (403) | Sí | Sí |
| Carrito / ventas | Sí (solo los suyos) | Todos | Todos |
| Inventario, detalle-ventas | No (403) | Sí | Sí |
| Usuarios | No (403) | No (403) | Sí |

---

## 5. Puntos de atención (errores frecuentes en Postman)

### 5.1 Prueba IDOR del carrito (carpeta 09)

La request **403 — CLIENTE carrito de otro (IDOR)** usa `GET /api/carrito/2`.

En el **seed inicial**, los carritos 1 y 2 pertenecen a **cliente1**. Por eso esa petición devuelve **200**, no 403.

**Procedimiento correcto para capturar 403:**

1. Login como **empleado1**
2. `POST /api/carrito` con body:
   ```json
   {
     "usuario": { "id": 2 },
     "producto": { "id": 1 },
     "cantidad": 1
   }
   ```
3. Anotar el `id` devuelto (ej. `4`)
4. Logout → Login como **cliente1**
5. `GET /api/carrito/4` → debe responder **403** con mensaje: `No puede acceder al carrito de otro usuario`

### 5.2 Producto id 3 en POST carrito

Si en pruebas anteriores se eliminó el producto 3 (Jugo natural), el POST carrito con `"producto": { "id": 3 }` responde **500** (violación FK).

**Solución:** reiniciar la aplicación (H2 en memoria recarga el seed) o cambiar a `"producto": { "id": 1 }` o `{ "id": 2 }`.

### 5.3 DELETE producto id 1

El producto 1 (Agua) está referenciado en carrito y ventas del seed. `DELETE /api/productos/1` responde **500**, no 204.

**Solución:** crear un producto con POST y eliminar ese id nuevo, o documentar el 500 como restricción de integridad referencial.

### 5.4 Prueba 401 que devuelve 200

Si `GET /api/productos` sin login devuelve **200**, Postman aún tiene la cookie `JSESSIONID`. Borrar cookies de `localhost` o ejecutar Logout antes.

### 5.5 Una sola sesión

No se pueden tener cliente1, empleado1 y gerente1 autenticados a la vez en la misma instancia de Postman. Siempre logout o borrar cookies entre roles.

---

## 6. Orden recomendado para capturas (entrega AVA)

```
1. Sin login     → 401 productos (carpeta 09)
2. Login cliente1 → 02 GET productos, 04 carrito, 05 ventas
3. Mismo login   → 09: 403 usuarios, inventario, crear producto
4. Logout        → Login empleado1 → 02 POST/PUT, 06 inventario, 09 "200 EMPLEADO inventario"
5. Logout        → Login gerente1 → 08 usuarios, 09 "200 GERENTE usuarios"
6. IDOR carrito  → pasos §5.1 → captura 403
7. Login inválido + bloqueo 3 intentos (reiniciar app si la cuenta ya estaba bloqueada)
```

---

## 7. Datos iniciales (seed H2)

Tras `spring-boot:run` con BD vacía:

| Entidad | IDs típicos |
|---------|-------------|
| Categorías | 1 = Bebidas, 2 = Snacks |
| Productos | 1 = Agua 500ml, 2 = Papas fritas, 3 = Jugo natural |
| Usuario cliente1 | id = 1 |
| Carrito cliente1 | ids 1 y 2 |
| Venta cliente1 | id = 1 |
| Inventario | ids 1 y 2 |

Los IDs pueden cambiar si se crean o eliminan registros durante las pruebas. Usar variables de colección (`productoId`, `carritoId`, etc.) y actualizarlas según la respuesta de POST.

---

## 8. Verificación técnica realizada

- Revisión de `SecurityConfig.java` y servicios con IDOR (`CarritoServiceImpl`, `VentaServiceImpl`)
- Pruebas HTTP contra app en ejecución (`localhost:8080`)
- `.\mvnw.cmd test` — `SecurityRolesIntegrationTest` en verde

---

## 9. Pendientes opcionales (mejora futura)

| Mejora | Descripción |
|--------|-------------|
| Colección Postman | Corregir request IDOR (carrito id dinámico o pasos en descripción) |
| Colección Postman | Nota en POST carrito sobre `producto.id` 3 |
| Backend | Devolver 400 en DELETE producto con FK en lugar de 500 |
| Colección | Duplicar request de bloqueo para empleado1 y gerente1 (si se requiere evidencia por rol) |

---

## Referencias

- [04-GUIA_PRUEBAS_POSTMAN.md](04-GUIA_PRUEBAS_POSTMAN.md) — lista detallada de URLs y bodies
- [03-ESTADO_ACTUAL_SEGURIDAD.md](03-ESTADO_ACTUAL_SEGURIDAD.md) — estado de seguridad y OWASP
- [06-INVENTARIO_ENDPOINTS.md](06-INVENTARIO_ENDPOINTS.md) — tabla de endpoints
