# Bloque 8 — Pulido, paginación, despliegue continuo, SSL, PWA, SEO y prueba de carga

Cubre: punto mandatorio "certificado SSL en el dominio del sitio publicado", puntos extra "CI/CD", "app responsive +
PWA", "herramienta para pruebas de stress" y "evaluación SEO Optimer"; RNF-004 (interfaz clara), RNF-005
(rendimiento) y RNF-008 (disponibilidad web). Decisiones: D-37 a D-43 en `REGISTRO-DECISIONES.md`.
Sin migraciones nuevas (management sigue en V4 y audit-core en V7).

## Cambios en la API (audit-core-service)

| Método y ruta | Cambio |
|---|---|
| `GET /api/findings` | Parámetros opcionales `pagina` (desde 1) y `porPagina` (1 a 100, por defecto 25). Sin ellos devuelve todo, como antes |
| `GET /api/audits/history` | Ídem |

- En ambos casos el encabezado `X-Total-Count` informa el total que cumple los filtros (también sin paginar).
- La prioridad de los hallazgos se calcula sobre el total y recién después se corta la página: el orden es el mismo con
  o sin paginar. Valores inválidos (`pagina=0`, `porPagina=101`, texto) responden 400.
- `X-Total-Count` se agregó a los encabezados expuestos por CORS (para el modo local con puertos distintos).

Ejemplo (F12, con la sesión iniciada en local):

```js
fetch('http://localhost:8082/api/audits/history?pagina=1&porPagina=5', {headers: {Authorization: 'Bearer ' + sessionStorage.getItem('tfi.auth.token')}})
  .then(r => console.log(r.status, r.headers.get('X-Total-Count')))
```

## Pantallas

- **Paginador** (25 por página; 10, 25, 50 o 100 a elección) en Hallazgos, Historial y Bitácoras. El historial pide
  cada página al servidor; Hallazgos y Bitácoras paginan en el navegador porque las métricas, la búsqueda rápida y el
  "límite" de bitácoras trabajan sobre todo el resultado (D-39). Con `?hallazgo=ID` se abre la página donde está.
  En el historial, `?auditoria=ID` (desde Hallazgos o Alertas) abre el detalle aunque la fila esté en otra página.
- **Descripciones de pantalla** reescritas en lenguaje simple y sin referencias a la especificación (CU, RF, RNF);
  esas referencias quedan solo en la documentación. Los comentarios del código se acortaron con el mismo criterio.
- **Textos para el usuario:** se quitaron las rutas de API visibles ("GET /api/…") y las etiquetas en inglés
  (`rule.code`, `executed.by`, `normalized.preview`, `select finding;`, `mi_cuenta`, etc.). El login dice "usuario o
  correo" y "contraseña"; la barra superior muestra el perfil con su nombre ("Administrador del sistema").
- **Marca:** ícono nuevo, "A_" blanca en JetBrains Mono ExtraBold (licencia OFL) convertida a trazos sobre el
  cuadrado naranja. La misma tipografía se usa en el logo animado del login y en el "A_" del menú.
- **App instalable (PWA):** manifiesto, íconos 192/512 y *maskable*, service worker mínimo y página "Sin conexión".
  En Chrome/Edge aparece "Instalar AuditNet" en la barra de direcciones; en el celular, "Agregar a la pantalla de
  inicio".
- **SEO:** solo la pantalla de acceso se ofrece a los buscadores (descripción, Open Graph, canonical, datos
  estructurados `WebApplication`); la consola lleva `noindex`. `robots.txt` y `sitemap.xml` los genera nginx con el
  dominio real. La raíz `/` redirige a `/login`.

## Infraestructura y despliegue

| Archivo | Qué hace |
|---|---|
| `infra/azure/variables.sh` | Función `registrar_proveedores` (evita el error `SubscriptionNotFound` del Bloque 7) y datos de GitHub |
| `01`, `04`, `05` | Registran solos el proveedor que necesitan (PostgreSQL/App/Log Analytics, DocumentDB, Storage) |
| `06-configurar-github-oidc.sh` | Identidad administrada + credencial federada OIDC + permisos mínimos; imprime los 3 valores para GitHub |
| `07-modo-demo.sh on/off/estado` | 1 réplica fija para una demo (sin espera de arranque) y vuelta a 0 |
| `.github/workflows/imagenes.yml` | Job nuevo **Desplegar en Azure (con aprobación)** después de publicar las imágenes |
| `nginx/default.conf.template` | HSTS, gzip (sin `/api/`), `/api/` con `no-store`, `robots.txt`, `sitemap.xml`, tipo del manifiesto, caché de fuentes e íconos |

Flujo después de un push a `entrega-2`: tests → imágenes → **espera aprobación** en Actions → `az containerapp update`
con `sha-<commit>` → prueba de humo (imagen y revisión nuevas en las 3 apps, `/login`, `/api/management/status`,
`/api/audit-core/status` y encabezado HSTS).
Hasta cargar las variables `AZURE_*` el job se omite y el CI sigue en verde. `03-actualizar-imagenes.sh` se puede
seguir usando a mano desde Cloud Shell.

### Configuración en GitHub (una sola vez)

1. Cloud Shell: `bash 06-configurar-github-oidc.sh` → anota `AZURE_CLIENT_ID`, `AZURE_TENANT_ID` y
   `AZURE_SUBSCRIPTION_ID` (identificadores, no contraseñas).
2. GitHub → *Settings → Environments → New environment* → `azure` → *Required reviewers*: tu usuario → *Save
   protection rules*. En *Deployment branches and tags* elegir *Selected branches* y agregar `entrega-2`.
