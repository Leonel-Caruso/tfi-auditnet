# Bloque 15 — Manuales

Decisiones: D-67 y D-68 en `docs/entrega-2/REGISTRO-DECISIONES.md`. Sin migraciones ni cambios en la API.

## Devolución de la Entrega 2

| Observación | Dónde quedó resuelta |
|---|---|
| CU | Sin cambios en este bloque (se completan en la carpeta, Bloque 16) |
| Multi idioma | Bloque 14; los manuales también están en español, inglés y portugués |
| Manuales de ayuda al usuario | Un manual por perfil, descargable desde la consola en el idioma de la cuenta |
| Manual del instalador (abordaje de un desarrollador) | Manual de instalación y desarrollo en los tres idiomas |
| Notificaciones | Bloques 12 y 13; explicadas en el capítulo *Avisos por correo* de cada manual de usuario |

## Manuales

| Manual | Contenido |
|---|---|
| Usuario (5, uno por perfil) | Qué puede hacer el perfil; acceso y recuperación de contraseña; la consola, el menú del usuario y Mi cuenta; tablero y estado del cliente; un capítulo por cada pantalla del perfil con los pasos, las capturas y las reglas que aplica la plataforma; avisos por correo; problemas frecuentes |
| Instalación y desarrollo | Arquitectura y decisiones clave; repositorio, capas y convenciones; instalación local y datos de demostración; pruebas; cómo agregar un endpoint, una migración, un tipo de regla, una pantalla, una traducción, un correo o regenerar los manuales; CI/CD y recursos de Azure; operación; problemas reales y su solución; permisos por perfil y dónde se controlan |

Cada manual de usuario incluye solo los capítulos de su perfil: el del analista no explica cómo crear baselines y el
del responsable de gestión IT explica cómo derivar alertas pero no cómo cambiar el estado de un hallazgo. Las
pantallas que un perfil solo consulta tienen un capítulo propio, más corto, que dice quién hace los cambios.

Generación y estructura de las fuentes: `docs/manuales/README.md`.

## En la consola

| Dónde | Qué |
|---|---|
| Tablero, arriba | Tarjeta *Manual de uso* con un botón por perfil del usuario; *Ocultar* la saca en ese navegador |
| Menú del usuario | *Manual · <perfil>* por cada perfil, siempre visible |
| Archivo | `/manuales/manual-<perfil>-<idioma>.pdf`, con el idioma de la cuenta |

Archivos: `src/lib/manual.ts`, `pages/dashboard.astro`, `scripts/dashboard.ts`, `components/Topbar.astro`,
`layouts/MasterLayout.astro`, claves `manual.*` en `i18n/es.ts`, `en.ts` y `pt.ts`.

## Otros cambios

- Reglas: las condiciones del formulario, la ayuda y el detalle se muestran traducidas ("debe contener", "must
  contain", "deve conter") en lugar del código interno.
- Datos de demostración: los períodos de los reportes se arman por días completos en hora de Argentina (antes en UTC,
  y el reporte del mes anterior empezaba y terminaba tres horas antes).

## Pruebas automáticas

| Suite | Nuevas |
|---|---|
| Pantallas | `test_manual.py`: un botón por perfil con su PDF, el PDF del idioma de la cuenta, los 15 PDF publicados y servidos, ocultar la tarjeta y seguir teniendo el manual en el menú, cambio de idioma, usuario con dos perfiles |
| Datos de demostración | `test_periodos.py`: mes anterior y últimas 8 semanas en hora de Argentina |

## Casos de prueba manuales

| CP | Pasos | Resultado esperado |
|---|---|---|
| CP-B15-01 | Ingresar como analista de redes | Arriba del tablero, *Manual de uso* con "Descargar manual · Analista de redes (PDF)" |
| CP-B15-02 | Descargarlo y abrirlo | PDF en español con portada del perfil, índice, capítulos y capturas numeradas |
| CP-B15-03 | Menú del usuario → EN y descargar de nuevo | "Download guide · Network analyst (PDF)"; el PDF está en inglés |
| CP-B15-04 | Mi cuenta → Português y descargar desde el menú del usuario | Manual en portugués |
| CP-B15-05 | *Ocultar* y recargar | La tarjeta no vuelve; el manual sigue en el menú del usuario |
| CP-B15-06 | Ingresar con un usuario con dos perfiles (administrador de organización y analista) | Dos botones, uno por perfil |
| CP-B15-07 | Ingresar con cada perfil | Cada manual describe solo lo que ese perfil ve en el menú |
| CP-B15-08 | Abrir `docs/manuales/pdf/manual-desarrollo-es.pdf` y seguir *Instalación local* en una PC sin el proyecto | La consola queda funcionando en `localhost:4321` |
| CP-B15-09 | Con el sitio de pruebas compilado, `python docs\manuales\generar.py` | Se regeneran las capturas y los 18 PDF sin errores |

## Despliegue en Azure

Publicar y desplegar como siempre: los PDF de usuario viajan dentro de la imagen web. No hay recursos ni variables
nuevas.
