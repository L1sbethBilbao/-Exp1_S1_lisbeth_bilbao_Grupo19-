# Guía de pruebas Postman — Minimarket

**URL base:** `http://localhost:8080`  
**Autenticación:** Form Login → cookie de sesión `JSESSIONID`

---

## 1. Configuración inicial de Postman

### 1.1 Habilitar cookies (obligatorio)

1. Abre Postman.
2. **Settings** (engranaje) → **General**.
3. Activa **"Automatically follow redirects"** (recomendado).
4. En la pestaña **Cookies** (o desde dominio `localhost`):
   - Asegúrate de que Postman **guarde cookies** para `localhost:8080`.

> Sin cookies, después del login las peticiones a `/api/**` devolverán **401**.

### 1.2 Variables de colección (opcional pero útil)

Crea un Environment o variables en la colección:

| Variable | Valor inicial |
|----------|---------------|
| `baseUrl` | `http://localhost:8080` |
| `username` | `cliente1` |
| `password` | `cliente123` |

Usa `{{baseUrl}}/api/productos` en las URLs.

### 1.3 Cómo hacer login en Postman

| Campo | Valor |
|-------|-------|
| **Método** | POST |
| **URL** | `http://localhost:8080/login` |
| **Body** | `x-www-form-urlencoded` |
| **Parámetros** | `username` = `cliente1` , `password` = `cliente123` |

**No uses** JSON en el login. Debe ser **form-urlencoded**.

**Respuesta esperada:**

- Status: **302** (redirect) o **200** según configuración de Postman.
- Header `Set-Cookie` con `JSESSIONID=...`
- Si sigues redirects, terminas en `/public/hola` con cuerpo `¡Hola Mundo!`

**Importante:** Después del login, las demás peticiones de la **misma sesión de Postman** envían la cookie automáticamente.

### 1.4 Cerrar sesión (logout)

| Campo | Valor |
|-------|-------|
| Método | POST |
| URL | `http://localhost:8080/logout` |

Requiere cookie activa. Redirige a `/public/hola`.

### 1.5 Cambiar de usuario

1. Haz **logout** O borra cookies de `localhost`.
2. Vuelve a ejecutar **Login** con otro `username` / `password`.

---

## 2. Lista de pruebas Postman

Ejecuta en orden lógico: primero **sin login**, luego **login por rol**, luego pruebas de permisos.

### Bloque A — Endpoints públicos (sin login, sin Body)

| # | Nombre prueba | Método | URL | Body | Respuesta esperada |
|---|---------------|--------|-----|------|-------------------|
| A1 | Public hola | GET | `{{baseUrl}}/public/hola` | — | **200** — texto `¡Hola Mundo!` |
| A2 | Productos sin auth | GET | `{{baseUrl}}/api/productos` | — | **401** Unauthorized |
| A3 | Usuarios sin auth | GET | `{{baseUrl}}/api/usuarios` | — | **401** Unauthorized |

---

### Bloque B — Login

| # | Nombre prueba | Método | URL | Body (x-www-form-urlencoded) | Respuesta esperada |
|---|---------------|--------|-----|-------------------------------|-------------------|
| B1 | Login cliente OK | POST | `{{baseUrl}}/login` | username=`cliente1`, password=`cliente123` | **302/200** + cookie JSESSIONID |
| B2 | Login credenciales malas | POST | `{{baseUrl}}/login` | username=`cliente1`, password=`wrong` | **401** |
| B3 | Login empleado | POST | `{{baseUrl}}/login` | username=`empleado1`, password=`empleado123` | Cookie nueva |
| B4 | Login gerente | POST | `{{baseUrl}}/login` | username=`gerente1`, password=`gerente123` | Cookie nueva |

**Prueba extra fuerza bruta (B5):** Repite B2 **tres veces**; el **cuarto** intento incluso con password correcta → fallo (cuenta bloqueada temporalmente). Reinicia la app para desbloquear.

---

### Bloque C — CLIENTE (`cliente1` logueado)

Hacer **B1** antes de cada fila si cambiaste de usuario.

| # | Nombre prueba | Método | URL | Body | Respuesta esperada |
|---|---------------|--------|-----|------|-------------------|
| C1 | Cliente lista productos | GET | `{{baseUrl}}/api/productos` | — | **200** — JSON array (3 productos) |
| C2 | Cliente lista categorias | GET | `{{baseUrl}}/api/categorias` | — | **200** — JSON array |
| C3 | Cliente su carrito | GET | `{{baseUrl}}/api/carrito` | — | **200** — solo items de cliente1 (2 items) |
| C4 | Cliente sus ventas | GET | `{{baseUrl}}/api/ventas` | — | **200** — solo ventas de cliente1 |
| C5 | Cliente inventario denegado | GET | `{{baseUrl}}/api/inventario` | — | **403** Forbidden |
| C6 | Cliente usuarios denegado | GET | `{{baseUrl}}/api/usuarios` | — | **403** Forbidden |
| C7 | Cliente crear producto denegado | POST | `{{baseUrl}}/api/productos` | Ver JSON abajo | **403** Forbidden |
| C8 | Cliente IDOR carrito ajeno | GET | `{{baseUrl}}/api/carrito/99` | — | **404** o **403** si id existe de otro usuario |

