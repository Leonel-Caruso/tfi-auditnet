# Bloque 9 — Cierre de la Entrega 2

Decisión: D-44 en `REGISTRO-DECISIONES.md`. Sin migraciones nuevas (management V4, audit-core V7).

## Cambios

- **Pruebas de pantalla en el CI** (`tests/pantallas/`): pytest + Playwright contra el sitio compilado y una API
  simulada. Job nuevo "Pruebas de pantalla" en GitHub Actions; las imágenes solo se publican si pasa.
  El sitio se compila con `npm run build:pruebas` (`.env.pruebas`): llama a la API en el mismo origen, como detrás de
  nginx.
- **Formularios en castellano:** identificador, nombre, tipo de dispositivo, fabricante, organización, sede,
  criticidad, código, condición, severidad, patrón o parámetro, valor esperado, descripción, impacto y recomendación.
- **Estado de los servicios:** `/api/management/status` y `/api/audit-core/status` informan "Entrega 2".
- **README** actualizado (arquitectura en Azure, ejecución local, pruebas, despliegue y documentación).
- **CI:** también corre en `main` (sin desplegar); el despliegue sigue saliendo solo de `entrega-2` con aprobación.

## Qué cubren las pruebas de pantalla (113 casos)

| Archivo | Verifica |
|---|---|
| `test_acceso.py` | Formulario en castellano, logo con la tipografía del ícono, ícono, metadatos para buscadores, manifiesto e íconos de la app, service worker, redirección al login sin sesión |
| `test_pantallas.py` | Cada pantalla con cada perfil: carga sin errores de JavaScript ni diálogos, sin textos técnicos (rutas de API, etiquetas en inglés, referencias CU/RF/RNF, incluidos los diálogos ocultos), sin desborde a 390 px; menú de administración solo para el administrador; HTML de los datos mostrado como texto |
| `test_paginacion.py` | Hallazgos (25 por página, métricas sobre el total, cambio de tamaño, búsqueda, `?hallazgo=`), foco del teclado, historial pedido al servidor por página, bitácoras |

## Prueba de carga y SEO

Se ejecutan sobre el sitio de Azure con los procedimientos de `CARGA.md` y `SEO.md`; los resultados se completan en
las tablas de esos documentos.

## Cierre

1. Push del bloque, CI en verde (5 jobs) y despliegue aprobado.
2. Tag `entrega-2-stable` sobre ese commit.
3. Unión de `entrega-2` en `main` por avance rápido (`main` es antecesor directo; no hay conflictos). Antes, verificar
   en Render que los servicios de la Entrega 1 estén suspendidos o sin despliegue automático, porque `render.yaml`
   despliega desde `main`.
4. El desarrollo sigue en `entrega-2` (el entorno `azure` y la credencial OIDC están configurados para esa rama).

## Casos de prueba manuales

| CP | Pasos | Resultado esperado |
|---|---|---|
| CP-B9-01 | Push a `entrega-2` | 5 jobs: dos de tests, frontend, pantallas e imágenes en verde; el despliegue espera aprobación |
| CP-B9-02 | Actions → ejecución → job "Pruebas de pantalla" | 113 passed |
| CP-B9-03 | Dispositivos → Registrar dispositivo; Baselines → Crear; Reglas → Crear | Todas las etiquetas en castellano |
| CP-B9-04 | `curl https://<sitio>/api/management/status` | `"delivery":"Entrega 2"` |
| CP-B9-05 | Prueba de carga (`CARGA.md`) | Todos los criterios en OK |
| CP-B9-06 | SEO Optimer (`SEO.md`) | Informe guardado con la nota |
| CP-B9-07 | GitHub → Tags | `entrega-1-stable` y `entrega-2-stable` |
| CP-B9-08 | GitHub → rama `main` | Muestra el README nuevo; Actions ofrece "Run workflow" |
