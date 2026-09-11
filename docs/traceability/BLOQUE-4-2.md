# Entrega 1 · Bloque 4.2 — Command Rail + Console Header

## Objetivo

El Bloque 4.2 conserva la lógica funcional y de seguridad validada en el Bloque 4.1 y modifica exclusivamente la experiencia de navegación del frontend para darle una identidad propia a AuditConfig.

La decisión principal es reemplazar el sidebar administrativo tradicional por una navegación compacta inspirada en herramientas técnicas, IDEs y consolas de infraestructura.

## Cambios visuales

### Command Rail

La navegación lateral se reduce a una barra de comandos de aproximadamente 72 px. Cada módulo utiliza un código corto y estable:

- `~/` Inicio;
- `ORG` Organizaciones y sedes;
- `USR` Usuarios;
- `DEV` Dispositivos;
- `BL` Baselines;
- `RUL` Reglas;
- `RUN` Auditorías;
- `LOG` Hallazgos.

El módulo activo se identifica mediante acento naranja y una línea vertical. Los nombres completos aparecen como ayuda contextual al pasar el cursor, evitando ocupar ancho permanente de pantalla.

### Console Header

El encabezado superior se reorganiza en tres zonas:

1. breadcrumb técnico, por ejemplo `AuditConfig / organizations`;
2. comando rápido `> ir a módulo...`;
3. estado de API y sesión de usuario compacta.

El campo de comando permite navegar escribiendo términos como `dispositivos`, `baseline`, `auditorias`, `ORG`, `RUN` o `LOG` y presionando Enter.

### Usuario compacto

La identidad del usuario deja de ocupar un bloque permanente. El encabezado muestra el usuario de manera resumida y despliega nombre, rol y acción de cierre de sesión en un popover.

## Alcance técnico

No se modifican:

- endpoints REST;
- JWT;
- BCrypt;
- roles;
- persistencia;
- entidades de dominio;
- repositorios;
- servicios de aplicación;
- PostgreSQL.

El cambio afecta únicamente componentes Astro, layout y CSS.

## Motivo académico

El nuevo esquema conserva el Site Master requerido por la cátedra, pero evita una apariencia genérica de panel administrativo. La navegación refleja mejor el dominio del proyecto: configuración de redes, auditoría técnica, operación por comandos y trazabilidad.
