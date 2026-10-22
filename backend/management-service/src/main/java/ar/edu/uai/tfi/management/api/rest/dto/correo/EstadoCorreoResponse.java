package ar.edu.uai.tfi.management.api.rest.dto.correo;

import java.util.List;

/** Panel de administración: proveedor configurado, cantidades de las últimas 24 h y últimos avisos. */
public record EstadoCorreoResponse(String proveedor, long pendientes, long enviados24h, long fallidos24h,
                                   List<CorreoResponse> ultimos) {
}
