## Bitácoras

Menú `BIT`. Tres registros de solo lectura, cada uno en su pestaña:

| Bitácora | Qué registra | Dónde se guarda |
|---|---|---|
| Auditoría de sistema | accesos y seguridad: ingresos, rechazos, bloqueos, cambios de cuentas | PostgreSQL |
| Transacciones | operaciones de negocio con el valor anterior y el nuevo (altas, cambios, auditorías, planes) | PostgreSQL |
| Excepciones | errores inesperados de los servicios, con su traza | MongoDB |

![Bitácora de auditoría de sistema](bitacoras.png)

Filtrá por evento, resultado, usuario, entidad o servicio y por fechas, y tocá **Consultar**. Al elegir un registro ves
todos sus campos. El **código de seguimiento** identifica una misma operación en las tres bitácoras: desde el detalle,
*Filtrar por este código en las 3 bitácoras* muestra todo lo que pasó en esa operación.
