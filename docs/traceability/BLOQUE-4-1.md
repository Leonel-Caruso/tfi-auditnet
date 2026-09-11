# Entrega 1 · Bloque 4.1 — Cierre visual y textual

## Objetivo

Consolidar la identidad visual antes de implementar los casos de uso de negocio. No se modifica el backend ni la arquitectura funcional validada: el cambio se concentra en experiencia de usuario, terminología y presentación del frontend Astro.

## Criterio de diseño

La interfaz deja de presentarse como un tablero de seguimiento de desarrollo y pasa a presentarse como un producto de auditoría técnica de redes. Se adopta una estética profesional inspirada en consolas de operación y herramientas de ciberseguridad, sin convertir la aplicación en una terminal oscura difícil de leer.

Principios aplicados:

- naranja como acento funcional;
- grises de consola y código como base;
- tipografía monospace sólo para estados, etiquetas técnicas y fragmentos de flujo;
- menos botones y menos elementos visuales compitiendo entre sí;
- tarjetas y paneles sobrios, con bordes y acentos lineales;
- textos orientados al usuario y al dominio de redes;
- referencias a bloques, entregas y Site Master retiradas de la UI visible.

## Terminología visible

La navegación se organiza en:

- Control de acceso: Organizaciones y sedes, Usuarios.
- Operación técnica: Dispositivos, Baselines, Reglas, Auditorías, Hallazgos.

El dashboard se denomina consola/resumen operativo y muestra datos reales ya integrados con `management-service`.

## Sin cambios de backend

El Bloque 4.1 no cambia:

- endpoints REST;
- JWT;
- BCrypt;
- autorización por roles;
- persistencia PostgreSQL;
- dominio o repositorios.

La integración existente sigue siendo Astro → REST → `management-service` → PostgreSQL.
