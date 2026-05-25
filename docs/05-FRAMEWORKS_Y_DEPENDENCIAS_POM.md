# Frameworks y dependencias del proyecto Minimarket

Documento de referencia para la actividad Semana 1: qué tecnologías usa el backend, para qué sirven y cómo contribuyen a la seguridad.

---

## 1. Visión general

El proyecto **Minimarket** es una **API REST** construida sobre el ecosistema **Spring** (Java), empaquetada y ejecutada con **Spring Boot 3.4.1** y **Java 17**.

```
┌─────────────────────────────────────────────────────────────┐
│                    CAPA DE PRESENTACIÓN                      │
│  Spring Web (REST) + Bean Validation                         │
├─────────────────────────────────────────────────────────────┤
│                    CAPA DE SEGURIDAD                         │
│  Spring Security (filtros, auth, roles, BCrypt)              │
├─────────────────────────────────────────────────────────────┤
│                    CAPA DE NEGOCIO                           │
│  Servicios Spring (@Service)                                 │
├─────────────────────────────────────────────────────────────┤
│                    CAPA DE DATOS                             │
│  Spring Data JPA + Hibernate + H2                            │
└─────────────────────────────────────────────────────────────┘
```

**Framework principal de seguridad:** **Spring Security** (dentro del ecosistema Spring).

---

## 2. Frameworks que estamos usando

### 2.1 Spring Boot 3.4.1

| Aspecto | Detalle |
|---------|---------|
| **Qué es** | Framework que simplifica la creación de aplicaciones Spring listas para ejecutar. |
| **Para qué lo usamos** | Arranque automático, configuración por convención, servidor embebido (Tomcat), inyección de dependencias. |
| **En este proyecto** | Punto de entrada `MinimarketApplication`, perfiles `dev`/`prod`, auto-configuración de JPA, Security y Web. |
| **Ayuda a la seguridad** | Integra Spring Security sin configuración manual extensa; permite perfiles para separar entornos (dev vs prod). |

### 2.2 Spring Security 6.x (vía starter-security)

| Aspecto | Detalle |
|---------|---------|
| **Qué es** | Framework de seguridad estándar en aplicaciones Java/Spring. |
| **Para qué lo usamos** | Autenticación (¿quién eres?), autorización (¿qué puedes hacer?), protección de endpoints. |
| **En este proyecto** | `SecurityConfig`, `SecurityFilterChain`, Form Login, roles CLIENTE/EMPLEADO/GERENTE, BCrypt, logs de acceso. |
| **Ayuda a la seguridad** | Ver sección 3 (detalle completo). |

### 2.3 Spring Web (spring-boot-starter-web)

| Aspecto | Detalle |
|---------|---------|
| **Qué es** | Módulo para construir aplicaciones web y APIs REST sobre servlet (Tomcat embebido). |
| **Para qué lo usamos** | Exponer endpoints HTTP JSON (`@RestController`). |
| **En este proyecto** | Todos los controladores en `/api/**` y `/public/**`. |
| **Ayuda a la seguridad** | Cada petición pasa primero por los filtros de Spring Security antes de llegar al controlador. |

### 2.4 Spring Data JPA + Hibernate

| Aspecto | Detalle |
|---------|---------|
| **Qué es** | Abstracción para acceso a datos con JPA (Java Persistence API); Hibernate es la implementación ORM. |
| **Para qué lo usamos** | Persistir entidades (Usuario, Producto, etc.) sin escribir SQL manual en la mayoría de casos. |
| **En este proyecto** | Repositorios `JpaRepository`, entidades `@Entity`, consultas derivadas (`findByUsername`). |
| **Ayuda a la seguridad** | Consultas **parametrizadas** → reduce riesgo de **inyección SQL**; usuarios y roles se leen de BD de forma controlada para autenticación. |

### 2.5 Bean Validation (spring-boot-starter-validation)

