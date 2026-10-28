## Tablero de control y estado del cliente

### Tablero de control

Es la primera pantalla al ingresar. Resume el estado de tu organización (el administrador del sistema elige la
organización arriba a la derecha o ve todas juntas).

- **Nivel general de riesgo:** alto, medio o bajo, con el motivo.
  - **Alto:** hay hallazgos críticos pendientes o alertas vigentes en equipos de criticidad alta o crítica.
  - **Medio:** hay hallazgos de severidad alta pendientes en equipos de criticidad media o baja.
  - **Bajo:** solo quedan hallazgos de severidad media o baja, o ninguno.
- **Indicadores:** dispositivos auditados, cumplimiento actual, hallazgos pendientes, críticos y altos, alertas
  vigentes, auditorías de los últimos 30 días, hallazgos vencidos y configuraciones desactualizadas. **Cada indicador
  abre el detalle** (por ejemplo, *hallazgos vencidos* abre Hallazgos filtrado por vencidos).
- **Hallazgos pendientes por severidad** y **Desvíos por semana** (detectados y cerrados en las últimas 8 semanas).
  Pasá el mouse sobre una barra para ver los números, o abrí *ver tabla*.
- **Alertas vigentes**, **Auditorías recientes** y **Dispositivos con mayor riesgo**, con enlaces a cada uno.

### Estado del cliente

Desde **Estado por sede →** o el menú `CLI`. Muestra el riesgo de cada sede, los hallazgos pendientes por severidad,
las auditorías recientes, los dispositivos ordenados por nivel de riesgo y los reportes disponibles de la organización.

![Estado del cliente: riesgo general e indicadores de la organización](estado-cliente.png)

> **Cumplimiento actual** es el porcentaje de reglas cumplidas en la última auditoría de cada equipo. Un **hallazgo
> vencido** es uno pendiente que superó el plazo de su severidad (por defecto: crítica 7 días, alta 15, media 30 y
> baja 60; cada organización puede ajustarlos).
