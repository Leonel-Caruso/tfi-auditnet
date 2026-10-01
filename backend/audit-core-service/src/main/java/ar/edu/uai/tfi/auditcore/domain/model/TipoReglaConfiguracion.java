package ar.edu.uai.tfi.auditcore.domain.model;

/**
 * Criterios de comparación que interpreta el motor de auditoría
 * (presencia, ausencia o valor esperado de un comando o parámetro).
 */
public enum TipoReglaConfiguracion {
    /** Cumple si el patrón aparece en la configuración. */
    DEBE_CONTENER,
    /** Cumple si el patrón no aparece en la configuración. */
    NO_DEBE_CONTENER,
    /** Cumple si alguna línea que empieza con el parámetro tiene exactamente el valor esperado. */
    VALOR_ESPERADO
}
