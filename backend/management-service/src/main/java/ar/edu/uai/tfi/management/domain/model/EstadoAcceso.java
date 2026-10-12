package ar.edu.uai.tfi.management.domain.model;

import java.time.Instant;

/**
 * Estado de acceso de una cuenta: intentos fallidos, bloqueo temporal, último acceso y desde cuándo valen sus
 * sesiones.
 *
 * @param sesionesValidasDesde los tokens emitidos antes de este momento ya no valen (null = todos valen)
 */
public record EstadoAcceso(
        Long usuarioId,
        int intentosFallidos,
        Instant bloqueadoHasta,
        Instant ultimoAcceso,
        Instant sesionesValidasDesde
) {

    public static EstadoAcceso inicial(Long usuarioId) {
        return new EstadoAcceso(usuarioId, 0, null, null, null);
    }

    public boolean bloqueado(Instant ahora) {
        return bloqueadoHasta != null && bloqueadoHasta.isAfter(ahora);
    }
}