| Aspecto | Detalle |
|---------|---------|
| **Qué es** | Estándar Java (JSR 380) para validar datos de entrada (`@NotBlank`, `@Size`, `@Min`, etc.). |
| **Para qué lo usamos** | Rechazar datos inválidos antes de persistirlos. |
| **En este proyecto** | `@Valid` en controllers; validaciones en `Usuario`, `Producto`, `Categoria`, `Carrito`. |
| **Ayuda a la seguridad** | Mitiga entrada maliciosa o corrupta (OWASP A03 – validación de datos); evita estados inconsistentes en BD. |

### 2.6 H2 Database

| Aspecto | Detalle |
|---------|---------|
| **Qué es** | Motor de base de datos relacional ligero, en este proyecto usado **en memoria**. |
| **Para qué lo usamos** | Desarrollo y pruebas sin instalar PostgreSQL/MySQL. |
| **En este proyecto** | `jdbc:h2:mem:testdb`, consola web en perfil `dev`. |
| **Ayuda a la seguridad** | No es un framework de seguridad; la seguridad depende de **cómo se configura** (consola restringida a GERENTE, perfiles, no usar en producción real sin endurecimiento). |

### 2.7 Maven

| Aspecto | Detalle |
|---------|---------|
| **Qué es** | Herramienta de gestión de dependencias y build (compilar, test, empaquetar JAR). |
| **Para qué lo usamos** | Descargar librerías del `pom.xml`, ejecutar `mvnw test`, `mvnw spring-boot:run`. |
| **Ayuda a la seguridad** | Centraliza versiones de dependencias (menos librerías obsoletas si se mantiene actualizado). |

---

## 3. Spring Security: cómo ayuda a la seguridad en este proyecto

Spring Security actúa como una **aduana** entre Internet/Postman y tu código de negocio.

### 3.1 Flujo de una petición

```
Cliente (Postman)
       │
       ▼
┌──────────────────┐
│ Filtros Security │  ← Verifica sesión, roles, bloqueos
└────────┬─────────┘
         ▼
┌──────────────────┐
│  Controller      │  ← Solo si está autorizado
└────────┬─────────┘
         ▼
┌──────────────────┐
│  Service / JPA   │
└──────────────────┘
```

### 3.2 Funciones concretas en Minimarket

| Función de Spring Security | Implementación en el proyecto | Beneficio |
|-----------------------------|------------------------------|-----------|
| **Autenticación** | Form Login + `CustomUserDetailsService` + BD | Solo usuarios registrados acceden |
| **Hash de contraseñas** | `BCryptPasswordEncoder` | Contraseñas no legibles en BD |
| **Autorización por URL** | `SecurityFilterChain` + `hasRole` / `hasAnyRole` | Cada rol accede solo a lo permitido |
| **Sesión HTTP** | Cookie `JSESSIONID` | Estado de login entre peticiones |
| **Entry point 401** | `HttpStatusEntryPoint` | API clara sin login |
| **Access denied 403** | `accessDeniedHandler` + log | Bloqueo y trazabilidad |
| **Bloqueo por intentos** | `LoginAttemptService` + `LockedException` | Anti fuerza bruta |
| **Method security** | `@EnableMethodSecurity` | Base para `@PreAuthorize` futuro |
| **Headers** | `frameOptions sameOrigin` | Soporte controlado de H2 console |

### 3.3 Amenazas que mitiga Spring Security (directamente o con nuestra config)

| Amenaza | Mitigación |
|---------|------------|
| Acceso no autorizado | Filtros + roles |
| Contraseñas en texto plano | BCrypt |
| Escalada de privilegios | RBAC por rol |
| Fuerza bruta en login | Límite de intentos |
| Sesiones no validadas | Filtro de autenticación en cada request |

---

## 4. Explicación del `pom.xml`

### 4.1 Identidad del proyecto

```xml
<groupId>com.minimarket</groupId>
<artifactId>minimarket</artifactId>
<version>0.0.1-SNAPSHOT</version>
```

