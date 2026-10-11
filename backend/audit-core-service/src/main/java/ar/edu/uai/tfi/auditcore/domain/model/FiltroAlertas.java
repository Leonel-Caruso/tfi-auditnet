package ar.edu.uai.tfi.auditcore.domain.model;

/**
 * Criterios de GET /api/alerts/critical. Los null no filtran.
 *
 * @param organizacionId null para el administrador (todas las organizaciones)
 * @param estado         estado puntual; si es null y {@code soloVigentes} es true, ACTIVA o EN_ATENCION
 * @param soloVigentes   true por defecto: el listado muestra las alertas que requieren atención
 */
public record FiltroAlertas(
        Long organizacionId,
        EstadoAlerta estado,
        boolean soloVigentes,
        SeveridadRegla severidad,
        Long dispositivoId
) {

    public static FiltroAlertas vigentes(Long organizacionId) {
        return new FiltroAlertas(organizacionId, null, true, null, null);
    }

    public static FiltroAlertas todas(Long organizacionId) {
        return new FiltroAlertas(organizacionId, null, false, null, null);
    }
}
