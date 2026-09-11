# AuditNet — TFI — Entrega 1

**Plataforma de auditoria inteligente de configuraciones para redes**

Esta es la version estable de la Primera Entrega preparada para ejecucion local y deploy
cloud. Conserva el flujo funcional validado de los Bloques 1 a 8 y agrega solamente la
configuracion necesaria para publicarlo sin versionar secretos.

## Flujo demostrado

```text
usuario autenticado
    ↓
organizacion / sede
    ↓
dispositivo de red
    ↓
baseline + reglas
    ↓
configuracion versionada
    ↓
auditoria automatica
    ↓
evaluacion regla por regla
    ↓
hallazgos trazables
```

## Arquitectura de la Entrega 1

```text
Astro frontend
   ├── management-service (Quarkus)
   └── audit-core-service (Quarkus)
                 ↓
             PostgreSQL
```

## Ejecucion local

1. Copiar `.env.example` a `.env` y completar los valores locales.
2. Desde la raiz:

```powershell
.\scripts\load-env.ps1
docker compose --env-file .env -f docker/compose.database.yaml up -d
```

3. `management-service`:

```powershell
.\scripts\load-env.ps1
cd .\backend\management-service
mvn quarkus:dev
```

4. `audit-core-service`:

```powershell
.\scripts\load-env.ps1
cd .\backend\audit-core-service
mvn quarkus:dev
```

5. Frontend:

```powershell
cd .\frontend\web-app
npm install
npm run dev -- --host localhost
```

Abrir `http://localhost:4321`.

## Regresion local

```powershell
.\scripts\test-entrega1.ps1
```

## Deploy

La version cloud usa Docker para ambos servicios Quarkus y un Blueprint de Render:

```text
render.yaml
backend/management-service/Dockerfile
backend/audit-core-service/Dockerfile
docs/deploy/RENDER.md
```

Seguir `docs/deploy/RENDER.md` antes de crear recursos cloud.

## Secretos

El archivo `.env` real esta ignorado por Git y no se incluye en esta distribucion.
No publicar contrasenas, JWT, credenciales de PostgreSQL ni tokens dentro del repositorio.

## Documentacion de Entrega 1

```text
docs/ENTREGA-1-CHECKLIST.md
docs/architecture/ENTREGA-1-ARQUITECTURA.md
docs/demo/GUIA-DEMO-ENTREGA-1.md
docs/traceability/BLOQUE-8.md
```
