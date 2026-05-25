# Estado actual del proyecto — Seguridad, datos y endpoints

**Versión:** Post-correcciones Semana 1  
**Base de datos:** H2 en memoria (perfil `dev` por defecto)  
**URL base:** `http://localhost:8080`

---

## 1. Cómo levantar la aplicación

### Opción A — Maven (terminal)

```bash
cd C:\Users\lisbe\OneDrive\Escritorio\minimarket
.\mvnw.cmd spring-boot:run
```

### Opción B — IDE (IntelliJ / VS Code)

1. Abrir el proyecto como proyecto Maven.
2. Ejecutar `MinimarketApplication.java` (Run).

### Verificar que arrancó

- Consola muestra: `Started MinimarketApplication`
- Log: `Datos iniciales cargados correctamente` (primera vez)
- Log: `Usuarios de prueba: cliente1/cliente123 | ...`
- Navegador: `http://localhost:8080/public/hola` → `¡Hola Mundo!`

### Perfiles Spring

| Comando | Perfil | Uso |
|---------|--------|-----|
| Por defecto | `dev` | Desarrollo, H2 console, SQL en logs |
| `.\mvnw.cmd spring-boot:run -Dspring-boot.run.arguments=--spring.profiles.active=prod` | `prod` | Sin consola H2, sin SQL en logs |

### Ejecutar tests

```bash
.\mvnw.cmd test
```

---

## 2. Base de datos H2

### Conexión (consola H2 — solo gerente)

| Campo | Valor |
|-------|-------|
| URL consola | `http://localhost:8080/h2-console` |
| JDBC URL | `jdbc:h2:mem:testdb` |
| Usuario | `sa` |
| Password | *(vacío)* |

> Solo usuario con rol **GERENTE** puede acceder a `/h2-console` vía navegador.

### Datos cargados automáticamente (seed)

Los IDs suelen ser 1, 2, 3… en el primer arranque. Si reinicias la app, H2 en memoria **borra todo** y vuelve a cargar.

#### Roles (`rol`)

| id | nombre |
|----|--------|
| 1 | ROLE_CLIENTE |
| 2 | ROLE_EMPLEADO |
| 3 | ROLE_GERENTE |

#### Usuarios (`usuario` + `usuario_roles`)

| id | username | password (texto para Postman) | rol |
|----|----------|--------------------------------|-----|
| 1 | cliente1 | cliente123 | ROLE_CLIENTE |
| 2 | empleado1 | empleado123 | ROLE_EMPLEADO |
| 3 | gerente1 | gerente123 | ROLE_GERENTE |

> En BD la contraseña está **hasheada** (BCrypt). En Postman usas el texto plano de la tabla.

#### Categorías (`categoria`)

| id | nombre |
|----|--------|
| 1 | Bebidas |
| 2 | Snacks |

#### Productos (`producto`)

| id | nombre | precio | stock | categoria_id |
|----|--------|--------|-------|--------------|
| 1 | Agua 500ml | 800 | 50 | 1 |
| 2 | Papas fritas | 1200 | 30 | 2 |
| 3 | Jugo natural | 1500 | 20 | 1 |

#### Carrito (`carrito`) — del cliente1

| id | usuario_id | producto_id | cantidad |
|----|------------|-------------|----------|
| 1 | 1 | 1 | 2 |
| 2 | 1 | 2 | 1 |

#### Venta (`venta` + `detalle_venta`)

| venta id | usuario_id | detalle | producto | cantidad | precio |
|----------|------------|---------|----------|----------|--------|
| 1 | 1 (cliente1) | 1 | 1 (Agua) | 2 | 800 |

#### Inventario (`inventario`)

| id | producto_id | cantidad | tipoMovimiento |
|----|-------------|----------|----------------|
| 1 | 1 | 50 | Entrada |
| 2 | 2 | 30 | Entrada |

---

## 3. Usuarios y roles — seguridad

### Estrategia de autenticación

