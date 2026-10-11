package ar.edu.uai.tfi.auditcore.application.port;

import java.io.OutputStream;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Copia lógica de la base de datos (Bloque 7, D-36): datos de todas las tablas en CSV, versión del esquema según
 * Flyway y el guion para restaurarlos, empaquetados en un ZIP.
 */
public interface ExportadorBaseDatosPort {

    ResumenExportacion exportar(OutputStream destino, MetadatosRespaldo metadatos);

    record MetadatosRespaldo(String nombre, String generadoPor, Instant fecha, String versionAplicacion) {
    }

    record TablaExportada(String tabla, long filas, String sha256) {
    }

    /** @param versionesEsquema tabla de historial de Flyway → última versión aplicada */
    record ResumenExportacion(List<TablaExportada> tablas, Map<String, String> versionesEsquema) {
        public long filas() {
            return tablas.stream().mapToLong(TablaExportada::filas).sum();
        }
    }
}
