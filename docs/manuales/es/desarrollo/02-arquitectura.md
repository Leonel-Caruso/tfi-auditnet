## Arquitectura

```text
Navegador ──HTTPS──► web (nginx: sitio Astro + gateway de la API)
                      ├─ /api/auth, users, roles, client-organizations, sites,
                      │  plans, admin ──────────────────────────► management-service
                      └─ el resto de /api ──────────────────────► audit-core-service

management-service ──┬─► PostgreSQL (datos, bitácoras, bandeja de salida de correo)
audit-core-service ──┤
                     └─► MongoDB / Cosmos DB (bitácora de excepciones)
audit-core-service ────► Blob Storage (configuraciones, reportes y respaldos)
management-service ────► Azure Communication Services (correo; Mailpit en local)
Logic App (6:00) ──────► POST /internal/review/run (revisión periódica)
```

### Componentes

| Componente | Tecnología | Responsabilidad |
|---|---|---|
| web | nginx + Astro 5 (sitio estático) | Sirve la consola y enruta `/api/...` a cada servicio en el mismo origen: los servicios no se exponen y no hace falta CORS. Publica la política de seguridad de contenido. |
| management-service (puerto 8081) | Java 21, Quarkus 3 | Acceso y JWT, usuarios y perfiles, organizaciones y sedes, planes, correo, bitácora de sistema. |
| audit-core-service (puerto 8082) | Java 21, Quarkus 3 | Dispositivos, configuraciones, baselines y reglas, motor de auditoría, hallazgos, alertas, tablero, reportes, revisión periódica, archivos y respaldos. |
| PostgreSQL 17 | Flyway por servicio | Una base compartida; cada servicio migra sus tablas con su propia tabla de historial. |
| MongoDB / Cosmos DB | API de MongoDB 4.2 | Bitácora de excepciones (la entidad NoSQL). |
| Blob Storage / Azurite | contenedor privado | Archivos con su SHA-256. |

### Decisiones clave

- **Dos servicios y una base.** Se separan por responsabilidad (acceso y administración por un lado, auditoría por
  otro) y comparten PostgreSQL para que las consultas de alcance (organización, plan) sean simples. audit-core lee el
  plan con una consulta nativa; por eso management migra primero.
- **Seguridad.** El login devuelve un JWT firmado (HS256) con el usuario, su organización y sus perfiles. Cada endpoint
  declara los perfiles permitidos con `@RolesAllowed` y cada servicio aplica el alcance por organización. Cambiar el
  perfil, la organización o la contraseña invalida las sesiones abiertas.
- **Tres bitácoras.** Sistema (accesos y seguridad) y transacciones (valor anterior y nuevo de cada operación de
  negocio) en PostgreSQL; excepciones en MongoDB con su nivel configurable. Un código de seguimiento (`X-Request-ID`,
  lo pone nginx) relaciona una misma operación en las tres.
- **Evidencia inalterable.** Configuraciones, auditorías, seguimientos de hallazgos y reportes no se modifican: hay
  triggers de solo inserción y los reportes son una foto fija.
- **Correo con bandeja de salida.** Los avisos se guardan en `correos_salientes` dentro de la misma transacción del
  hecho que los origina y se despachan después, con reintentos. Así un fallo del proveedor no pierde avisos.
- **Multi idioma.** La consola traduce en el navegador con diccionarios; los mensajes de error se traducen con un
  catálogo según `Accept-Language`; los correos usan el idioma del destinatario.
