# Bloque 14 — Multi idioma (español, inglés, portugués)

Decisiones: D-57 a D-66 en `docs/entrega-2/REGISTRO-DECISIONES.md`. Migraciones nuevas: management **V7**
(`usuarios.idioma`) y **V8** (`usuarios.tema`, 14c).

Se entrega en tres partes:

- **14a:** idioma guardado, selector, login y recuperación de contraseña, menú lateral y barra superior, Mi cuenta,
  correos y mensajes del servidor.
- **14b:** el resto de las pantallas, el reporte PDF, el CSV y la recuperación de contraseña en el idioma de la
  pantalla desde donde se pide.
- **14c:** mejoras de la consola (diálogo de confirmación propio, fechas y evidencia en el idioma, título del
  reporte, tema oscuro, menos movimiento, política de seguridad de contenido, scripts en archivos aparte) y datos de
  demostración.

## Cómo funciona

| Parte | Dónde | Cómo |
|---|---|---|
| Idioma del usuario | management V7, `PUT /api/auth/me/language` `{idioma}` | `es`, `en` o `pt`; vuelve en la respuesta del login (`idioma`) |
| Tema del usuario (14c) | management V8, `PUT /api/auth/me/theme` `{tema}` | `claro` u `oscuro`; vuelve en el login (`tema`, vacío si no eligió) |
| Elección antes de ingresar | Selector ES / EN / PT arriba a la derecha | Se recuerda en el navegador (`auditnet.idioma`) |
| Elección con sesión | Menú del usuario y Mi cuenta → *Idioma* | Se guarda en la cuenta y la página se recarga |
| Textos comunes (acceso, menú, barra, Mi cuenta) | `frontend/web-app/src/i18n/es.ts`, `en.ts`, `pt.ts` | Marcas `data-i18n` en el HTML y `t()` en los scripts |
| Textos de cada pantalla | `frontend/web-app/src/i18n/pantallas/<pantalla>.ts` (los tres idiomas juntos) | Igual; `general.ts` tiene lo compartido |
| Reporte PDF | `frontend/web-app/src/lib/report-document.ts` | Se imprime desde la consola, en su idioma |
| Reporte CSV | `ExportadorCsv` (audit-core) | Idioma de `Accept-Language`; el guardado en el repositorio es el de español |
| Mensajes del servidor | `src/main/resources/i18n/mensajes.txt` de cada servicio | Según `Accept-Language`, que la consola envía siempre |
| Correos | `src/main/resources/correo/textos_<idioma>.properties` | Idioma del destinatario al momento de enviar; la recuperación, el de la pantalla desde donde se pidió |

Para agregar un texto a una pantalla: sumar la clave en el `es` de su diccionario y la traducción en `en` y `pt` (si
falta, `npm run check` falla). Para un mensaje nuevo del servidor: agregar el bloque `es:` / `en:` / `pt:` en `mensajes.txt`
del servicio (si no está, se muestra en español).

## API nueva o modificada

| Servicio | Método y ruta | Quién |
|---|---|---|
| management | `POST /api/auth/login` y `POST /api/auth/password` informan `idioma` | Igual que antes |
| management | `PUT /api/auth/me/language` `{idioma}` → 200 `{idioma}`; otro valor → 400 | Usuario autenticado |
| management | `PUT /api/auth/me/theme` `{tema}` → 200 `{tema}`; otro valor → 400 (14c) | Usuario autenticado |
| management | `POST /api/auth/password-recovery` usa `Accept-Language` para el idioma del correo | Público |
| audit-core | `GET /api/reports/{id}/download` arma el CSV en el idioma de `Accept-Language` | Igual que antes |
| ambos | Los mensajes de error salen en el idioma de `Accept-Language` (`es` si no viene) | Igual que antes |

## Pruebas automáticas

