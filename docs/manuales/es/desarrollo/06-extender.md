## Cómo extender la plataforma

### Un endpoint nuevo

1. **Dominio:** si hace falta, un record en `domain/model` y los métodos en la interfaz de `domain/repository`.
2. **Aplicación:** el caso de uso en un servicio de `application/<módulo>`. Valida, aplica el alcance de la
   organización y registra la operación en la bitácora de transacciones (`TrazabilidadPort`, con el valor anterior y
   el nuevo).
3. **Infraestructura:** la implementación del repositorio con Panache en `infrastructure/persistence`.
4. **API:** el recurso en `api/rest` con `@Path`, los DTO como records en `api/rest/dto` y **`@RolesAllowed`** con
   los perfiles que pueden usarlo.
5. **Permisos:** si es una acción nueva, sumarla a `MatrizPermisos` (management) y a `RolServiceTest`; la matriz se
   muestra en la pantalla de Usuarios.
6. **Gateway:** si el prefijo de la ruta es nuevo y corresponde a management-service, agregarlo a la expresión de
   `location ~ ^/api/(auth|users|...)` en `frontend/web-app/nginx/default.conf.template`. Lo demás va a audit-core.
7. **Mensajes:** cada mensaje de error nuevo que vea el usuario va en `src/main/resources/i18n/mensajes.txt` del
   servicio, con sus líneas `es:`, `en:` y `pt:` (si falta, se muestra en español).
8. **Pruebas:** del servicio con los repositorios en memoria y, si la usa una pantalla, la respuesta simulada en
   `tests/pantallas/servidor.py`.

### Una migración

1. Crear `V<n>__<descripcion>.sql` en `db/migration` del servicio dueño de la tabla, con el número siguiente al último
   de **ese** servicio.
2. Ajustar la entidad Panache y el repositorio.
3. Agregar la fila en `docs/database/MIGRACIONES.md`.
4. Al arrancar (`mvn quarkus:dev`, o el despliegue en Azure) Flyway la aplica sola. Nunca editar una ya aplicada.

### Un tipo de regla de auditoría

Los tipos son `DEBE_CONTENER`, `NO_DEBE_CONTENER` y `VALOR_ESPERADO`. Para sumar uno:

1. El valor en `TipoReglaConfiguracion` y su evaluación en `MotorAuditoriaService` (devuelve si cumple y la evidencia).
2. Una migración que amplíe el `CHECK` de `reglas_baseline.tipo`.
3. En la consola, la opción en el formulario de Reglas, su nombre en el diccionario (`condicion.<TIPO>`) y, si la
   evidencia tiene una frase nueva, su traducción en `lib/evidencia.ts`.
4. Pruebas del motor con configuraciones que cumplen y que no.

### Una pantalla

1. `src/pages/<ruta>.astro` con `MasterLayout` (el atributo `i18n` indica el prefijo de las claves del encabezado) y
   su script en `src/scripts/<ruta>.ts`, cargado con `<script src="../scripts/<ruta>.ts">`. No se escriben scripts
   dentro del HTML: la política de seguridad no los permite.
2. El ítem del menú en `components/Sidebar.astro` (código corto y perfiles) y la ruta en la lista de
   `components/Topbar.astro`; opcionalmente palabras para *Ir a módulo* en `layouts/MasterLayout.astro`.
3. Las claves `menu.<ruta>` y `ruta.<ruta>` en `i18n/es.ts`, `en.ts` y `pt.ts`, y un diccionario
   `i18n/pantallas/<pantalla>.ts` con `es`, `en` y `pt`, sumado en `i18n/index.ts`.
4. La ruta en `tests/pantallas/test_pantallas.py` (`PANTALLAS` o `PANTALLAS_POR_PERFIL`).

### Una traducción

- En el HTML: `data-i18n="clave"` para el texto y `data-i18n-placeholder`, `data-i18n-title`,
  `data-i18n-aria-label` o `data-i18n-label` para atributos.
- En los scripts: `t('clave', { dato })`, `plural(n, 'clave')` (claves `clave.uno` y `clave.otros`) y
  `etiqueta('grupo', codigo)` para códigos como severidades o estados.
- El tipo `Textos<typeof es>` obliga a que inglés y portugués tengan las mismas claves: `npm run check` falla si falta
  una. Los datos cargados por los usuarios (nombres de reglas, sedes) no se traducen.

### Un correo

Los textos están en `management-service/src/main/resources/correo/textos_<idioma>.properties` y el armado en
`PlantillasCorreo`. Un tipo nuevo necesita su valor en `TipoCorreo`, sus textos en los tres idiomas y una prueba en
`PlantillasCorreoTest`. Se encola con `CorreoService.encolar(...)` (o, desde audit-core, en la bandeja de salida
común) y sale con los reintentos de siempre.

### Los manuales

Las fuentes están en `docs/manuales/<idioma>/usuario` y `docs/manuales/<idioma>/desarrollo`, en Markdown. Las
capturas se sacan solas del sitio de pruebas con datos ficticios (`capturas.py`, `datos_manual.py`):

```powershell
cd .\frontend\web-app; npm run build:pruebas; cd ..\..
python -m pip install -r docs\manuales\requirements.txt
python docs\manuales\generar.py
```

Los manuales de usuario quedan en `frontend/web-app/public/manuales` (la consola los ofrece para descargar) y este en
`docs/manuales/pdf`.
