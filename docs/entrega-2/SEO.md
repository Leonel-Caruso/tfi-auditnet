# Evaluación SEO Optimer — Bloque 8

Punto extra "Realizar evaluación SEO Optimer (https://www.seoptimer.com/)". Decisión D-41.

## Qué se preparó

AuditNet es una consola privada: lo único público es la pantalla de acceso, y es lo único que se ofrece a los
buscadores.

| Aspecto | Implementación |
|---|---|
| Título y descripción | `Acceso a consola · AuditNet` + descripción de 130 caracteres |
| Idioma y adaptación móvil | `lang="es"`, `viewport` con `initial-scale=1` |
| Indexación | Login `index, follow`; consola `noindex, nofollow` |
| robots.txt / sitemap.xml | Generados por nginx con el dominio real; solo `/login` (y sus estilos e íconos) |
| Canonical y Open Graph | URL absoluta con el dominio real (nginx completa `__ORIGEN_PUBLICO__`) e imagen 512×512 |
| Datos estructurados | JSON-LD `WebApplication` |
| HTTPS, HSTS y compresión | Certificado de Azure, `Strict-Transport-Security`, gzip en HTML/CSS/JS/SVG |
| Favicon y app | SVG + PNG, manifiesto PWA |
| Raíz del sitio | `/` responde 302 a `/login` |

## Cómo ejecutarla

1. Abrir <https://www.seoptimer.com/> e ingresar `https://<sitio>/login`.
2. Guardar el informe en PDF ("Download PDF" puede pedir registrarse; si no, imprimir la página a PDF) en la carpeta de
   tesis, por ejemplo `Anexos/SEO-Optimer-AAAA-MM-DD.pdf`.
3. Completar la tabla.

Observaciones esperables que **no se corrigen a propósito** (justificarlas en la tesis): poco texto en la página
(es un login), sin enlaces a redes sociales, sin herramienta de analítica (no se rastrea a los usuarios de una
consola privada), sin registros SPF/DMARC (el dominio es de Azure, no propio) y que la mayoría del sitio esté
bloqueado en robots.txt (requiere sesión).

## Resultado (completar)

| Fecha | URL | Nota general | On-page SEO | Usabilidad | Rendimiento | Seguridad | Observaciones principales |
|---|---|---|---|---|---|---|---|
| | `https://<sitio>/login` | | | | | | |
