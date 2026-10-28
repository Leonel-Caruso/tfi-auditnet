## Extending the platform

### A new endpoint

1. **Domain:** if needed, a record in `domain/model` and the methods in the `domain/repository` interface.
2. **Application:** the use case in a service of `application/<module>`. It validates, applies the organization scope
   and records the operation in the transaction log (`TrazabilidadPort`, with the previous and new values).
3. **Infrastructure:** the repository implementation with Panache in `infrastructure/persistence`.
4. **API:** the resource in `api/rest` with `@Path`, the DTOs as records in `api/rest/dto` and **`@RolesAllowed`**
   with the profiles that may use it.
5. **Permissions:** if it is a new action, add it to `MatrizPermisos` (management) and to `RolServiceTest`; the
   matrix is shown on the Users screen.
6. **Gateway:** if the route prefix is new and belongs to management-service, add it to the expression of
   `location ~ ^/api/(auth|users|...)` in `frontend/web-app/nginx/default.conf.template`. Everything else goes to
   audit-core.
7. **Messages:** every new error message the user sees goes in the service's `src/main/resources/i18n/mensajes.txt`,
   with its `es:`, `en:` and `pt:` lines (if one is missing, it is shown in Spanish).
8. **Tests:** service tests with the in-memory repositories and, if a screen uses it, the mock response in
   `tests/pantallas/servidor.py`.

### A migration

1. Create `V<n>__<description>.sql` in `db/migration` of the service that owns the table, with the number following
   the last one of **that** service.
2. Adjust the Panache entity and the repository.
3. Add the row in `docs/database/MIGRACIONES.md`.
4. On startup (`mvn quarkus:dev`, or the Azure deployment) Flyway applies it on its own. Never edit one that has
   already been applied.

### An audit rule type

The types are `DEBE_CONTENER`, `NO_DEBE_CONTENER` and `VALOR_ESPERADO`. To add one:

1. The value in `TipoReglaConfiguracion` and its evaluation in `MotorAuditoriaService` (it returns whether it passes
   and the evidence).
2. A migration that widens the `CHECK` of `reglas_baseline.tipo`.
3. In the console, the option in the Rules form, its name in the dictionary (`condicion.<TYPE>`) and, if the evidence
   has a new sentence, its translation in `lib/evidencia.ts`.
4. Engine tests with configurations that pass and that do not.

### A screen

1. `src/pages/<route>.astro` with `MasterLayout` (the `i18n` attribute sets the key prefix of the header) and its
   script in `src/scripts/<route>.ts`, loaded with `<script src="../scripts/<route>.ts">`. No scripts are written
   inside the HTML: the security policy does not allow them.
2. The menu item in `components/Sidebar.astro` (short code and profiles) and the route in the list of
   `components/Topbar.astro`; optionally words for *Go to module* in `layouts/MasterLayout.astro`.
3. The `menu.<route>` and `ruta.<route>` keys in `i18n/es.ts`, `en.ts` and `pt.ts`, and a dictionary
   `i18n/pantallas/<screen>.ts` with `es`, `en` and `pt`, added in `i18n/index.ts`.
4. The route in `tests/pantallas/test_pantallas.py` (`PANTALLAS` or `PANTALLAS_POR_PERFIL`).

### A translation

- In the HTML: `data-i18n="key"` for the text and `data-i18n-placeholder`, `data-i18n-title`, `data-i18n-aria-label`
  or `data-i18n-label` for attributes.
- In the scripts: `t('key', { value })`, `plural(n, 'key')` (keys `key.uno` and `key.otros`) and
  `etiqueta('group', code)` for codes such as severities or statuses.
- The `Textos<typeof es>` type forces English and Portuguese to have the same keys: `npm run check` fails if one is
  missing. Data loaded by users (rule names, sites) is not translated.

### An email

The texts are in `management-service/src/main/resources/correo/textos_<language>.properties` and they are assembled
in `PlantillasCorreo`. A new type needs its value in `TipoCorreo`, its texts in the three languages and a test in
`PlantillasCorreoTest`. It is queued with `CorreoService.encolar(...)` (or, from audit-core, in the shared outbox) and
goes out with the usual retries.

### The guides

The sources are in `docs/manuales/<language>/usuario` and `docs/manuales/<language>/desarrollo`, in Markdown. The
screenshots are taken automatically from the test site with fictional data (`capturas.py`, `datos_manual.py`):

```powershell
cd .\frontend\web-app; npm run build:pruebas; cd ..\..
python -m pip install -r docs\manuales\requirements.txt
python docs\manuales\generar.py
```

The user guides end up in `frontend/web-app/public/manuales` (the console offers them for download) and this one in
`docs/manuales/pdf`.
