package ar.edu.uai.tfi.auditcore.api.rest.dto.alert;

/**
 * PATCH /api/alerts/{id}/status.
 *
 * @param estado     EN_ATENCION (derivar) o ACTIVA (devolver)
 * @param perfil     ANALISTA_RED o AUDITOR_TECNICO; obligatorio para EN_ATENCION
 * @param comentario opcional al derivar; obligatorio al devolver
 */
public record CambiarEstadoAlertaRequest(String estado, String perfil, String comentario) {
}