| Elemento | Significado |
|----------|-------------|
| `groupId` | Identificador de la organización/paquete (`com.minimarket`). |
| `artifactId` | Nombre del proyecto/JAR (`minimarket`). |
| `version` | `0.0.1-SNAPSHOT` = versión en desarrollo, no release final. |

### 4.2 Parent: spring-boot-starter-parent 3.4.1

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.4.1</version>
</parent>
```

| Qué hace | Por qué importa |
|----------|-----------------|
| Hereda versiones compatibles de todas las dependencias Spring | Evita conflictos entre librerías |
| Define plugins de compilación y empaquetado | Build reproducible |
| Fija Java 17 en propiedades | Versión de lenguaje del proyecto |

### 4.3 Propiedades

```xml
<java.version>17</java.version>
<project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
```

- **Java 17:** versión LTS usada para compilar y ejecutar.
- **UTF-8:** codificación de archivos fuente (evita errores con caracteres especiales).

---

## 5. Dependencias (`<dependencies>`) — detalle completo

### 5.1 Dependencias de producción (runtime)

#### `spring-boot-starter-data-jpa`

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
```

| Aspecto | Detalle |
|---------|---------|
| **Incluye** | Spring Data JPA, Hibernate, JDBC, transacciones |
| **Para qué** | Entidades `@Entity`, repositorios, persistencia en H2 |
| **Seguridad** | Consultas parametrizadas; datos de usuarios/roles desde BD para login |

---

#### `spring-boot-starter-security` ⭐ Principal para la actividad

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

| Aspecto | Detalle |
|---------|---------|
| **Incluye** | Spring Security Core, Web, Crypto (BCrypt), filtros servlet |
| **Para qué** | Toda la capa de autenticación y autorización |
| **Seguridad** | Es el **framework de seguridad** requerido por la asignatura |

---

#### `spring-boot-starter-web`

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```

| Aspecto | Detalle |
|---------|---------|
| **Incluye** | Spring MVC, Tomcat embebido, Jackson (JSON) |
| **Para qué** | API REST HTTP en puerto 8080 |
| **Seguridad** | Integración nativa con filtros de Security; JSON para respuestas controladas |

---

#### `spring-boot-starter-validation`

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

| Aspecto | Detalle |
|---------|---------|
| **Incluye** | Hibernate Validator (implementación de Bean Validation) |
| **Para qué** | `@Valid`, `@NotBlank`, `@Min`, etc. |
| **Seguridad** | Valida entrada antes de procesarla (defensa en profundidad) |

---

#### `spring-boot-devtools`

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-devtools</artifactId>
    <scope>runtime</scope>
    <optional>true</optional>
</dependency>
```

| Aspecto | Detalle |
|---------|---------|
| **Scope** | `runtime` — solo al ejecutar, no en compilación de otros proyectos |
| **Optional** | No se propaga como dependencia transitiva |
| **Para qué** | Recarga automática en desarrollo |
| **Seguridad** | ⚠️ **No debe usarse en producción**; solo desarrollo local |

---

#### `h2`

```xml
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>runtime</scope>
</dependency>
```

| Aspecto | Detalle |
|---------|---------|
| **Scope** | `runtime` — necesaria al ejecutar la app |
| **Para qué** | Base de datos en memoria `jdbc:h2:mem:testdb` |
| **Seguridad** | Riesgo si consola H2 queda abierta; mitigado restringiendo acceso a rol GERENTE y perfil `prod` |

---

#### `lombok`

```xml
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <optional>true</optional>
</dependency>
```

| Aspecto | Detalle |
|---------|---------|
| **Para qué** | Reducir boilerplate (getters/setters generados en compilación) |
| **En este proyecto** | Declarado pero el código usa getters/setters manuales |
| **Seguridad** | Sin impacto directo en seguridad |

---

### 5.2 Dependencias de prueba (`test`)

#### `spring-boot-starter-test`

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

