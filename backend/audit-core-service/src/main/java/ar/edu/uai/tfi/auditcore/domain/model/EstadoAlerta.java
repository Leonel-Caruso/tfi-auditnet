package ar.edu.uai.tfi.auditcore.domain.model;

/**
 * Estado de una alerta crítica (CU-006-002).
 * <ul>
 *   <li>ACTIVA: generada a partir de un hallazgo CRITICA o ALTA pendiente; nadie la tomó todavía.</li>
 *   <li>EN_ATENCION: derivada a un perfil técnico (analista de redes o auditor técnico).</li>
 *   <li>CERRADA: el hallazgo se resolvió o se aceptó el riesgo. Se conserva como evidencia.</li>
 * </ul>
 * El cierre y la reapertura siguen al hallazgo; en la pantalla de alertas solo se deriva o se devuelve.
 */
public enum EstadoAlerta {
    ACTIVA,
    EN_ATENCION,
    CERRADA;

    /** Vigente = requiere atención (ACTIVA o EN_ATENCION). */
    public boolean vigente() {
        return this != CERRADA;
    }
}
