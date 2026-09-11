# Entrega 1 — Arquitectura implementada

## Vista física del MVP

```text
┌──────────────────────────────────────┐
│ Frontend Web                         │
│ Astro · localhost:4321               │
└──────────────────┬───────────────────┘
                   │ REST + Bearer JWT
          ┌────────┴─────────┐
          │                  │
          ▼                  ▼
┌───────────────────┐  ┌────────────────────────┐
│ management-service│  │ audit-core-service     │
│ Quarkus · 8081    │  │ Quarkus · 8082         │
│                   │  │                        │
│ Auth              │  │ Dispositivos           │
│ Organizaciones    │  │ Baselines              │
│ Sedes             │  │ Reglas                 │
│ Usuarios / roles  │  │ Configuraciones        │
│ JWT               │  │ Motor de auditoría     │
│                   │  │ Auditorías / hallazgos │
└─────────┬─────────┘  └──────────┬─────────────┘
          │                       │
          └───────────┬───────────┘
                      ▼
             ┌──────────────────┐
             │ PostgreSQL       │
             │ localhost:5432   │
             └──────────────────┘
```

## Relación con la arquitectura lógica de la tesis

Para la Primera Entrega se agrupan responsabilidades lógicas para evitar microservicios vacíos o artificiales:

```text
Auth + B2B                 → management-service
Técnico + Auditoría        → audit-core-service
Resultados + trazabilidad  → dentro de audit-core-service en el MVP
```

La separación lógica sigue estando visible en paquetes, servicios de aplicación, modelos, repositorios y recursos REST. La separación física adicional puede realizarse cuando exista suficiente comportamiento para justificarla.

## Capas utilizadas

Los dos servicios Quarkus mantienen una separación sencilla:

```text
api/rest
   ↓
application
   ↓
domain
   ↑
infrastructure
```

- **API REST:** recibe HTTP, valida datos básicos, aplica roles y transforma DTOs.
- **Aplicación:** coordina casos de uso.
- **Dominio:** modelos y contratos de repositorios.
- **Infraestructura:** JPA/Panache, JWT, BCrypt y trazabilidad temporal.

## Flujo fuerte

```text
DispositivoRed
   ↓
ConfiguracionDispositivo
   ↓
BaselineConfiguracion
   ↓
ReglaBaseline
   ↓
MotorAuditoriaService
   ↓
AuditoriaConfiguracion
   ├── ResultadoReglaAuditoria
   └── HallazgoAuditoria
```

## Seguridad del MVP

- login centralizado en `management-service`;
- contraseña persistida como hash BCrypt;
- JWT HS256 emitido por `management-service`;
- el mismo JWT es validado por `audit-core-service`;
- autorización por roles en endpoints;
- aislamiento por `organizationId` para perfiles no administradores;
- CORS local explícito;
- secretos fuera del repositorio mediante `.env`.

## Persistencia

La Primera Entrega utiliza una misma instancia PostgreSQL. Es una decisión deliberada para el MVP académico: simplifica la demostración y mantiene el DER documentado. La lógica sigue separada por servicios y capas, lo que permite evolucionar la persistencia en futuras entregas.
