package ar.edu.uai.tfi.management.domain.model;

import java.time.Instant;

/**
 * Fila persistida de la bitácora de auditoría de sistema: el evento más el contexto
 * de la solicitud HTTP en la que ocurrió.
 */
public record RegistroBitacoraSistema(
        Long id,
        Instant fecha,
        TipoEventoSistema tipo,
        ResultadoEvento resultado,
        String actor,
        Long usuarioAfectadoId,
        Long organizacionId,
        String origenIp,
        String userAgent,
        String correlacion,
        String detalle
) {
}
