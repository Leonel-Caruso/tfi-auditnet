# AuditNet

**Plataforma de auditoría inteligente de configuraciones para redes** · Trabajo Final de Ingeniería en Sistemas
Informáticos (UAI) · Leonel Caruso

AuditNet audita configuraciones de equipos Cisco IOS/IOS-XE contra una configuración esperada (baseline), detecta
desvíos, los prioriza por severidad y criticidad del dispositivo, genera alertas y reportes, y conserva la evidencia
de cada auditoría. Es multi organización: cada cliente ve solo sus datos.

- Sitio: https://ca-auditnet-web.niceflower-73585a17.brazilsouth.azurecontainerapps.io
- Rama de trabajo: `entrega-2`

## Arquitectura

```text
Navegador ──HTTPS──► web (nginx: sitio Astro + API Gateway)
                         ├── /api/auth, users, roles, organizaciones, sedes, admin ──► management-service (Quarkus)
                         └── resto de /api ─────────────────────────────────────────► audit-core-service (Quarkus)

management-service y audit-core-service ──► PostgreSQL (datos y bitácoras de transacciones y de sistema)
                                        ──► MongoDB / Cosmos DB (bitácora de excepciones)
audit-core-service ──────────────────────► Blob Storage (configuraciones, reportes y respaldos)
```

| Capa | Tecnología |
|---|---|
| Backend | Java 21, Quarkus 3, Hibernate ORM con Panache, SmallRye JWT, Flyway |
| Frontend | Astro 5 + TypeScript, app instalable (PWA) |
| Gateway | nginx |
| Datos | PostgreSQL 17, Cosmos DB para MongoDB, Azure Blob Storage |
| Nube | Azure Container Apps (scripts en `infra/azure`) |
| CI/CD | GitHub Actions: tests, pruebas de pantalla, imágenes en GHCR y despliegue con aprobación (OIDC) |

## Ejecución local

Requisitos: Docker, Java 21, Maven, Node 24 y PowerShell.

1. Copiar `.env.example` a `.env` y completar los valores locales.
2. Bases de datos y emulador de Blob Storage:

   ```powershell
   .\scripts\load-env.ps1
   docker compose --env-file .env -f docker/compose.database.yaml up -d
   ```

3. management-service (primero: aplica sus migraciones):

   ```powershell
   .\scripts\load-env.ps1
   cd .\backend\management-service
   mvn quarkus:dev
   ```

4. audit-core-service, en otra terminal:

   ```powershell
   .\scripts\load-env.ps1
   cd .\backend\audit-core-service
   mvn quarkus:dev
   ```

5. Frontend, en otra terminal:

   ```powershell
   cd .\frontend\web-app
   npm install
   npm run dev -- --host localhost
   ```

Abrir `http://localhost:4321`. El usuario administrador inicial se crea con los datos `BOOTSTRAP_*` del `.env`.

## Pruebas

```powershell
# Tests de cada servicio
cd .\backend\management-service; mvn test
cd ..\audit-core-service; mvn test

# Pruebas de pantalla (ver tests/pantallas/README.md)
cd ..\..\frontend\web-app; npm run build:pruebas; cd ..\..
python -m pip install -r tests\pantallas\requirements.txt
python -m playwright install chromium
python -m pytest tests\pantallas
```

Prueba de carga con k6: `docs/entrega-2/CARGA.md`.

## Despliegue

Cada push a `entrega-2` corre el CI; el despliegue a Azure espera la aprobación del entorno `azure` en GitHub Actions.
Creación de la infraestructura y operación: `docs/deploy/AZURE.md`.

## Documentación

| Tema | Ubicación |
|---|---|
| Decisiones de diseño | `docs/entrega-2/REGISTRO-DECISIONES.md` |
| Detalle por bloque y casos de prueba | `docs/entrega-2/BLOQUE-*.md` |
| Plan de los próximos bloques | `docs/entrega-3/PLAN-BLOQUES.md` |
| Migraciones y respaldos | `docs/database/` |
| Despliegue en Azure | `docs/deploy/AZURE.md` |
| Entrega 1 (Render, histórico) | `docs/ENTREGA-1-CHECKLIST.md`, `docs/deploy/RENDER.md` |

## Secretos

El `.env` real está ignorado por Git. No se publican contraseñas, tokens, cadenas de conexión ni claves: en Azure se
guardan como secretos de Container Apps y GitHub entra a Azure por OIDC, sin contraseñas.