**JSON para C7 (no debería ejecutarse):**
```json
{
  "nombre": "Producto Hack",
  "precio": 100,
  "stock": 10,
  "categoria": { "id": 1 }
}
```
Headers: `Content-Type: application/json`

---

### Bloque D — EMPLEADO (`empleado1` logueado)

| # | Nombre prueba | Método | URL | Body | Respuesta esperada |
|---|---------------|--------|-----|------|-------------------|
| D1 | Empleado lista inventario | GET | `{{baseUrl}}/api/inventario` | — | **200** |
| D2 | Empleado lista usuarios denegado | GET | `{{baseUrl}}/api/usuarios` | — | **403** |
| D3 | Empleado crea producto | POST | `{{baseUrl}}/api/productos` | Ver JSON abajo | **200** — producto creado |
| D4 | Empleado ve todo el carrito | GET | `{{baseUrl}}/api/carrito` | — | **200** — todos los carritos |

**JSON para D3:**
```json
{
  "nombre": "Galletas",
  "precio": 900,
  "stock": 25,
  "categoria": { "id": 2 }
}
```
Headers: `Content-Type: application/json`

---

### Bloque E — GERENTE (`gerente1` logueado)

| # | Nombre prueba | Método | URL | Body | Respuesta esperada |
|---|---------------|--------|-----|------|-------------------|
| E1 | Gerente lista usuarios | GET | `{{baseUrl}}/api/usuarios` | — | **200** — sin campo password |
| E2 | Gerente lista inventario | GET | `{{baseUrl}}/api/inventario` | — | **200** |
| E3 | Gerente accede H2 console | GET | `{{baseUrl}}/h2-console/` | — | **200** (HTML consola) |
| E4 | Gerente crea usuario | POST | `{{baseUrl}}/api/usuarios` | Ver JSON abajo | **200** |

**JSON para E4:**
```json
{
  "username": "cliente2",
  "password": "cliente456",
  "activo": true,
  "roles": [
    { "nombre": "ROLE_CLIENTE" }
  ]
}
```
Headers: `Content-Type: application/json`

> El password se guarda hasheado. Roles inventados en JSON son ignorados si no existen en BD.

---

### Bloque F — Validación

Login como **empleado1**. 

| # | Nombre prueba | Método | URL | Body | Respuesta esperada |
|---|---------------|--------|-----|------|-------------------|
| F1 | Producto precio negativo | POST | `{{baseUrl}}/api/productos` | `{"nombre":"X","precio":-1,"stock":1,"categoria":{"id":1}}` | **400** + JSON errores |

---

### Bloque G — Password no expuesto

| # | Nombre prueba | Método | URL | Respuesta esperada |
|---|---------------|--------|-----|-------------------|
| G1 | Usuarios sin password en JSON | GET | `{{baseUrl}}/api/usuarios` (como gerente1) | **200** — ningún objeto tiene campo `password` |

---

## 3. Resumen rápido: qué llenar en Postman

| Tipo de petición | Headers | Body |
|------------------|---------|------|
| **Login** | — | `x-www-form-urlencoded`: username, password |
| **GET público/privado** | — | vacío |
| **POST/PUT JSON** | `Content-Type: application/json` | raw JSON |
| **Todas las privadas** | Cookie automática tras login | — |

---

## 4. Checklist de evidencias para el informe

Captura pantalla de:

- [ ] A1 — 200 público
- [ ] A2 — 401 sin auth
- [ ] B1 — login exitoso (cookies visibles)
- [ ] B2 — 401 login fallido
- [ ] C5 — 403 cliente en inventario
- [ ] C6 — 403 cliente en usuarios
- [ ] D1 — 200 empleado inventario
- [ ] D2 — 403 empleado usuarios
- [ ] E1 — 200 gerente usuarios (sin password)
- [ ] F1 — 400 validación
- [ ] Consola del IDE con log `Acceso denegado` o `Login exitoso`

---

## 5. Solución de problemas

| Problema | Solución |
|----------|----------|
| Siempre 401 en `/api/**` | Haz login primero; revisa cookies en Postman |
| 403 inesperado | Verifica con qué usuario hiciste login |
| Login no guarda cookie | Body debe ser **x-www-form-urlencoded**, no JSON |
| Datos vacíos | Reinicia app y espera log de DataInitializer |
| Cuenta bloqueada | Reinicia la aplicación (mapa en memoria se limpia) |
| IDs 404 en carrito | Usa ids 1 o 2 tras seed; reinicio borra y recrea |

---

## 6. Referencias

- Estado de seguridad: `03-ESTADO_ACTUAL_SEGURIDAD.md`
- Auditoría original: `01-AUDITORIA_INICIAL.md`
- Cambios aplicados: `02-CAMBIOS_IMPLEMENTADOS.md`
