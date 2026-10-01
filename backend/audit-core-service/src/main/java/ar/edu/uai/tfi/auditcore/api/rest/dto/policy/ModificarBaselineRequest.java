package ar.edu.uai.tfi.auditcore.api.rest.dto.policy;

/** Cuerpo de PUT /api/baselines/{id}: solo la descripción (los criterios se cambian versionando). */
public record ModificarBaselineRequest(String descripcion) {
}
