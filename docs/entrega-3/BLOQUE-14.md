# Bloque 14 — Multi idioma (español, inglés, portugués)

Decisiones: D-57 a D-60 en `docs/entrega-2/REGISTRO-DECISIONES.md`. Migración nueva: management **V7**
(`usuarios.idioma`).

Se entrega en dos partes:

- **14a:** idioma guardado, selector, login y recuperación de contraseña, menú lateral y barra superior, Mi cuenta,
  correos y mensajes del servidor.
- **14b:** el resto de las pantallas, el reporte PDF y el CSV.

Hasta 14b, en inglés o portugués las demás pantallas muestran el menú, la barra y los errores del servidor traducidos,
pero su contenido sigue en español.

## Cómo funciona

| Parte | Dónde | Cómo |
|---|---|---|
| Idioma del usuario | management V7, `PUT /api/auth/me/language` `{idioma}` | `es`, `en` o `pt`; vuelve en la respuesta del login (`idioma`) |
| Elección antes de ingresar | Selector ES / EN / PT arriba a la derecha | Se recuerda en el navegador (`auditnet.idioma`) |
| Elección con sesión | Menú del usuario y Mi cuenta → *Idioma* | Se guarda en la cuenta y la página se recarga |
| Textos de las pantallas | `frontend/web-app/src/i18n/es.ts`, `en.ts`, `pt.ts` | Marcas `data-i18n` en el HTML y `t()` en los scripts |
| Mensajes del servidor | `src/main/resources/i18n/mensajes.txt` de cada servicio | Según `Accept-Language`, que la consola envía siempre |
| Correos | `src/main/resources/correo/textos_<idioma>.properties` | Idioma del destinatario al momento de enviar |

Para agregar un texto a una pantalla: sumar la clave en `es.ts` y su traducción en `en.ts` y `pt.ts` (si falta,
`npm run check` falla). Para un mensaje nuevo del servidor: agregar el bloque `es:` / `en:` / `pt:` en `mensajes.txt`
del servicio (si no está, se muestra en español).

## API nueva o modificada

| Servicio | Método y ruta | Quién |
|---|---|---|
| management | `POST /api/auth/login` y `POST /api/auth/password` informan `idioma` | Igual que antes |
| management | `PUT /api/auth/me/language` `{idioma}` → 200 `{idioma}`; otro valor → 400 | Usuario autenticado |
| ambos | Los mensajes de error salen en el idioma de `Accept-Language` (`es` si no viene) | Igual que antes |

## Pruebas automáticas

| Servicio / suite | Nuevas |
|---|---|
| management | `AuthServiceTest` (idioma guardado y devuelto en el login), `PlantillasCorreoTest` (inglés, portugués, meses, mismas claves), `CorreoServiceTest` (idioma del destinatario), `CatalogoMensajesTest` |
| audit-core | `CatalogoMensajesTest`; `RevisionPeriodicaServiceTest` con el período `2026-09` |
| Frontend | `npm run check`: los tres diccionarios con las mismas claves |
| Pantallas | `test_idiomas.py`: acceso en inglés y portugués, login con el idioma de la cuenta, cambio desde el menú, Mi cuenta, sin claves a la vista |

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

## Despliegue en Azure

Publicar las imágenes y desplegar como siempre: management aplica V7 al arrancar (los usuarios existentes quedan en
español). No hay recursos ni variables nuevas.

## Pendiente de actualizar en la carpeta de tesis (Bloque 16)

- CU nuevo: elegir idioma.
- DER: columna `idioma` en `usuarios`.
- Arquitectura: catálogo de mensajes y diccionarios del frontend.
