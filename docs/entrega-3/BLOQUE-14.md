# Bloque 14 — Multi idioma (español, inglés, portugués)

Decisiones: D-57 a D-62 en `docs/entrega-2/REGISTRO-DECISIONES.md`. Migración nueva: management **V7**
(`usuarios.idioma`).

Se entrega en dos partes:

- **14a:** idioma guardado, selector, login y recuperación de contraseña, menú lateral y barra superior, Mi cuenta,
  correos y mensajes del servidor.
- **14b:** el resto de las pantallas, el reporte PDF, el CSV y la recuperación de contraseña en el idioma de la
  pantalla desde donde se pide.

## Cómo funciona

| Parte | Dónde | Cómo |
|---|---|---|
| Idioma del usuario | management V7, `PUT /api/auth/me/language` `{idioma}` | `es`, `en` o `pt`; vuelve en la respuesta del login (`idioma`) |
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

Publicar las imágenes y desplegar como siempre: management aplica V7 al arrancar (los usuarios existentes quedan en
español). No hay recursos ni variables nuevas.

## Pendiente de actualizar en la carpeta de tesis (Bloque 16)

- CU nuevo: elegir idioma.
- DER: columna `idioma` en `usuarios`.
- Arquitectura: catálogo de mensajes y diccionarios del frontend.
- Capturas de pantallas en los tres idiomas para el manual de usuario (Bloque 15).
