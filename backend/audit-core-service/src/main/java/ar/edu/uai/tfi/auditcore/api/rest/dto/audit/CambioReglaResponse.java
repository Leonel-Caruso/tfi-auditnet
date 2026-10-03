package ar.edu.uai.tfi.auditcore.api.rest.dto.audit;

/**
 * @param categoria NUEVO_HALLAZGO, CORREGIDO, PERSISTENTE, SIN_CAMBIO, REGLA_NUEVA o REGLA_RETIRADA
 */
public record CambioReglaResponse(
        String codigoRegla,
        String nombreRegla,
        String severidad,
        Boolean cumpleAntes,
        Boolean cumpleAhora,
        String categoria
) {
}
