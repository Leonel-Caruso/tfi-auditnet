## Alertas críticas

Menú `ALR`. Cada hallazgo de severidad **alta o crítica** genera una alerta (como máximo una vigente por equipo y
regla). Sirven para que alguien se haga cargo rápido.

![Alertas priorizadas con la alerta elegida](alertas.png)

Filtrá por estado (por defecto las vigentes), severidad, criticidad del equipo, dispositivo y sede. Los indicadores
muestran las vigentes, las sin atender, las derivadas y las críticas.

| Estado | Significa |
|---|---|
| activa | nadie la tomó todavía |
| en atención | se derivó a un perfil técnico |
| cerrada | su hallazgo se resolvió o se aceptó el riesgo |

### Derivar o devolver una alerta

![Detalle de la alerta con el hallazgo que la originó](alerta-derivar.png)

1. Elegí la alerta: ves el hallazgo que la originó, el equipo y su evidencia.
2. En **acción** elegí:
   - **derivar a Analista de redes** o **derivar a Auditor técnico**: pasa a *en atención*; el comentario es opcional
     (qué hay que revisar);
   - **devolver a activa (sin asignar):** el motivo es obligatorio.
3. Tocá **Actualizar alerta**.

Una alerta no se cierra a mano: se cierra sola cuando su hallazgo se resuelve o se acepta el riesgo. Para reactivarla,
hay que reabrir el hallazgo.
