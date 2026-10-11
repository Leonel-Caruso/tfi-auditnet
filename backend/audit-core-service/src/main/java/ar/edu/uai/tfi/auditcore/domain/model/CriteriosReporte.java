package ar.edu.uai.tfi.auditcore.domain.model;

import java.time.Instant;
import java.util.List;

/**
 * Criterios con los que se generó un reporte (CU-006-001, paso 3). Se guardan junto al reporte.
 *
 * @param organizacionId null solo si el administrador lo generó para todas las organizaciones
 * @param dispositivoIds vacío = todos los dispositivos
 * @param severidades    vacío = todas (filtra los hallazgos incluidos)
 * @param estados        vacío = todos (filtra los hallazgos incluidos)
 * @param auditoriaIds   auditorías específicas; si se indican, el rango de fechas es opcional
 */
public record CriteriosReporte(
        Long organizacionId,
        Instant desde,
        Instant hasta,
        List<Long> dispositivoIds,
        List<SeveridadRegla> severidades,
        List<EstadoHallazgo> estados,
        List<Long> auditoriaIds
) {

    public CriteriosReporte {
        dispositivoIds = dispositivoIds == null ? List.of() : List.copyOf(dispositivoIds);
        severidades = severidades == null ? List.of() : List.copyOf(severidades);
        estados = estados == null ? List.of() : List.copyOf(estados);
        auditoriaIds = auditoriaIds == null ? List.of() : List.copyOf(auditoriaIds);
    }

    public CriteriosReporte conOrganizacion(Long organizacion) {
        return new CriteriosReporte(organizacion, desde, hasta, dispositivoIds, severidades, estados, auditoriaIds);
    }
}
