package ar.edu.uai.tfi.auditcore.api.rest.dto.alert;

import ar.edu.uai.tfi.auditcore.api.rest.dto.finding.HallazgoResponse;

/**
 * Detalle de una alerta con su hallazgo de origen (evidencia, impacto, recomendación y auditoría).
 *
 * @param hallazgo null si el hallazgo no pudo obtenerse (CU-006-002, flujo alternativo 5)
 */
public record AlertaDetalleResponse(AlertaResponse alerta, HallazgoResponse hallazgo) {
}
