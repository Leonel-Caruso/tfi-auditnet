# Entrega 1 · Bloque 4 — Astro + Site Master + Login integrado

## Objetivo

El Bloque 4 incorpora el frontend base de la Primera Entrega sin modificar el núcleo funcional validado en los bloques anteriores. Su objetivo es cumplir la base visual y de navegación indicada por la cátedra: arquitectura base, Site Master y un punto de entrada integrado sobre el cual se implementarán los casos de uso de negocio fuertes.

## Decisiones

### Astro sin framework UI adicional

El frontend utiliza Astro con HTML, CSS y TypeScript/JavaScript simple. No se agrega React, Vue, Tailwind ni una librería de componentes porque no son necesarios para el alcance académico de la Primera Entrega y aumentarían complejidad sin aportar al dominio.

### Site Master

`MasterLayout.astro` define la estructura reutilizable de la aplicación:

- sidebar principal;
- topbar con usuario, rol y cierre de sesión;
- encabezado contextual de cada módulo;
- área de contenido;
- protección de sesión antes de mostrar la interfaz.

La navegación ya reserva ubicaciones estables para Dispositivos, Baselines, Reglas, Auditorías y Resultados. Los siguientes bloques completan esos módulos sin rehacer el layout.

### Integración real con management-service

El login del frontend consume `POST /api/auth/login`. El JWT recibido se utiliza para `GET /api/auth/me` y para los endpoints protegidos. También se integran de manera visual las funcionalidades ya construidas y validadas del Bloque 3:

- organizaciones;
- sedes;
- usuarios;
- roles implícitos en la sesión.

### Sesión del MVP

Para la Primera Entrega el JWT se conserva en `sessionStorage` y se elimina al cerrar la pestaña/sesión del navegador o al presionar Salir. Es una decisión acotada al MVP académico. Una solución productiva podría evolucionar hacia cookie segura HttpOnly/BFF o el API Gateway definitivo.

### CORS

Durante desarrollo Astro corre en `http://localhost:4321` y `management-service` en `http://localhost:8081`. Se habilita CORS de Quarkus únicamente para el origen local configurado mediante `FRONTEND_ORIGIN`.

### API Gateway

La arquitectura documentada de la tesis contiene un API Gateway. En este bloque Astro consume directamente `management-service` como integración temporal de la Entrega 1. La URL está encapsulada en `PUBLIC_MANAGEMENT_API_URL`, de modo que el frontend pueda apuntar posteriormente al Gateway sin reescribir los componentes.

## Trazabilidad con la Primera Entrega

| Requisito de cátedra | Bloque 4 |
|---|---|
| Arquitectura base | Backend Quarkus + PostgreSQL + frontend Astro integrados |
| Site Master | `MasterLayout.astro`, Sidebar y Topbar |
| Acceso al sistema | Login real + BCrypt + JWT + roles |
| CU negocio | El Site Master deja ubicaciones definitivas para los 3 CU elegidos |
| CU fuerte | Baseline/Reglas y Ejecutar Auditoría se implementan en Bloques 6 y 7 |

## Casos de uso objetivo de la Primera Entrega

1. Gestionar dispositivos — Bloque 5.
2. Gestionar baseline y reglas — CU fuerte — Bloque 6.
3. Ejecutar auditoría y generar hallazgos — CU fuerte — Bloque 7.

El Bloque 4 no declara estos casos como terminados. Construye la interfaz base, seguridad e integración necesarias para implementarlos de forma acumulativa.
