package ar.edu.uai.tfi.management.api.rest.dto.bitacora;

import java.time.Instant;

public record RegistroExcepcionResponse(
        Instant fecha,
        String servicio,
        String nivel,
        String tipo,
        String mensaje,
        String traza,
        String metodoHttp,
        String ruta,
        int estadoHttp,
        String usuario,
        String correlacion
) {
}
