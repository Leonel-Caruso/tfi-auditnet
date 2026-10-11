package ar.edu.uai.tfi.management.api.rest.dto.bitacora;

import java.time.Instant;

public record RegistroBitacoraSistemaResponse(
        Long id,
        Instant fecha,
        String evento,
        String resultado,
        String actor,
        Long usuarioAfectadoId,
        Long organizacionId,
        String origenIp,
        String userAgent,
        String correlacion,
        String detalle
) {

    public static RegistroBitacoraSistemaResponse de(ar.edu.uai.tfi.management.domain.model.RegistroBitacoraSistema registro) {
        return new RegistroBitacoraSistemaResponse(
                registro.id(),
                registro.fecha(),
                registro.tipo().name(),
                registro.resultado().name(),
                registro.actor(),
                registro.usuarioAfectadoId(),
                registro.organizacionId(),
                registro.origenIp(),
                registro.userAgent(),
                registro.correlacion(),
                registro.detalle()
        );
    }
}
