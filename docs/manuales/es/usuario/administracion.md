## Panel de administración

Menú `ADM`. Estado de la plataforma, correo, revisión periódica, archivos, respaldos y seguridad de las cuentas.

![Panel de administración](administracion.png)

| Panel | Qué muestra o permite |
|---|---|
| Estado de la plataforma | si los servicios responden, su versión, memoria y sus componentes (PostgreSQL, MongoDB, almacenamiento). **Actualizar** vuelve a verificar. |
| Planes | cuántas solicitudes de cambio de plan hay por resolver, con enlace a *Planes y solicitudes*. |
| Correo | la bandeja de salida: pendientes, enviados y fallidos. Los avisos salen solos y los fallidos se reintentan; **Enviar pendientes** fuerza el envío. |
| Revisión periódica | la última ejecución (re-auditorías, hallazgos vencidos, configuraciones desactualizadas, correos). Corre sola todos los días a las 6:00; **Ejecutar revisión ahora** la lanza en el momento. |
| Repositorio de archivos | configuraciones importadas y reportes con su hash SHA-256. **Subir pendientes** sube los que todavía no tienen su archivo. |
| Respaldos | **Generar respaldo** crea un ZIP con los datos de todas las tablas y el guion de restauración. Incluye datos personales: descargalo solo a un lugar seguro. |
| Seguridad | usuarios activos, cuentas bloqueadas, ingresos y rechazos de las últimas 24 horas, con enlace a la bitácora de sistema. |
