# Trazabilidad — Bloque 5 — Dispositivos

## Objetivo

Implementar el primer caso de uso de negocio visible de la Primera Entrega: el inventario de dispositivos de red que conforman el universo auditable de la plataforma.

Este bloque materializa principalmente:

- **CU-002-001 Registrar dispositivo de red**.
- **CU-002-002 Consultar inventario de dispositivos**.
- **RF-003 Gestión de dispositivos**.
- **RF-004 Consulta de inventario**.

## Correspondencia con la tesis

El dispositivo registrado conserva los datos definidos para el caso de uso:

- nombre;
- identificador único;
- tipo de dispositivo;
- fabricante;
- organización;
- sede opcional;
- criticidad operativa;
- estado.

El alcance inicial continúa orientado a **Cisco IOS / IOS-XE**, routers y switches de capa 3, sin conexión automática a equipos reales.

## Backend

Responsabilidad lógica de activos de red implementada dentro de `audit-core-service` para la Primera Entrega.

### Endpoints

- `GET /api/device-types`
- `GET /api/devices`
- `GET /api/devices/{id}`
- `POST /api/devices`

### Seguridad

El `audit-core-service` valida los mismos JWT HS256 emitidos por `management-service`.

- Consulta: roles autenticados de la plataforma.
- Alta de dispositivo: `ADMINISTRADOR_SISTEMA`.

### Reglas implementadas

1. El identificador es obligatorio y se normaliza a mayúsculas.
2. El identificador debe ser único.
3. El tipo de dispositivo debe existir y estar activo.
4. La organización debe existir.
5. Si se informa una sede, debe existir y pertenecer a la organización indicada.
6. La criticidad debe ser `BAJA`, `MEDIA`, `ALTA` o `CRITICA`.
7. Todo nuevo dispositivo inicia en estado `ACTIVO`.
8. El alta genera un evento temporal de trazabilidad estructurada en log.

## Persistencia

Nueva tabla principal:

`dispositivos_red`

Relaciones:

```text
organizaciones_cliente 1 ─── N dispositivos_red
sedes                  1 ─── N dispositivos_red
tipos_dispositivo      1 ─── N dispositivos_red
```

Se mantiene la base PostgreSQL compartida del MVP académico acordada para la Primera Entrega.

## Frontend

La página `/devices` deja de ser placeholder y pasa a implementar:

- inventario técnico;
- búsqueda rápida;
- filtro por criticidad;
- alta mediante formulario modal;
- selección de organización y sede;
- selección de tipo de dispositivo;
- detalle del activo mediante inspector lateral;
- consulta real de `GET /api/devices/{id}`;
- estética Command Rail / consola definida en Bloque 4.2.

## Decisiones deliberadamente diferidas

Para mantener el alcance de la Primera Entrega enfocado en los casos de uso principales, este bloque no incorpora todavía:

- edición completa de dispositivos;
- baja lógica desde interfaz;
- fecha de última auditoría;
- filtros avanzados server-side;
- descubrimiento automático de equipos;
- conexión SSH/Telnet/SNMP con dispositivos reales;
- bitácora persistente definitiva.

Estas decisiones no impiden demostrar los casos de uso de registrar y consultar inventario, ni continuar con Baselines y Reglas.