| Servicio / suite | Nuevas |
|---|---|
| management | `AuthServiceTest` (idioma guardado y devuelto en el login), `PlantillasCorreoTest` (inglés, portugués, meses, mismas claves), `CorreoServiceTest` (idioma del destinatario y del pedido), `RecuperacionContrasenaServiceTest` (idioma de la pantalla), `CatalogoMensajesTest` |
| audit-core | `CatalogoMensajesTest`; `RevisionPeriodicaServiceTest` con el período `2026-09`; `ReporteServiceTest` (CSV en inglés y portugués) |
| Frontend | `npm run check`: los tres idiomas con las mismas claves en cada diccionario |
| Pantallas | `test_idiomas.py`: acceso, login con el idioma de la cuenta, cambio desde el menú, Mi cuenta; todas las pantallas con cada perfil en inglés y portugués (sin claves ni restos en español, sin desbordes en celular); textos de los scripts, PDF, calendario |
| audit-core (14c) | `ReporteServiceTest`: título en inglés y portugués |
| Pantallas (14c) | `test_tema.py`: tema elegido y recordado, traducido, reporte claro y todas las pantallas en oscuro sin fondos claros; `test_idiomas.py`: evidencia traducida; `test_acceso.py`: la política de seguridad bloquea un script agregado. Todas las pruebas corren con las mismas políticas que nginx |
| Datos de demostración | `tests/datos_demo`: cada configuración cumple exactamente los controles de su versión, límites de planes y códigos únicos |

## Casos de prueba manuales

| CP | Pasos | Resultado esperado |
|---|---|---|
| CP-B14-01 | Abrir `/login` en una ventana privada | Español; selector con ES marcado |
| CP-B14-02 | Tocar EN y después PT | La pantalla cambia sin mostrar el español en el medio; al recargar queda en portugués |
| CP-B14-03 | Ingresar con un usuario que nunca eligió idioma (desde el login en portugués) | La consola queda en español: manda el idioma de la cuenta |
| CP-B14-04 | Menú del usuario → EN | Menú lateral, barra superior y Mi cuenta en inglés; al cerrar sesión e ingresar desde otro navegador sigue en inglés |
| CP-B14-05 | Login en portugués con una contraseña equivocada | "Usuário ou senha incorretos." |
| CP-B14-06 | Con el idioma en inglés: *¿Olvidaste tu contraseña?* para ese usuario | Correo "Reset your AuditNet password", todo en inglés |
| CP-B14-07 | Mi cuenta → Español | Todo vuelve a español |
| CP-B14-08 | Con un usuario en español: cerrar sesión, poner el login en PT y pedir *Esqueceu sua senha?* | El correo llega en portugués; al ingresar, la consola sigue en español |
| CP-B14-09 | En inglés, recorrer Dashboard, Devices, Rules, Findings, Alerts y Reports | Títulos, tablas, filtros, botones y mensajes en inglés; los nombres cargados (reglas, sedes) quedan como se registraron |
| CP-B14-10 | En portugués: Achados → elegir uno → cambiar el estado sin comentario | El detalle y el aviso "é preciso informar um comentário" en portugués |
| CP-B14-11 | En portugués, abrir la consola en el celular | Nada se sale de la pantalla |
| CP-B14-12 | En inglés: Reports → abrir uno → *Download PDF* | El PDF en inglés ("Executive summary", "Page 1 of 2") |
| CP-B14-13 | En inglés: *Download CSV* y abrirlo con Excel | Columnas en inglés separadas por coma; en español sigue con punto y coma |

## Despliegue en Azure

Publicar las imágenes y desplegar como siempre: management aplica V7 y V8 al arrancar (los usuarios existentes quedan
en español y sin tema elegido). No hay recursos ni variables nuevas.

## 14c — Mejoras de la consola

