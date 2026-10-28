## Hallazgos

Menú `LOG`. Un **hallazgo** es una regla que no se cumplió en una auditoría, con su evidencia. La lista está
**priorizada**: primero los pendientes, de mayor a menor severidad, considerando la criticidad del equipo.

![Hallazgos priorizados con el plazo de cada uno](hallazgos.png)

### Buscar

Arriba hay filtros por estado (incluido *vencidos*), severidad, dispositivo, baseline y fecha de detección, y una
búsqueda por regla, dispositivo o evidencia. Los indicadores muestran pendientes, críticos y altos, en revisión y
cerrados. La columna de estado indica el **plazo**: *faltan N días*, *vence hoy* o *vencido hace N días*.

### Detalle y seguimiento

Al elegir un hallazgo ves la regla, la condición, el dispositivo, la baseline, la auditoría que lo detectó, el plazo,
el patrón buscado, la **evidencia** y el **seguimiento** (cada cambio de estado con usuario, fecha y comentario).

![Detalle de un hallazgo con el formulario de seguimiento](hallazgo-seguimiento.png)

**Cambiar el estado**

1. Elegí el **nuevo estado**.
2. Escribí un **comentario**: es obligatorio salvo para pasar a *en revisión*.
3. Tocá **Actualizar estado**.

| Estado | Cuándo usarlo | Puede pasar a |
|---|---|---|
| abierto | recién detectado o reabierto | en revisión, resuelto, riesgo aceptado |
| en revisión | alguien está trabajando en la corrección | abierto, resuelto, riesgo aceptado |
| resuelto | se corrigió en el equipo | abierto (reabrir) |
| riesgo aceptado | se decide no corregir, con justificación (excepción o falso positivo) | abierto (reabrir) |

> Lo habitual es corregir en el equipo, importar la configuración nueva y auditar: si la regla ahora se cumple, el
> hallazgo pasa solo a **resuelto** con el usuario SISTEMA y un comentario que indica la auditoría. Al cerrar un
> hallazgo (resuelto o riesgo aceptado) su alerta se cierra; al reabrirlo, vuelve a quedar vigente.
