package ar.edu.uai.tfi.management.api.rest.dto.b2b;

import ar.edu.uai.tfi.management.domain.model.Sede;

public record SedeResponse(Long id, Long organizationId, String nombre, String ubicacion, String estado) {

    public static SedeResponse de(Sede sede) {
        return new SedeResponse(sede.id(), sede.organizacionId(), sede.nombre(), sede.ubicacion(),
                sede.estado().name());
    }
}
