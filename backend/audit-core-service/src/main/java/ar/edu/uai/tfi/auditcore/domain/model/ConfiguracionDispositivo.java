package ar.edu.uai.tfi.auditcore.domain.model;

import java.time.Instant;

public record ConfiguracionDispositivo(
        Long id,
        Long dispositivoId,
        Long organizacionId,
        Integer version,
        FormatoConfiguracion formato,
        String nombreFuente,
        String contenidoOriginal,
        String contenidoNormalizado,
        Instant fechaImportacion,
        String usuarioResponsable
) {
}
