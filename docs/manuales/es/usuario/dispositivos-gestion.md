## Dispositivos

Menú `DEV`. Es el inventario de equipos de red que se pueden auditar. Arriba están la búsqueda (identificador, nombre o
fabricante) y los filtros por criticidad y estado; al elegir un equipo, a la derecha aparece su detalle.

![Inventario de dispositivos con el detalle del equipo elegido](dispositivos.png)

### Registrar un dispositivo

1. Tocá **+ registrar activo**.
2. Completá:
   - **identificador:** código único dentro de la organización, por ejemplo `ROS-RTR-01`;
   - **nombre** descriptivo;
   - **tipo de dispositivo** (el fabricante se completa según el tipo);
   - **organización** (el administrador del sistema la elige; el de organización registra en la suya);
   - **sede** (opcional);
   - **criticidad:** baja, media, alta o crítica. Pesa en el nivel de riesgo y en las alertas.
3. Tocá **Registrar dispositivo**. Queda en estado activo.

![Alta de un dispositivo](dispositivo-alta.png)

El alta cuenta para el límite de dispositivos activos del plan de la organización.

### Modificar, dar de baja o reactivar

En el detalle del equipo: **Editar** cambia sus datos (la organización no se cambia). **Dar de baja** lo deja inactivo:
conserva su historial, pero no admite configuraciones ni auditorías nuevas. **Reactivar** lo vuelve a habilitar. Las
dos acciones piden confirmación y todos los cambios quedan en la bitácora de transacciones con el valor anterior y el
nuevo.

### Tipos de dispositivo

Los tipos (por ejemplo *Router ISR 4331 · Cisco · IOS-XE*) son un catálogo común a todas las organizaciones y las
baselines se definen por tipo. Solo el administrador del sistema los registra, con **+ tipo**: nombre, fabricante y
familia o sistema operativo.
