# Evaluación SEO Optimer — Bloque 8

Punto extra "Realizar evaluación SEO Optimer (https://www.seoptimer.com/)". Decisión D-41.

## Qué se preparó

AuditNet es una consola privada: lo único público es la pantalla de acceso, y es lo único que se ofrece a los
buscadores.

| Aspecto | Implementación |
|---|---|
| Título y descripción | `Acceso a la consola de auditoría de redes · AuditNet` (52 caracteres) + descripción de 130 |
| Idioma y adaptación móvil | `lang="es"`, `viewport` con `initial-scale=1` |
| Indexación | Login `index, follow`; consola `noindex, nofollow` |
| robots.txt / sitemap.xml | Generados por nginx con el dominio real; solo `/login` (y sus estilos e íconos) |
| Canonical y Open Graph | URL absoluta con el dominio real (nginx completa `__ORIGEN_PUBLICO__`) e imagen 512×512 |
| Datos estructurados | JSON-LD `Organization` + `WebApplication` |
| HTTPS, HSTS y compresión | Certificado de Azure, `Strict-Transport-Security`, gzip en HTML/CSS/JS/SVG |
| Favicon y app | SVG + PNG, manifiesto PWA |
| Raíz del sitio | `/` responde 302 a `/login` |

## Cómo ejecutarla

1. Abrir <https://www.seoptimer.com/> e ingresar `https://<sitio>/login` (con `/login`: desde la raíz el informe
   suma la redirección `/` → `/login`).
2. Guardar el informe en PDF ("Download PDF" puede pedir registrarse; si no, imprimir la página a PDF) en la carpeta de
   tesis, por ejemplo `Anexos/SEO-Optimer-AAAA-MM-DD.pdf`.
3. Completar la tabla.

Observaciones esperables que **no se corrigen a propósito** (justificarlas en la tesis): poco texto en la página
(es un login), sin enlaces a redes sociales, sin herramienta de analítica (no se rastrea a los usuarios de una
consola privada), sin registros SPF/DMARC (el dominio es de Azure, no propio) y que la mayoría del sitio esté
bloqueado en robots.txt (requiere sesión).

## Resultado

Informe del 22/10/2026 (plan gratuito), guardado en PDF en la carpeta de tesis.

| Sección | Nota |
|---|---|
| **General** | **B+** ("Your page is good"), 17 recomendaciones |
| On-Page SEO | A |
| Links | A+ |
| Usabilidad | A |
| Rendimiento | A- (PageSpeed 95 en celular y 95 en escritorio) |
| GEO (buscadores con IA) | B- |

Aprobado: título y descripción, `lang`, H1 y estructura de encabezados, textos alternativos, canonical, SSL y
redirección a HTTPS, robots.txt, sitemap.xml, datos estructurados, rastreadores de IA permitidos, viewport, favicon,
tamaño de página (0,04 MB), compresión, caché del navegador, HTTP/2, código minificado, sin errores de JavaScript,
Open Graph, tarjeta de X y HSTS.

### Observaciones y qué se hizo

| Observación | Prioridad | Decisión |
|---|---|---|
| Título de 27 caracteres (ideal 50–60) | Media | Corregido: 52 caracteres |
| Falta esquema de identidad (`Organization`) | Baja | Corregido: se agregó al JSON-LD |
| Una sola etiqueta H2 | Media | Se mantiene: el login tiene un único bloque de contenido |
| Velocidad del servidor (0,7 s) y "múltiples redirecciones" | Media | Arranque en frío de Container Apps (escala a cero) y medición desde la raíz `/`. Con modo demo y `/login` directo desaparecen. Mantener una réplica fija tiene impacto financiero |
| Recursos que bloquean el render | Baja | Son las hojas de estilo propias, ya minificadas y comprimidas; PageSpeed da 95 |
| Poco contenido (70 palabras) | Baja | Se mantiene: es una pantalla de acceso |
| Sin analítica ni píxel de Facebook | Baja | Se mantiene: no se rastrea a los usuarios de una consola privada |
| Sin `llms.txt` | Baja | Se mantiene: no es un estándar adoptado y el sitio no tiene contenido público para IA |
| Sin SPF/DMARC | Baja | El dominio `azurecontainerapps.io` es de Microsoft, no propio |
| Sin perfiles sociales ni esquema de negocio local | Baja | No aplica a un producto B2B sin presencia pública |
