package ar.edu.uai.tfi.management.domain.model;

import java.time.Instant;

/** Pedido de recuperación de contraseña. Solo se guarda el hash del token; vale una vez y por tiempo limitado. */
public record RecuperacionContrasena(
        Long id,
        Long usuarioId,
        String tokenHash,
        Instant creadaEn,
        Instant venceEn,
        Instant usadaEn
) {

    public boolean vigente(Instant ahora) {
        return usadaEn == null && ahora.isBefore(venceEn);
    }
}