3. GitHub → *Settings → Secrets and variables → Actions → Variables → New repository variable* con los 3 valores.
4. En el próximo push, *Actions → la ejecución → Review deployments → azure → Approve and deploy*.

Los revisores obligatorios solo existen en repositorios **públicos** (en uno privado requieren GitHub Enterprise; sin
ellos el job desplegaría sin esperar). El repositorio de la tesis es público.

GitHub envía el sujeto del token con los ids numéricos del dueño y del repositorio
(`repo:Leonel-Caruso@111381248/tfi-auditnet@1366741898:environment:azure`). El script 06 registra ese formato y el
de solo nombres; si el login falla con `AADSTS700213`, comparar el *subject claim* del log con `REPO_GITHUB_IDS`.

Si el primer despliegue falla con `AuthorizationFailed` sobre `cae-auditnet` (acción
`Microsoft.App/managedEnvironments/join/action`), asignar a la identidad el rol *Contributor* también sobre el entorno
de Container Apps y volver a ejecutar el job.

## SSL / HTTPS (punto mandatorio)

El sitio publicado `https://ca-auditnet-web.<dominio>.brazilsouth.azurecontainerapps.io` usa el certificado que
Azure administra y renueva para `*.azurecontainerapps.io` (sin costo). El ingress tiene `allowInsecure: false`: un
pedido por HTTP se redirige a HTTPS. Los backends no tienen ingress externo. Además nginx envía
`Strict-Transport-Security: max-age=31536000` para que el navegador recuerde usar solo HTTPS (D-37).

## Casos de prueba manuales (para la matriz de CP)

| CP | Pasos | Resultado esperado |
|---|---|---|
| CP-B8-01 | Mirar la pestaña del navegador y el login | Ícono naranja con "A_" blanca (los enlaces llevan `?v=2` para que el navegador no use el ícono anterior guardado); el logo del login usa la misma letra |
| CP-B8-02 | Hallazgos con más de 25 resultados (el paginador aparece desde 11) | 25 filas, "1–25 de N", Siguiente/Anterior; las métricas cuentan todos |
| CP-B8-03 | Hallazgos → cambiar a 10 por página | 10 filas; sigue a la vista el mismo tramo |
| CP-B8-04 | Historial con más de 25 auditorías → Siguiente | Pide `pagina=2` al servidor (F12 → Red) y el contador muestra el total |
| CP-B8-05 | Bitácoras → Sistema con más de 25 registros (límite 100) → Siguiente | Paginador sobre los registros cargados; el detalle abre el registro correcto |
| CP-B8-06 | F12: `fetch` del ejemplo de arriba | 200 y `X-Total-Count` con el total |
| CP-B8-07 | F12: igual con `pagina=0` | 400 "El parámetro 'pagina' empieza en 1." |
| CP-B8-08 | Recorrer todas las pantallas | Sin rutas de API ni etiquetas en inglés a la vista |
| CP-B8-09 | Celular (o F12 → modo dispositivo 390 px) en todas las pantallas | Sin desplazamiento horizontal; paginador compacto |
| CP-B8-10 | Sitio en Azure → `http://` en lugar de `https://` | Redirige a HTTPS; candado válido (emitido por Microsoft) |
| CP-B8-11 | `curl -I -H "Accept-Encoding: gzip" https://<sitio>/login` (Cloud Shell) | `Strict-Transport-Security` y `Content-Encoding: gzip` |
| CP-B8-12 | Abrir `https://<sitio>/robots.txt` y `/sitemap.xml` | Solo `/login` permitido; el sitemap con el dominio real |
| CP-B8-13 | Chrome → ícono "Instalar" en la barra de direcciones | Se instala "AuditNet" y abre en ventana propia en `/dashboard` |
| CP-B8-14 | App instalada → F12 → Red → "Sin conexión" → recargar | Página "Sin conexión" con el logo; no se muestran datos guardados |
| CP-B8-15 | Push a `entrega-2` | El job "Desplegar en Azure" queda esperando aprobación; al aprobar, las 3 apps quedan con `sha-<commit>`, la revisión nueva aprovisionada y la prueba de humo pasa |
| CP-B8-16 | Panel ADM después del despliegue aprobado | Versión `sha-<commit>` del push en ambos servicios |
| CP-B8-17 | `bash 07-modo-demo.sh on`, luego `estado`, luego `off` | 1 réplica mínima en las 3 apps; vuelve a 0 |
| CP-B8-18 | Prueba de carga (`docs/entrega-2/CARGA.md`) | Todos los criterios en OK |
| CP-B8-19 | Evaluación SEO Optimer (`docs/entrega-2/SEO.md`) | Informe guardado con la nota y las observaciones |

## Pendiente de actualizar en la carpeta de tesis

- 10.5.9 Diagrama de despliegue y 10.4.5.2: HTTPS con certificado administrado por Azure, HSTS y GitHub Actions →
  aprobación → Azure (OIDC con identidad administrada).
- Matriz de Puntos Mandatorios: SSL (cumplido). Puntos extra: CI/CD, PWA, prueba de stress (k6) y SEO Optimer.
- RNF-005: resultados de la prueba de carga (tabla de `CARGA.md`). RNF-004: paginación y lenguaje de usuario.
- Interfaz (capturas): ícono nuevo, paginador, app instalada.
- Presupuesto (Excel): GitHub Actions gratuito en repositorio público; el modo demo consume la cuota gratuita de
  Container Apps mientras está encendido (impacto financiero a confirmar en Cost Management).
