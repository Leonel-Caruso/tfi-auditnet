## Reportes de auditoría

Menú `REP`. Un reporte reúne auditorías con su resumen ejecutivo, hallazgos, severidad, impacto y evidencia. Queda
guardado como **foto fija**: refleja el estado de los hallazgos al momento de emitirlo y no cambia aunque después
cambien los datos.

![Criterios de un reporte nuevo y reportes generados](reportes.png)

### Generar un reporte

1. En **Criterios** indicá el rango de fechas (**desde** y **hasta**) o los números de **auditorías específicas**
   separados por coma (por ejemplo `12, 15`; en ese caso las fechas son opcionales).
2. Si querés, acotá por **dispositivo**, **severidad** y **estado** de los hallazgos. Sin marcar = todos.
3. Tocá **Generar reporte**.

El administrador del sistema además elige la organización (o todas). Un reporte incluye hasta 500 auditorías: si los
criterios abarcan más, acotá las fechas. El título se arma en el idioma de quien lo genera.

### Ver y descargar

![Vista del documento del reporte](reporte-documento.png)

Al elegir un reporte se muestra el documento: datos generales, resumen ejecutivo con el cumplimiento, alcance y
criterios, distribución de hallazgos, auditorías incluidas, detalle de cada hallazgo y observaciones.

- **Descargar PDF:** imprime el documento en A4 con número de página. Sale en el idioma de la consola.
- **Descargar CSV:** para Excel, con una fila por hallazgo. En español y portugués usa `;` como separador y en
  inglés `,`.
