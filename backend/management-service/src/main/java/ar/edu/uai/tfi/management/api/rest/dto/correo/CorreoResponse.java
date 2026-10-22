package ar.edu.uai.tfi.management.api.rest.dto.correo;

import ar.edu.uai.tfi.management.domain.model.CorreoSaliente;

import java.time.Instant;

public record CorreoResponse(Long id, String origen, String tipo, String destinatario, String estado, int intentos,
                             Instant creadoEn, Instant proximoIntento, Instant enviadoEn, String ultimoError) {

    public static CorreoResponse de(CorreoSaliente correo) {
        return new CorreoResponse(correo.id(), correo.origen(), correo.tipo().name(), correo.destinatario(),
                correo.estado().name(), correo.intentos(), correo.creadoEn(), correo.proximoIntento(),
                correo.enviadoEn(), correo.ultimoError());
    }
}
