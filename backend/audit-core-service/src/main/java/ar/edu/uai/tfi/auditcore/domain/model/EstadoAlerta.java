package ar.edu.uai.tfi.auditcore.domain.model;

/**
 * Estado de una alerta crítica: ACTIVA (nadie la tomó), EN_ATENCION (derivada a un perfil técnico) o CERRADA
 * (hallazgo resuelto o riesgo aceptado). El cierre y la reapertura siguen al hallazgo; en la pantalla de alertas
 * solo se deriva o se devuelve.
 */
public enum EstadoAlerta {
    ACTIVA,
    EN_ATENCION,
    CERRADA;

    public boolean vigente() {
        return this != CERRADA;
    }
}
