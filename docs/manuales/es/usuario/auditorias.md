## Auditorías

Menú `RUN`. Arriba están las **configuraciones** importadas y debajo las **auditorías ejecutadas**. A la derecha, el
contexto de lo que elijas.

### Importar una configuración

1. Tocá **+ cargar configuración**.
2. Elegí el **dispositivo**.
3. Pegá el **running-config** o subí un archivo `.txt` o `.cfg` (el contenido aparece en el cuadro).
4. Tocá **Importar configuración**.

![Carga de una configuración](configuracion-carga.png)

Cada importación es una **versión nueva** del equipo (v1, v2, …). Se conserva el original tal como llegó y se genera
una copia normalizada para comparar. El archivo original queda en el repositorio de archivos con su hash SHA-256:
desde el contexto de la configuración se puede descargar y AuditNet verifica que no haya cambiado.

### Ejecutar una auditoría

1. Elegí la configuración en la lista. El contexto muestra la **baseline aplicable** y cuántas reglas activas tiene.
2. Tocá **ejecutar auditoría**.

Si no aparece una baseline aplicable, falta una baseline activa para ese tipo de dispositivo en la organización.

### Leer el resultado

![Resultado de una auditoría con la evaluación de cada regla](auditoria-resultado.png)

- **reglas evaluadas / cumplidas**, **hallazgos** y **severidad máxima**.
- **Evaluación de reglas:** cada regla con CUMPLE o HALLAZGO y su evidencia (la línea encontrada o lo que falta).
- **Ver hallazgos →** abre los hallazgos de esta auditoría; **Ver en el historial →** la compara con la anterior.

Qué pasa además al auditar:

- Cada regla que no se cumple genera un **hallazgo**.
- Los hallazgos de severidad **alta o crítica** generan una **alerta crítica** (una sola por equipo y regla) y un
  correo a quienes tienen ese aviso activado.
- Los hallazgos pendientes de auditorías anteriores del mismo equipo cuya regla ahora se cumple pasan solos a
  **resuelto** y su alerta se cierra.