- **Tipo:** Usuario y contraseña almacenados en BD (JDBC/JPA).
- **Mecanismo:** Spring Security Form Login + sesión HTTP.
- **Cookie:** `JSESSIONID` (Postman la guarda automáticamente).
- **Hash:** BCrypt.

### Matriz de autorización por rol

| Recurso | CLIENTE | EMPLEADO | GERENTE |
|---------|---------|----------|---------|
| `/public/**` | ✅ | ✅ | ✅ |
| GET productos/categorías | ✅ | ✅ | ✅ |
| POST/PUT/DELETE productos/categorías | ❌ | ✅ | ✅ |
| Carrito (CRUD) | ✅* | ✅ | ✅ |
| Ventas (GET/POST) | ✅* | ✅ | ✅ |
| Inventario | ❌ | ✅ | ✅ |
| Detalle ventas | ❌ | ✅ | ✅ |
| Usuarios (CRUD) | ❌ | ❌ | ✅ |
| H2 console | ❌ | ❌ | ✅ |

\* **CLIENTE:** solo **sus** carritos y **sus** ventas (control IDOR).

---

## 4. Endpoints públicos vs privados

### Públicos (sin login)

| Método | URL | Descripción |
|--------|-----|-------------|
| GET | `/public/hola` | Mensaje de prueba |
| POST | `/login` | Autenticación (form) |

### Privados — requieren sesión (cookie)

Todos los de `/api/**` y:

| Método | URL | Rol mínimo |
|--------|-----|------------|
| POST | `/logout` | Autenticado |

### Detalle completo de API privada

| Método | URL | Roles permitidos |
|--------|-----|------------------|
| GET | `/api/productos` | CLIENTE, EMPLEADO, GERENTE |
| GET | `/api/productos/{id}` | CLIENTE, EMPLEADO, GERENTE |
| POST | `/api/productos` | EMPLEADO, GERENTE |
| PUT | `/api/productos/{id}` | EMPLEADO, GERENTE |
| DELETE | `/api/productos/{id}` | EMPLEADO, GERENTE |
| GET | `/api/categorias` | CLIENTE, EMPLEADO, GERENTE |
| GET | `/api/categorias/{id}` | CLIENTE, EMPLEADO, GERENTE |
| POST | `/api/categorias` | EMPLEADO, GERENTE |
| PUT | `/api/categorias/{id}` | EMPLEADO, GERENTE |
| DELETE | `/api/categorias/{id}` | EMPLEADO, GERENTE |
| GET | `/api/carrito` | CLIENTE*, EMPLEADO, GERENTE |
| GET | `/api/carrito/{id}` | CLIENTE*, EMPLEADO, GERENTE |
| POST | `/api/carrito` | CLIENTE*, EMPLEADO, GERENTE |
| PUT | `/api/carrito/{id}` | CLIENTE*, EMPLEADO, GERENTE |
| DELETE | `/api/carrito/{id}` | CLIENTE*, EMPLEADO, GERENTE |
| GET | `/api/ventas` | CLIENTE*, EMPLEADO, GERENTE |
| GET | `/api/ventas/{id}` | CLIENTE*, EMPLEADO, GERENTE |
| POST | `/api/ventas` | CLIENTE*, EMPLEADO, GERENTE |
| GET | `/api/inventario` | EMPLEADO, GERENTE |
| GET | `/api/inventario/{id}` | EMPLEADO, GERENTE |
| POST | `/api/inventario` | EMPLEADO, GERENTE |
| PUT | `/api/inventario/{id}` | EMPLEADO, GERENTE |
| DELETE | `/api/inventario/{id}` | EMPLEADO, GERENTE |
| GET | `/api/detalle-ventas` | EMPLEADO, GERENTE |
| GET | `/api/detalle-ventas/{id}` | EMPLEADO, GERENTE |
| POST | `/api/detalle-ventas` | EMPLEADO, GERENTE |
| PUT | `/api/detalle-ventas/{id}` | EMPLEADO, GERENTE |
| DELETE | `/api/detalle-ventas/{id}` | EMPLEADO, GERENTE |
| GET | `/api/usuarios` | GERENTE |
| GET | `/api/usuarios/{id}` | GERENTE |
| POST | `/api/usuarios` | GERENTE |
| PUT | `/api/usuarios/{id}` | GERENTE |
| DELETE | `/api/usuarios/{id}` | GERENTE |
| GET | `/h2-console/**` | GERENTE |

