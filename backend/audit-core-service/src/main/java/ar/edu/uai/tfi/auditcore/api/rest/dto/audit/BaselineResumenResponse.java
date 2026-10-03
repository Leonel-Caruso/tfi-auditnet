package ar.edu.uai.tfi.auditcore.api.rest.dto.audit;

/** Baseline utilizada como referencia por una auditoría. */
public record BaselineResumenResponse(Long id, String nombre, Integer version, String estado) {
}
