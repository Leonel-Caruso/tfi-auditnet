# Trazabilidad documental - Bloque 3

| Documento / concepto | Implementación del Bloque 3 | Estado |
|---|---|---|
| Sector Gestión B2B y Seguridad | `backend/management-service` | Parcial E1 |
| MS Autenticación | paquetes `application/auth`, `infrastructure/security`, `AuthResource` | Implementado en forma compacta dentro de `management-service` |
| MS B2B | paquetes `application/b2b`, repositorios y recursos de organizaciones/sedes | Implementado en forma compacta dentro de `management-service` |
| CU-001-001 Iniciar sesión | `POST /api/auth/login` | Implementado |
| Contraseña protegida/hasheada | `PasswordPort` + `BcryptPasswordAdapter` | Implementado |
| JWT + roles | `JwtPort` + `JwtTokenAdapter`, claim `groups` | Implementado |
| CU-001-002 Usuarios | `GET/POST /api/users` | Parcial |
| CU-001-002 Roles | `GET/POST /api/roles` | Parcial |
| CU-007-001 Organizaciones | `GET/POST /api/client-organizations`, `GET /{id}` | Implementado para alta/consulta |
| CU-007-002 Sedes | `GET/POST` por organización, `GET /api/sites/{id}` | Parcial |
| DER organizaciones_cliente | `OrganizacionClienteEntity` | Implementado |
| DER sedes | `SedeEntity` | Implementado |
| DER usuarios | `UsuarioEntity` | Implementado |
| DER roles | `RolEntity` | Implementado |
| DER usuarios_roles | `@ManyToMany` con join table `usuarios_roles` | Implementado |
| Bitácora de actividad | `TrazabilidadPort` + log estructurado temporal | Pendiente persistencia en MS Trazabilidad |

## Decisiones de implementación de Entrega 1

1. Se mantiene PostgreSQL único porque el DER y el despliegue de la tesis plantean una base relacional principal y no exigen database-per-service.
2. Se mantiene Panache en estilo Repository para conservar la separación servicio/repositorio expresada en el diseño.
3. `management-service` agrupa temporalmente los componentes lógicos Autenticación y B2B del sector Gestión B2B y Seguridad. La separación interna evita que esta simplificación obligue a reescribir el dominio si luego se decide desplegarlos por separado.
4. Los permisos granulares no se inventan en este bloque: se implementa primero RBAC por roles con los actores explícitamente documentados.
5. La bitácora persistente no se coloca en este servicio porque la tesis asigna esa responsabilidad al componente de Trazabilidad. Se utiliza un puerto para dejar preparada la integración posterior.
