# Inventario de endpoints API — Minimarket

**Base URL:** `http://localhost:8080`  
**Autenticacion:** Form Login (`POST /login`) + cookie `JSESSIONID`

---

## Resumen por recurso

| Recurso | Base path | Metodos | Roles |
|---------|-----------|---------|-------|
| Publico | `/public/hola` | GET | Sin login |
| Auth | `/login`, `/logout` | POST, GET | Login publico |
| Productos | `/api/productos` | GET, POST, PUT, DELETE | GET: todos / Write: EMPLEADO, GERENTE |
| Categorias | `/api/categorias` | GET, POST, PUT, DELETE | GET: todos / Write: EMPLEADO, GERENTE |
| Carrito | `/api/carrito` | GET, POST, PUT, DELETE | CLIENTE*, EMPLEADO, GERENTE |
| Ventas | `/api/ventas` | GET, POST | CLIENTE*, EMPLEADO, GERENTE |
| Inventario | `/api/inventario` | GET, POST, PUT, DELETE | EMPLEADO, GERENTE |
| Detalle ventas | `/api/detalle-ventas` | GET, POST, PUT, DELETE | EMPLEADO, GERENTE |
| Usuarios | `/api/usuarios` | GET, POST, PUT, DELETE | GERENTE |
| H2 Console | `/h2-console/**` | GET | GERENTE |

\* CLIENTE: solo sus propios registros (carrito/ventas).

---

## Detalle completo

### Publico

| Metodo | URL | Auth | Respuesta esperada |
|--------|-----|------|-------------------|
| GET | `/public/hola` | No | 200 |

### Autenticacion (Spring Security)

| Metodo | URL | Auth | Body |
|--------|-----|------|------|
| GET | `/login` | No | Formulario HTML |
| POST | `/login` | No | `username`, `password` (form-urlencoded) |
| POST | `/logout` | Si | — |

### Productos

| Metodo | URL | Roles |
|--------|-----|-------|
| GET | `/api/productos` | CLIENTE, EMPLEADO, GERENTE |
| GET | `/api/productos/{id}` | CLIENTE, EMPLEADO, GERENTE |
| POST | `/api/productos` | EMPLEADO, GERENTE |
| PUT | `/api/productos/{id}` | EMPLEADO, GERENTE |
| DELETE | `/api/productos/{id}` | EMPLEADO, GERENTE |

### Categorias

| Metodo | URL | Roles |
|--------|-----|-------|
| GET | `/api/categorias` | CLIENTE, EMPLEADO, GERENTE |
| GET | `/api/categorias/{id}` | CLIENTE, EMPLEADO, GERENTE |
| POST | `/api/categorias` | EMPLEADO, GERENTE |
| PUT | `/api/categorias/{id}` | EMPLEADO, GERENTE |
| DELETE | `/api/categorias/{id}` | EMPLEADO, GERENTE |

### Carrito

| Metodo | URL | Roles |
|--------|-----|-------|
| GET | `/api/carrito` | CLIENTE*, EMPLEADO, GERENTE |
| GET | `/api/carrito/{id}` | CLIENTE*, EMPLEADO, GERENTE |
| POST | `/api/carrito` | CLIENTE*, EMPLEADO, GERENTE |
| PUT | `/api/carrito/{id}` | CLIENTE*, EMPLEADO, GERENTE |
| DELETE | `/api/carrito/{id}` | CLIENTE*, EMPLEADO, GERENTE |

### Ventas

| Metodo | URL | Roles |
|--------|-----|-------|
| GET | `/api/ventas` | CLIENTE*, EMPLEADO, GERENTE |
| GET | `/api/ventas/{id}` | CLIENTE*, EMPLEADO, GERENTE |
| POST | `/api/ventas` | CLIENTE*, EMPLEADO, GERENTE |

### Inventario

| Metodo | URL | Roles |
|--------|-----|-------|
| GET | `/api/inventario` | EMPLEADO, GERENTE |
| GET | `/api/inventario/{id}` | EMPLEADO, GERENTE |
| POST | `/api/inventario` | EMPLEADO, GERENTE |
| PUT | `/api/inventario/{id}` | EMPLEADO, GERENTE |
| DELETE | `/api/inventario/{id}` | EMPLEADO, GERENTE |

### Detalle ventas

| Metodo | URL | Roles |
|--------|-----|-------|
| GET | `/api/detalle-ventas` | EMPLEADO, GERENTE |
| GET | `/api/detalle-ventas/{id}` | EMPLEADO, GERENTE |
| POST | `/api/detalle-ventas` | EMPLEADO, GERENTE |
| PUT | `/api/detalle-ventas/{id}` | EMPLEADO, GERENTE |
| DELETE | `/api/detalle-ventas/{id}` | EMPLEADO, GERENTE |

### Usuarios

| Metodo | URL | Roles |
|--------|-----|-------|
| GET | `/api/usuarios` | GERENTE |
| GET | `/api/usuarios/{id}` | GERENTE |
| POST | `/api/usuarios` | GERENTE |
| PUT | `/api/usuarios/{id}` | GERENTE |
| DELETE | `/api/usuarios/{id}` | GERENTE |

---

## Coleccion Postman

Importar: [`Minimarket.postman_collection.json`](Minimarket.postman_collection.json)
