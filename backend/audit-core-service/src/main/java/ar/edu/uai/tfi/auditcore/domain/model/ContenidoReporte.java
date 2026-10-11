package ar.edu.uai.tfi.auditcore.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Contenido del reporte (CU-006-001, paso 10): resumen, auditorías incluidas y hallazgos con su evidencia. Es una
 * foto fija tomada al generarlo: si después cambian los datos de origen, el reporte no cambia (regla 5).
 */
public record ContenidoReporte(
        Resumen resumen,
        List<AuditoriaIncluida> auditorias,
        List<HallazgoIncluido> hallazgos,
        List<String> inconsistencias
) {

    /**
     * @param cumplimiento     porcentaje de reglas cumplidas sobre todas las evaluadas (null si no hubo reglas)
     * @param resultadoGeneral SIN_DESVIOS o CON_DESVIOS, según los hallazgos incluidos
     * @param severidadMaxima  NINGUNA, BAJA, MEDIA, ALTA o CRITICA
     */
    public record Resumen(
            int auditorias,
            int dispositivos,
            int reglasEvaluadas,
            int reglasCumplidas,
            Integer cumplimiento,
            int hallazgos,
            int hallazgosPendientes,
            Map<String, Long> hallazgosPorSeveridad,
            Map<String, Long> hallazgosPorEstado,
            String severidadMaxima,
            String resultadoGeneral
    ) {
    }

    /** Auditoría de origen con el dispositivo, la configuración y la baseline usadas (regla 4). */
    public record AuditoriaIncluida(
            Long id,
            Instant fechaEjecucion,
            String ejecutadoPor,
            Long dispositivoId,
            String dispositivoIdentificador,
            String dispositivoNombre,
            String criticidadDispositivo,
            Long sedeId,
            Long configuracionId,
            Integer configuracionVersion,
            Long baselineId,
            String baselineNombre,
            Integer baselineVersion,
            int totalReglas,
            int reglasCumplidas,
            int totalHallazgos,
            String severidadMaxima,
            String resultado
    ) {
    }

    /** Hallazgo con severidad, impacto, evidencia y estado al momento de generar el reporte (regla 3). */
    public record HallazgoIncluido(
            Long id,
            Long auditoriaId,
            Long dispositivoId,
            String dispositivoIdentificador,
            Long reglaId,
            String codigoRegla,
            String nombreRegla,
            String severidad,
            String impacto,
            String evidencia,
            String recomendacion,
            String estado,
            Instant fechaDeteccion
    ) {
    }
}