| Aspecto | Detalle |
|---------|---------|
| **Incluye** | JUnit 5, Mockito, AssertJ, MockMvc |
| **Para qué** | Tests unitarios e integración (`mvn test`) |
| **Seguridad** | Permite probar que la app arranca y comportamiento básico |

---

#### `spring-security-test`

```xml
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-test</artifactId>
    <scope>test</scope>
</dependency>
```

| Aspecto | Detalle |
|---------|---------|
| **Para qué** | `@WithMockUser`, simular roles en tests sin login real |
| **En este proyecto** | `SecurityRolesIntegrationTest` (401, 403, 200 por rol) |
| **Seguridad** | Verifica reglas de acceso automáticamente; evita regresiones |

---

## 6. Plugins de build (`<build>`)

### `maven-resources-plugin`

- Codificación **UTF-8** al copiar `application.properties` y recursos.
- Evita errores de build con archivos en español.

### `maven-compiler-plugin`

- Compila con **Java 17**.
- Procesa anotaciones de **Lombok** en tiempo de compilación.

### `spring-boot-maven-plugin`

- Genera JAR ejecutable (`java -jar minimarket.jar`).
- Excluye Lombok del JAR final (solo se usa al compilar).

---

## 7. Dependencias que NO están en el pom (y por qué)

| Dependencia | Motivo de ausencia |
|-------------|-------------------|
| **jjwt / nimbus-jose-jwt** | JWT no implementado en Semana 1 (actividad pide usuario/contraseña) |
| **postgresql / mysql** | Se usa H2 para desarrollo |
| **springdoc-openapi** | Sin documentación Swagger |
| **spring-boot-starter-actuator** | Sin endpoints de monitoreo `/actuator` |
| **oauth2-resource-server** | OAuth2 no requerido en S1 |

Estas pueden mencionarse en el informe como **evolución futura** (JWT para SPA, PostgreSQL para producción).

---

## 8. Tabla resumen: dependencia → seguridad

| Dependencia | ¿Aporta seguridad? | Cómo |
|-------------|-------------------|------|
| spring-boot-starter-security | **Sí — principal** | Auth, roles, BCrypt, filtros |
| spring-boot-starter-validation | **Sí** | Validación de entrada |
| spring-boot-starter-data-jpa | **Parcial** | Anti SQLi por ORM |
| spring-boot-starter-web | **Parcial** | Integración con Security |
| spring-security-test | **Sí (tests)** | Pruebas de acceso |
| h2 | **Neutral** | Depende de configuración |
| devtools | **No** (riesgo en prod) | Solo desarrollo |
| lombok | **No** | Productividad |

---

## 9. Texto sugerido para el informe Word (copiar/adaptar)

> El backend Minimarket utiliza **Spring Boot 3.4.1** como framework base y **Spring Security** como framework de seguridad, seleccionado según los requerimientos del cliente de autenticación con usuario y contraseña almacenados en base de datos. Spring Security intercepta cada petición HTTP mediante una cadena de filtros (`SecurityFilterChain`), valida credenciales con **BCrypt**, aplica **autorización basada en roles** (CLIENTE, EMPLEADO, GERENTE) y registra eventos de acceso denegado e intentos de login fallidos. Complementariamente, **Spring Data JPA** reduce el riesgo de inyección SQL mediante consultas parametrizadas, y **Bean Validation** rechaza datos de entrada inválidos. La base de datos **H2** se emplea en entorno de desarrollo con perfiles Spring que separan configuración de desarrollo y producción simulada.

---

## 10. Documentos relacionados

| Archivo | Tema |
|---------|------|
| `01-AUDITORIA_INICIAL.md` | Vulnerabilidades del código original |
| `02-CAMBIOS_IMPLEMENTADOS.md` | Correcciones en SecurityConfig y servicios |
| `03-ESTADO_ACTUAL_SEGURIDAD.md` | Endpoints, roles, OWASP actual |
| `04-GUIA_PRUEBAS_POSTMAN.md` | Pruebas de la seguridad implementada |