### Códigos HTTP de seguridad

| Código | Significado |
|--------|-------------|
| **200** | OK — acceso permitido |
| **201/204** | Creado / sin contenido |
| **400** | Validación fallida |
| **401** | No autenticado (sin cookie o login fallido) |
| **403** | Autenticado pero sin permiso (rol o IDOR) |
| **404** | Recurso no encontrado |

---

## 5. Seguridad que cubre el proyecto hoy

| Capa | Qué protege |
|------|-------------|
| **Autenticación** | Solo usuarios de BD con BCrypt |
| **Autorización (RBAC)** | Permisos por rol en URLs |
| **IDOR** | Cliente aislado a sus carritos/ventas |
| **Confidencialidad** | Password no en JSON |
| **Integridad parcial** | Validación de campos, roles solo desde BD |
| **Anti fuerza bruta** | 3 intentos → bloqueo temporal |
| **Logging** | Login fallido, exitoso, acceso denegado |
| **Configuración** | Perfiles dev/prod, H2 restringida |
| **Tests** | Pruebas automáticas de roles |

---

## 6. OWASP Top 10 (2021) — Cumplimiento actual

| # | Categoría | Estado | Evidencia / pendiente |
|---|-----------|--------|----------------------|
| **A01** | Broken Access Control | **Parcial → Mejorado** | RBAC + IDOR carrito/ventas. Documentar en informe. |
| **A02** | Cryptographic Failures | **Parcial** | BCrypt ✅. HTTPS y H2 password vacío: documentar para prod. |
| **A03** | Injection | **Parcial → Mejorado** | JPA parametrizado + `@Valid`. |
| **A04** | Insecure Design | **Parcial** | Form login en API; JWT planificado a futuro. |
| **A05** | Security Misconfiguration | **Parcial → Mejorado** | Perfiles dev/prod; prod sin consola H2. |
| **A06** | Vulnerable Components | **Parcial** | Spring Boot 3.4.1 actual; recomendar SCA en informe. |
| **A07** | Auth Failures | **Parcial → Mejorado** | Rate limit, logs, BCrypt. Sin política compleja de password. |
| **A08** | Software & Data Integrity | **No** | Sin CI de seguridad — documentar como mejora. |
| **A09** | Logging & Monitoring | **Parcial → Mejorado** | Logs básicos; sin SIEM. |
| **A10** | SSRF | **N/A** | No hay llamadas HTTP salientes. |

### Resumen OWASP

| Nivel | Cantidad |
|-------|----------|
| Mitigado / Mejorado | 6 categorías (parcial o más) |
| Pendiente (informe) | A02 HTTPS, A08 CI, A04 JWT |
| N/A | A10 |

**¿Cumple OWASP Top 10 al 100%?** **No** — ningún proyecto real suele cumplir al 100% en S1. **Sí demuestra conocimiento** de las 10 categorías con mitigaciones reales en código y mejoras documentadas.

---

## 7. Qué falta para la entrega académica

| Ítem | Dónde |
|------|-------|
| Informe Word (Pasos 1–3) | Entrega AVA |
| Capturas Postman | Informe + evidencias |
| Texto normativa datos personales (Ley 19.628) | Informe |
| Subir Word a GitHub | Repositorio |
| Enlace del repo en AVA | Plataforma |

Ver lista detallada de pruebas: **`04-GUIA_PRUEBAS_POSTMAN.md`**.

---

## 8. Documentos relacionados

| Archivo | Contenido |
|---------|-----------|
| `01-AUDITORIA_INICIAL.md` | Hallazgos del código original |
| `02-CAMBIOS_IMPLEMENTADOS.md` | Correcciones aplicadas |
| `04-GUIA_PRUEBAS_POSTMAN.md` | Colección de pruebas paso a paso |
