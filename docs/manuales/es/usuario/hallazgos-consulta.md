## Hallazgos

Menú `LOG`. Un **hallazgo** es una regla que no se cumplió en una auditoría, con su evidencia. La lista está
**priorizada**: primero los pendientes, de mayor a menor severidad, considerando la criticidad del equipo.

![Hallazgos priorizados con el plazo de cada uno](hallazgos.png)

Usá los filtros por estado (incluido *vencidos*), severidad, dispositivo, baseline y fecha, o la búsqueda por regla,
dispositivo o evidencia. La columna de estado indica el plazo: *faltan N días*, *vence hoy* o *vencido hace N días*.

Al elegir un hallazgo ves la regla, el dispositivo, la auditoría que lo detectó, la evidencia, la recomendación y el
**seguimiento** (cada cambio de estado con usuario, fecha y comentario). Tu perfil lo consulta; el seguimiento lo
actualizan el analista de redes y el auditor técnico.

| Estado | Significa |
|---|---|
| abierto | detectado, nadie lo tomó todavía |
| en revisión | alguien está trabajando en la corrección |
| resuelto | corregido; normalmente lo confirma una auditoría nueva |
| riesgo aceptado | se decidió no corregir, con justificación |