| Mejora | Dónde |
|---|---|
| Confirmar dar de baja, desactivar o reactivar con el diálogo de la consola (en lugar del del navegador) | `components/ConfirmDialog.astro`, `lib/ui.ts` |
| Fechas con el formato del idioma (día/mes en español y portugués, mes/día en inglés) en tablas, calendario y PDF | `lib/format.ts`, `lib/datepicker.ts`, `lib/report-document.ts` |
| Evidencia del motor traducida al mostrarla; se guarda en español | `lib/evidencia.ts` |
| Título del reporte en el idioma de quien lo genera | `IdiomaReporte`, `ReporteService` (audit-core) |
| Párrafos del reporte (resumen ejecutivo y alcance) a todo el ancho del documento, en pantalla y en el PDF | `global.css` |
| Tema Claro / Oscuro en el acceso, el menú del usuario y Mi cuenta, guardado en la cuenta (sin elegir, el del sistema) | management V8, `lib/tema.ts`, `public/inicio.js`, variables de `global.css` |
| Sin animaciones con "reducir movimiento" activado en el sistema | `global.css` |
| `Content-Security-Policy` y `Permissions-Policy` | `nginx/default.conf.template` |
| Script de cada página en su archivo y funciones repetidas en un solo lugar | `src/scripts/`, `lib/ui.ts` |

### Casos de prueba manuales (14c)

| CP | Pasos | Resultado esperado |
|---|---|---|
| CP-B14-14 | Dispositivos → elegir uno → *Dar de baja* | Diálogo de la consola con Cancelar y Dar de baja; Cancelar no cambia nada |
| CP-B14-15 | En inglés, Hallazgos → elegir uno | Evidencia en inglés ("Required pattern not found: …") y fechas mes/día |
| CP-B14-16 | En portugués, Reportes → generar uno por fechas | Título "Relatório de auditorias de … a …" |
| CP-B14-17 | Menú del usuario → Oscuro; recargar | La consola queda oscura; el reporte se sigue viendo como hoja blanca |
| CP-B14-18 | En una ventana privada abrir `/login`, tocar Oscuro e ingresar con un usuario que nunca eligió tema | La consola queda oscura; al ingresar con ese usuario desde otro navegador también |
| CP-B14-19 | Con un usuario que tiene Claro guardado: en el acceso tocar Oscuro e ingresar | Queda claro: manda el de la cuenta |
| CP-B14-20 | Herramientas del navegador → Red → `/login` | Encabezados `Content-Security-Policy` y `Permissions-Policy`; la consola sin errores en la pestaña Consola |

## 14c — Datos de demostración

`scripts/datos-demo/`: `cargar.py` (Python 3.9 o superior, sin dependencias), `catalogo.py` (los datos) e
`historia.sql`.

| Qué carga | Cantidad |
|---|---|
| Organizaciones ficticias (planes Piloto, Inicial y Profesional) | 6 |
| Sedes | 16 |
| Usuarios con distintos perfiles (algunos en inglés y portugués) | 20 |
| Tipos de dispositivo Cisco (ISR 4331, ISR 1100, Catalyst 9200, Catalyst 2960-X) | 4 |
| Dispositivos | 41 |
| Baselines (una de routers y una de switches por organización) con 12 reglas cada una | 12 |
| Configuraciones (1 a 4 versiones por equipo) y sus auditorías | unas 100 |

1. `cargar.py` usa la API como el administrador del sistema. Pide su contraseña y la que van a tener los usuarios
   nuevos (no se guardan). Los correos de los usuarios son alias de la casilla indicada con `--correo`
   (`casilla+usuario@dominio`), así llegan a un solo lugar; antes de auditar apaga los avisos de cada uno. Al crear
   cada usuario se envía el correo de bienvenida. Si se vuelve a ejecutar, reutiliza lo que ya existe.
2. `historia.sql` reparte las auditorías recién cargadas en las últimas 8 semanas, con sus hallazgos, cambios de estado
   y alertas. Solo toca esas organizaciones y las auditorías de las últimas 24 horas. Las bitácoras y el catálogo de
   archivos conservan la fecha real de la carga.
3. `cargar.py --reportes` genera, con el auditor de cada organización, el reporte de las últimas 8 semanas y el del
   mes anterior.

## Pendiente de actualizar en la carpeta de tesis (Bloque 16)

- CU nuevo: elegir idioma.
- DER: columna `idioma` en `usuarios`.
- Arquitectura: catálogo de mensajes y diccionarios del frontend.
- Capturas de pantallas en los tres idiomas para el manual de usuario (Bloque 15).
- Manual de usuario: tema oscuro y datos de demostración para las capturas.
