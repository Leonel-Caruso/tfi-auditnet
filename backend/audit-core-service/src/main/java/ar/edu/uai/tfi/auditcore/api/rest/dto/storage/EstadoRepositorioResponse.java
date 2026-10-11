package ar.edu.uai.tfi.auditcore.api.rest.dto.storage;

import java.util.Map;

/**
 * GET /api/system/storage.
 *
 * @param archivos tipo → {cantidad, bytes}
 */
public record EstadoRepositorioResponse(boolean configurado, String contenedor, Map<String, Totales> archivos,
                                        long configuracionesPendientes, long reportesPendientes) {

    public record Totales(long cantidad, long bytes) {
    }
}
