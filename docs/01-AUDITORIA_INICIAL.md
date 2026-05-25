# Auditoría inicial de seguridad — Proyecto Minimarket

**Fecha de referencia:** Semana 1 — Desarrollo Backend II  
**Proyecto:** API REST Spring Boot 3.4.1 + Spring Security + H2  
**Tipo:** Auditoría de solo lectura sobre el código entregado (antes de las correcciones)

---

## 1. Resumen ejecutivo

El backend **Minimarket** es una API para gestionar usuarios, productos, categorías, carrito, ventas e inventario. Incluía Spring Security de forma **parcial e inconsistente**: había estructura de seguridad (roles, `UserDetailsService`, BCrypt como bean) pero con **defectos intencionados** típicos de un caso académico para que el estudiante los detecte y corrija.

**Nivel de riesgo inicial:** ALTO — no apto para producción sin correcciones.

---

## 2. Estructura del proyecto auditado

```
com.minimarket/
├── MinimarketApplication.java
├── config/          (vacío al inicio)
├── controller/      (8 controladores REST)
├── entity/          (8 entidades JPA)
├── repository/      (8 repositorios)
├── service/         (interfaces + impl)
└── security/
    ├── config/SecurityConfig.java
    ├── model/CustomUserDetails.java, LoginRequest.java (vacío)
    ├── service/CustomUserDetailsService.java
    └── util/JwtUtil.java (vacío)
```

**Patrón:** Controller → Service → Repository → H2 (memoria).

---

## 3. Stack tecnológico (pom.xml)

| Dependencia | Uso |
|-------------|-----|
| spring-boot-starter-web | API REST |
| spring-boot-starter-data-jpa | Persistencia |
| spring-boot-starter-security | Seguridad |
| h2 | Base de datos en memoria |
| lombok | Declarado, poco usado |

**No incluía:** JWT, validación (`starter-validation`), PostgreSQL, Flyway, OpenAPI.

---

## 4. Configuración inicial detectada

```properties
spring.datasource.url=jdbc:h2:mem:testdb
spring.h2.console.enabled=true
spring.jpa.show-sql=true
spring.jpa.hibernate.ddl-auto=update
```

| Configuración | Riesgo |
|---------------|--------|
| H2 en memoria | Datos se pierden al reiniciar |
| Consola H2 habilitada | Acceso directo a BD |
| `show-sql=true` | Filtración de datos en logs |
| Sin perfiles dev/prod | Misma config para todo entorno |

---

## 5. Estado de la base de datos al entregar

| Tabla | Datos al arrancar |
|-------|-------------------|
| usuario | **0** |
| rol | **0** |
| producto, categoria, carrito, venta, etc. | **0** |

**Problema crítico:** Sin `data.sql` ni `CommandLineRunner` → imposible hacer login ni probar la API sin intervención manual en H2.

**Paradoja:** `POST /api/usuarios` requería autenticación, pero no existía ningún usuario.

---

## 6. Autenticación y autorización (estado inicial)

### Lo que existía

- `SecurityFilterChain` con `/public/**` público y resto `authenticated()`.
- Form Login (sesión HTTP + cookie `JSESSIONID`).
- `CustomUserDetailsService` cargando usuarios desde BD.
- Bean `BCryptPasswordEncoder` definido.
- Entidades `Usuario` y `Rol` con relación ManyToMany.

### Lo que NO existía o estaba mal

| Defecto | Impacto |
|---------|---------|
| Roles en BD sin uso en endpoints | Cualquier usuario autenticado = acceso total |
| BCrypt no aplicado al guardar usuarios | Contraseñas en texto plano posibles |
| Password sin `@JsonIgnore` | Exposición en respuestas JSON |
| `JwtUtil` y `LoginRequest` vacíos | Código incompleto / confusión |
| CSRF deshabilitado sin documentar | Riesgo con frontend basado en cookies |
| Sin `@PreAuthorize` ni reglas por rol | Sin autorización real |
| Sin control de propiedad (IDOR) | Cliente podía ver datos de otros |
| Sin validación de entrada | Datos inválidos aceptados |
| Sin logs de seguridad | Sin trazabilidad |
| Sin datos de prueba | API no demostrable |

---

## 7. Inventario de endpoints (antes de correcciones)

| Método | Ruta | Protección inicial |
|--------|------|-------------------|
| GET | `/public/hola` | Público |
| POST | `/login` | Público (Spring Security) |
| POST | `/logout` | Autenticado |
| CRUD | `/api/usuarios/**` | Solo autenticado (sin rol) |
| CRUD | `/api/productos/**` | Solo autenticado |
| CRUD | `/api/categorias/**` | Solo autenticado |
| CRUD | `/api/carrito/**` | Solo autenticado |
| GET/POST | `/api/ventas/**` | Solo autenticado |
| CRUD | `/api/inventario/**` | Solo autenticado |
| CRUD | `/api/detalle-ventas/**` | Solo autenticado |
| GET | `/h2-console/**` | Solo autenticado (cualquier rol) |

---

## 8. Puntos críticos identificados

### P0 — Críticos

1. Exposición de contraseñas en JSON (`GET /api/usuarios`).
2. Contraseñas sin hash al crear usuarios vía API.
3. Ausencia total de autorización por roles.
4. Consola H2 accesible para cualquier usuario logueado.
5. Base de datos vacía — sistema no usable.

### P1 — Altos

6. IDOR en carrito, ventas y recursos por usuario.
7. Mass assignment (asignar roles arbitrarios en JSON).
8. Seguridad inconsistente (JWT stub, form login en API REST).

### P2 — Medios

9. CSRF deshabilitado.
10. SQL visible en logs.
11. Sin auditoría de login fallido.
12. Referencias circulares JSON (Categoría ↔ Producto).
13. Sin tests de seguridad.

---

## 9. Amenazas potenciales

| Amenaza | Vector en el proyecto original |
|---------|-------------------------------|
| Robo de credenciales | Password en respuesta JSON |
| Escalada de privilegios | Sin RBAC |
| IDOR | Sin filtro por dueño del recurso |
| Acceso a BD vía H2 | Consola web |
| Fuerza bruta | Sin límite en `/login` |
| Inyección SQL | Mitigado parcialmente por JPA |
| CSRF | CSRF off + cookies de sesión |
| Information disclosure | `show-sql`, stack traces default |

---

## 10. OWASP Top 10 (2021) — Estado inicial

| # | Categoría | Cumplimiento inicial |
|---|-----------|---------------------|
| A01 | Broken Access Control | **NO** |
| A02 | Cryptographic Failures | **NO** |
| A03 | Injection | **PARCIAL** |
| A04 | Insecure Design | **NO** |
| A05 | Security Misconfiguration | **NO** |
| A06 | Vulnerable Components | **PARCIAL** |
| A07 | Identification & Auth Failures | **NO** |
| A08 | Software & Data Integrity | **NO** |
| A09 | Logging & Monitoring | **NO** |
| A10 | SSRF | **N/A** |

**Resultado:** ~15% de cumplimiento real.

---

## 11. Conclusión de la auditoría

El proyecto entregado era un **esqueleto con fallas pedagógicas** para practicar análisis y corrección de seguridad en Spring Security. La arquitectura en capas era correcta, pero la implementación de seguridad estaba **incompleta** y presentaba vulnerabilidades graves si se desplegara sin cambios.

Las correcciones implementadas se documentan en `02-CAMBIOS_IMPLEMENTADOS.md` y el estado actual en `03-ESTADO_ACTUAL_SEGURIDAD.md`.
