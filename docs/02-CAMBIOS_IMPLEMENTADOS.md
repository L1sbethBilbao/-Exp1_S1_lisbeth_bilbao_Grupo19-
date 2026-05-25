# Cambios de seguridad implementados — Minimarket

Documento que describe las correcciones aplicadas sobre el código auditado, para la actividad Semana 1.

---

## 1. Resumen de cambios

| # | Cambio | Archivo(s) principal(es) | Amenaza mitigada |
|---|--------|--------------------------|------------------|
| 1 | Carga automática de datos (seed) | `config/DataInitializer.java` | BD vacía, paradoja de login |
| 2 | Contraseñas BCrypt al guardar | `UsuarioServiceImpl.java` | Texto plano en BD |
| 3 | Ocultar password en JSON (respuestas); aceptar en POST/PUT | `Usuario.java` (`@JsonProperty(WRITE_ONLY)`) | Exposición de credenciales |
| 4 | Autorización por roles (RBAC) | `SecurityConfig.java` | Escalada de privilegios |
| 5 | Control IDOR carrito/ventas | `CarritoServiceImpl`, `VentaServiceImpl` | Broken Access Control |
| 6 | Anti mass-assignment de roles | `UsuarioServiceImpl.java` | Asignación arbitraria de roles |
| 7 | Validación de entrada | `pom.xml`, entidades, `@Valid` en controllers | Inyección / datos inválidos |
| 8 | Perfiles Spring dev/prod | `application-*.properties` | Misconfiguration |
| 9 | Límite intentos de login | `LoginAttemptService.java` | Fuerza bruta |
| 10 | Logs login y acceso denegado | `SecurityConfig.java` | Falta de monitoreo |
| 11 | Campo `activo` en usuario | `Usuario.java`, `CustomUserDetails` | Cuentas no gestionables |
| 12 | Respuesta 401 sin sesión | `HttpStatusEntryPoint` | API REST clara |
| 13 | H2 console solo GERENTE | `SecurityConfig.java` | Acceso directo a BD |
| 14 | Fix JSON circular | `Categoria`, `Rol`, `Venta`, `DetalleVenta` | Errores en API |
| 15 | Manejo global de errores | `GlobalExceptionHandler.java` | Respuestas consistentes |
| 16 | Tests de seguridad | `SecurityRolesIntegrationTest.java` | Regresiones |

---

## 2. Detalle por componente

### 2.1 DataInitializer (datos iniciales)

- Se ejecuta al arrancar si no existe `cliente1`.
- Crea roles, usuarios, categorías, productos, carrito, venta e inventario.
- Contraseñas hasheadas con BCrypt.

### 2.2 SecurityConfig

- Reglas por URL y método HTTP según rol.
- Login con handlers de éxito/fallo y logging.
- CSRF deshabilitado con comentario (API REST + Postman).
- `frameOptions sameOrigin` para consola H2 del gerente.

### 2.3 LoginAttemptService

- Máximo 3 intentos fallidos (`security.login.max-attempts`).
- Bloqueo temporal vía `LockedException` en `CustomUserDetailsService`.

### 2.4 IDOR (carrito y ventas)

- **CLIENTE:** solo ve y modifica su carrito y sus ventas.
- **EMPLEADO / GERENTE:** acceso a todos los registros.

### 2.5 UsuarioServiceImpl

- Hash BCrypt si la contraseña no empieza con `$2`.
- Roles del request se resuelven solo si existen en BD (anti mass assignment).
- En actualización, si no se envía password, se conserva el anterior.

### 2.6 Validación

- Dependencia `spring-boot-starter-validation`.
- `@NotBlank`, `@Size`, `@Min` en entidades clave.
- `@Valid` en POST/PUT de controllers.

### 2.7 Perfiles

| Perfil | H2 console | show-sql |
|--------|------------|----------|
| `dev` (default) | Habilitada | true |
| `prod` | Deshabilitada | false |

---

## 3. Lo que NO se implementó (documentar en informe)

| Tema | Motivo |
|------|--------|
| JWT completo | Actividad S1 pide usuario/contraseña, no tokens |
| PostgreSQL | Se mantiene H2 según decisión del proyecto |
| HTTPS/TLS | Entorno local / académico |
| DTOs completos | Alcance Semana 1; entidades con validación parcial |
| OAuth2 / LDAP | Solo análisis teórico en informe |
| CI/CD seguridad | Fuera de alcance |

---

## 4. Archivos nuevos

```
src/main/java/com/minimarket/
├── config/DataInitializer.java
├── exception/GlobalExceptionHandler.java
├── security/
│   ├── service/LoginAttemptService.java
│   └── util/SecurityUtils.java

src/main/resources/
├── application-dev.properties
└── application-prod.properties

src/test/java/com/minimarket/security/
└── SecurityRolesIntegrationTest.java

docs/
├── 01-AUDITORIA_INICIAL.md
├── 02-CAMBIOS_IMPLEMENTADOS.md
├── 03-ESTADO_ACTUAL_SEGURIDAD.md
└── 04-GUIA_PRUEBAS_POSTMAN.md
```

---

## 5. Cómo verificar los cambios

```bash
# Desde la raíz del proyecto
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

Luego seguir `04-GUIA_PRUEBAS_POSTMAN.md`.
