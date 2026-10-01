package ar.edu.uai.tfi.auditcore.api.rest.dto.policy;

/** Cuerpo opcional de POST /api/baselines/{id}/versions. Si no se envía descripción, se conserva. */
public record NuevaVersionBaselineRequest(String descripcion) {
}
