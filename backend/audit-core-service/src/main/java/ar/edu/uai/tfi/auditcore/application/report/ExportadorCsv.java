package ar.edu.uai.tfi.auditcore.application.report;

import ar.edu.uai.tfi.auditcore.domain.model.ContenidoReporte.AuditoriaIncluida;
import ar.edu.uai.tfi.auditcore.domain.model.ContenidoReporte.HallazgoIncluido;
import ar.edu.uai.tfi.auditcore.domain.model.ReporteAuditoria;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Descarga del reporte en CSV (GET /api/reports/{id}/download), para abrir con Excel.
 *
 * <ul>
 *   <li>Una fila por hallazgo; las auditorías sin hallazgos incluidos van en una fila con el resultado.</li>
 *   <li>Separador ";" y BOM UTF-8: Excel en español lo abre en columnas y con los acentos bien.</li>
 *   <li>Celdas que empiezan con = + - @ se anteponen con ' para que Excel no las tome como fórmulas.</li>
 * </ul>
 */
public final class ExportadorCsv {

    private static final String SEPARADOR = ";";
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(ReporteService.ZONA);

    private ExportadorCsv() {
    }

    public static String exportar(ReporteAuditoria reporte) {
        StringBuilder csv = new StringBuilder("﻿");
        linea(csv, List.of("Reporte", "Generado", "Generado por", "Auditoría", "Fecha auditoría", "Ejecutada por",
                "Dispositivo", "Criticidad", "Baseline", "Versión baseline", "Configuración", "Reglas cumplidas", "Reglas evaluadas",
                "Resultado auditoría", "Hallazgo", "Regla", "Nombre regla", "Severidad", "Impacto", "Estado hallazgo",
                "Evidencia", "Recomendación", "Detectado"));

        Map<Long, List<HallazgoIncluido>> porAuditoria = reporte.contenido().hallazgos().stream()
                .collect(Collectors.groupingBy(HallazgoIncluido::auditoriaId));
        for (AuditoriaIncluida auditoria : reporte.contenido().auditorias()) {
            List<String> base = List.of(
                    codigo("REP", reporte.id()),
                    fecha(reporte.fechaGeneracion()),
                    texto(reporte.generadoPor()),
                    codigo("AUD", auditoria.id()),
                    fecha(auditoria.fechaEjecucion()),
                    texto(auditoria.ejecutadoPor()),
                    texto(auditoria.dispositivoIdentificador()),
                    texto(auditoria.criticidadDispositivo()),
                    texto(auditoria.baselineNombre()),
                    auditoria.baselineVersion() == null ? "" : "v" + auditoria.baselineVersion(),
                    codigo("CFG", auditoria.configuracionId()),
                    String.valueOf(auditoria.reglasCumplidas()),
                    String.valueOf(auditoria.totalReglas()),
                    texto(auditoria.resultado()));
            List<HallazgoIncluido> hallazgos = porAuditoria.getOrDefault(auditoria.id(), List.of());
            if (hallazgos.isEmpty()) {
                linea(csv, concatenar(base, List.of("", "", "", "", "", "", "", "", "")));
                continue;
            }
            for (HallazgoIncluido hallazgo : hallazgos) {
                linea(csv, concatenar(base, List.of(
                        codigo("FND", hallazgo.id()),
                        texto(hallazgo.codigoRegla()),
                        texto(hallazgo.nombreRegla()),
                        texto(hallazgo.severidad()),
                        texto(hallazgo.impacto()),
                        texto(hallazgo.estado()),
                        texto(hallazgo.evidencia()),
                        texto(hallazgo.recomendacion()),
                        fecha(hallazgo.fechaDeteccion()))));
            }
        }
        return csv.toString();
    }

    /** Escapa una celda: comillas si tiene separador, comillas o saltos de línea; neutraliza fórmulas. */
    static String celda(String valor) {
        String limpio = valor == null ? "" : valor;
        if (!limpio.isEmpty() && "=+-@\t\r".indexOf(limpio.charAt(0)) >= 0) {
            limpio = "'" + limpio;
        }
        if (limpio.contains(SEPARADOR) || limpio.contains("\"") || limpio.contains("\n") || limpio.contains("\r")) {
            limpio = "\"" + limpio.replace("\"", "\"\"") + "\"";
        }
        return limpio;
    }

    private static void linea(StringBuilder csv, List<String> celdas) {
        csv.append(celdas.stream().map(ExportadorCsv::celda).collect(Collectors.joining(SEPARADOR))).append("\r\n");
    }

    private static List<String> concatenar(List<String> a, List<String> b) {
        return java.util.stream.Stream.concat(a.stream(), b.stream()).toList();
    }

    private static String codigo(String prefijo, Long id) {
        return id == null ? "" : String.format("%s-%03d", prefijo, id);
    }

    private static String fecha(Instant instante) {
        return instante == null ? "" : FECHA.format(instante);
    }

    private static String texto(String valor) {
        return valor == null ? "" : valor;
    }
}
