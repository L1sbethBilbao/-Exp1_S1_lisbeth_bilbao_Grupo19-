# Documentación Minimarket — Seguridad Semana 1

Índice de documentos para la actividad **Utilizando estrategias y configuración de frameworks de seguridad en backend**.

| # | Archivo | Descripción |
|---|---------|-------------|
| 1 | [01-AUDITORIA_INICIAL.md](01-AUDITORIA_INICIAL.md) | Auditoría del código entregado (defectos y amenazas originales) |
| 2 | [02-CAMBIOS_IMPLEMENTADOS.md](02-CAMBIOS_IMPLEMENTADOS.md) | Correcciones aplicadas al proyecto |
| 3 | [03-ESTADO_ACTUAL_SEGURIDAD.md](03-ESTADO_ACTUAL_SEGURIDAD.md) | Estado actual, datos BD, endpoints, OWASP, cómo levantar la app |
| 4 | [04-GUIA_PRUEBAS_POSTMAN.md](04-GUIA_PRUEBAS_POSTMAN.md) | Lista de pruebas Postman con URLs y respuestas esperadas |
| 5 | [05-FRAMEWORKS_Y_DEPENDENCIAS_POM.md](05-FRAMEWORKS_Y_DEPENDENCIAS_POM.md) | Frameworks usados, rol en seguridad y explicación del pom.xml |
| 6 | [06-INVENTARIO_ENDPOINTS.md](06-INVENTARIO_ENDPOINTS.md) | Tabla completa de endpoints publicos y privados |
| 7 | [07-REVISION_COLECCION_POSTMAN.md](07-REVISION_COLECCION_POSTMAN.md) | **Revision de la coleccion Postman** (estado por carpeta, roles, IDOR) |
| — | **[Minimarket.postman_collection.json](Minimarket.postman_collection.json)** | **Coleccion Postman para importar** |

## Inicio rápido

```bash
.\mvnw.cmd spring-boot:run
```

- Público: http://localhost:8080/public/hola  
- Login Postman: `POST http://localhost:8080/login` (form-urlencoded)  
- Usuarios: `cliente1` / `cliente123` | `empleado1` / `empleado123` | `gerente1` / `gerente123`
