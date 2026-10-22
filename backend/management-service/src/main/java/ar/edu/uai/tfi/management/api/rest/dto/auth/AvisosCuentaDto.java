package ar.edu.uai.tfi.management.api.rest.dto.auth;

import ar.edu.uai.tfi.management.domain.model.PreferenciasAviso;

/** Avisos por correo que el usuario elige recibir. */
public record AvisosCuentaDto(Boolean alertas, Boolean vencimientos, Boolean resumen) {

    public static AvisosCuentaDto de(PreferenciasAviso preferencias) {
        return new AvisosCuentaDto(preferencias.alertas(), preferencias.vencimientos(), preferencias.resumen());
    }
}
