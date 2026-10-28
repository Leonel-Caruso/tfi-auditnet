## Historial de auditorías

Menú `HIS`. Todas las auditorías, con la configuración evaluada, la baseline y las reglas usadas. Es de solo lectura.

![Historial con el detalle y la evolución de una auditoría](historial.png)

### Buscar

Filtrá por dispositivo, baseline, severidad máxima, resultado (cumple o con hallazgos), quién la ejecutó y rango de
fechas, y tocá **Consultar**. Las auditorías hechas por la revisión periódica aparecen como **automática**.

### Detalle y evolución

Al elegir una auditoría ves la configuración (versión y quién la importó), la baseline con su versión, las reglas
aplicadas y los hallazgos generados con su estado actual. La **evolución** la compara con la auditoría anterior del
mismo equipo:

| Categoría | Significa |
|---|---|
| nuevos desvíos | reglas que antes cumplían y ahora no |
| corregidos | reglas que fallaban y ahora cumplen |
| persisten | reglas que siguen fallando |
| reglas nuevas / retiradas | cambios en la baseline entre una auditoría y otra |

**Ver solo el historial de este dispositivo** filtra la lista por ese equipo y **Ver hallazgos de esta auditoría**
abre sus hallazgos.
