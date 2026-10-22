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

1. En el panel ADM → Usuarios, crear un usuario de prueba con perfil **Responsable de gestión IT** (solo consulta)
   en la organización demo. No usar la cuenta del administrador.
2. Recomendado: `bash 07-modo-demo.sh on` (mide el sistema ya encendido) y `off` al terminar.
3. Azure Cloud Shell (Bash). Cloud Shell no guarda archivos entre sesiones: se vuelve a clonar y k6 se descarga como
   binario, sin instalar nada:

```bash
[ -d ~/tfi-auditnet ] || git clone -b entrega-2 https://github.com/Leonel-Caruso/tfi-auditnet.git ~/tfi-auditnet
cd ~/tfi-auditnet && git pull
curl -sSL https://github.com/grafana/k6/releases/download/v0.54.0/k6-v0.54.0-linux-amd64.tar.gz | tar xz -C ~
read -s -p "Contraseña del usuario de prueba: " CLAVE_CARGA; echo
~/k6-v0.54.0-linux-amd64/k6 run -e BASE_URL=https://<sitio> -e USUARIO=<usuario-de-prueba> -e CLAVE="$CLAVE_CARGA" tests/carga/auditnet-carga.js
```

   En una PC con k6 instalado (<https://grafana.com/docs/k6/latest/set-up/install-k6/>) el comando `k6 run` es el
   mismo, desde la raíz del repositorio.

Opcionales: `-e VUS=20` (más usuarios), `-e DURACION=3m`. Al terminar muestra un resumen en castellano y guarda el
detalle en `resultado-carga.json` (ignorado por Git). El código de salida es 0 si se cumplen todos los criterios.
Si el calentamiento o el inicio de sesión fallan, no se mide nada: los criterios aparecen como **SIN MEDICIÓN** y el
error queda impreso debajo del resumen (por ejemplo `No se pudo iniciar sesión (HTTP 401)`).

**Costo:** unas 5 solicitudes por segundo durante 2 minutos: muy por debajo de la cuota gratuita de Container Apps
(2 millones de solicitudes por mes). Es una prueba de capacidad del prototipo (1 réplica por servicio, PostgreSQL
B1ms), no de la arquitectura escalada.

## Resultados

| Fecha | Entorno | Usuarios | Solicitudes | Errores | p95 total | p95 tablero | p95 hallazgos | Criterios |
|---|---|---|---|---|---|---|---|---|
| 22/10/2026 | Azure, `sha-c1b0f00…`, modo demo encendido | 10 | 279 (incluye calentamiento) | 0 % | 1239 ms | 1322 ms | 1235 ms | Todos OK |

Detalle de la ejecución (1 min 45 s, desde Azure Cloud Shell):

| Medida | p95 | Promedio | Máximo |
|---|---|---|---|
| Todas las solicitudes medidas | 1239 ms | 1224 ms | 2413 ms |
| Tablero | 1322 ms | — | 2413 ms |
| Hallazgos pendientes (página 1) | 1235 ms | — | — |
| Alertas críticas | 1232 ms | — | — |
| Historial (página 1) | 1232 ms | — | — |

Chequeos correctos: 100 %. Iteraciones: 68 completas y 1 interrumpida.

Observaciones:

- Los cuatro pasos tardan casi lo mismo (≈ 1,2 s), incluso alertas, que es la consulta más liviana. Eso indica un
  costo fijo de red y no de base de datos: Cloud Shell corre en otra región y cada pedido recorre la entrada de
  Container Apps, nginx y el servicio interno en Brazil South. El tiempo de las consultas en sí es una fracción menor.
- El máximo de 2,4 s del tablero es un pico aislado (el tablero hace varias consultas y agregaciones); el p95 queda
  en 1,3 s, por debajo de su criterio de 2 s.
- La iteración interrumpida es la que estaba en curso al terminar la bajada de 15 s; no es un error.
- El margen contra el criterio general (1,5 s) es de unos 260 ms. Para separar la red del tiempo propio del sistema
  habría que ejecutar la prueba desde Brazil South (por ejemplo, una VM temporal); no se hizo para no generar costo.
- Es una prueba del prototipo con 1 réplica por servicio y PostgreSQL B1ms. Con más usuarios, Container Apps puede
  agregar réplicas, pero eso tiene impacto financiero y queda fuera del alcance de esta entrega.
