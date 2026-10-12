# Prueba de carga (k6) — Bloque 8

Punto extra "utilizar alguna herramienta para pruebas de stress" y evidencia de RNF-005 (respuesta aceptable en
operaciones principales). Decisión D-42. Script: `tests/carga/auditnet-carga.js`.

## Qué mide

Usuarios simultáneos que recorren la consola **solo leyendo** (no modifica datos):

| Paso | Solicitud | Criterio (p95) |
|---|---|---|
| Tablero | `GET /api/dashboard/summary` | < 2000 ms |
| Hallazgos pendientes, página 1 | `GET /api/findings?estado=PENDIENTES&pagina=1&porPagina=25` | < 1500 ms |
| Alertas críticas | `GET /api/alerts/critical` | — |
| Historial, página 1 | `GET /api/audits/history?pagina=1&porPagina=25` | — |
| Todas | | < 1500 ms, errores < 1 %, chequeos > 99 % |

Perfil de carga: 30 s de subida hasta 10 usuarios, 1 minuto estable y 15 s de bajada; cada usuario espera entre 1 y
4 segundos entre pantallas, como una persona. Inicia sesión **una sola vez** (así no se activa el bloqueo por
intentos) y antes de medir espera a que las Container Apps salgan de cero réplicas. Los criterios solo cuentan las
solicitudes de la medición (etiqueta `fase:medicion`): el calentamiento y el login quedan afuera.

## Cómo ejecutarla

1. Instalar k6: <https://grafana.com/docs/k6/latest/set-up/install-k6/> (en Windows: `winget install k6 --source winget`).
2. En el panel ADM → Usuarios, crear un usuario de prueba con perfil **Responsable de gestión IT** (solo consulta)
   en la organización demo. No usar la cuenta del administrador.
3. Recomendado: `bash 07-modo-demo.sh on` en Cloud Shell (mide el sistema ya encendido) y `off` al terminar.
4. PowerShell, en la raíz del repositorio:

```powershell
k6 run -e BASE_URL=https://<sitio> -e USUARIO=<usuario-de-prueba> -e CLAVE=<VALOR_CONFIGURADO_EN_ENV> tests/carga/auditnet-carga.js
```

Opcionales: `-e VUS=20` (más usuarios), `-e DURACION=3m`. Al terminar muestra un resumen en castellano y guarda el
detalle en `resultado-carga.json` (ignorado por Git). El código de salida es 0 si se cumplen todos los criterios.

**Costo:** unas 5 solicitudes por segundo durante 2 minutos: muy por debajo de la cuota gratuita de Container Apps
(2 millones de solicitudes por mes). Es una prueba de capacidad del prototipo (1 réplica por servicio, PostgreSQL
B1ms), no de la arquitectura escalada.

## Resultados (completar después de ejecutarla)

| Fecha | Entorno | Usuarios | Solicitudes | Errores | p95 total | p95 tablero | p95 hallazgos | Criterios |
|---|---|---|---|---|---|---|---|---|
| | Azure (`sha-…`) | 10 | | | | | | |

Observaciones (cuello de botella, primera solicitud lenta por arranque en frío, etc.):

-
