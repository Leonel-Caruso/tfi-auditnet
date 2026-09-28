package ar.edu.uai.tfi.management.domain.model;

import java.time.Instant;

/** Documento de la bitácora de excepciones (se guarda en MongoDB). */
public record RegistroExcepcion(
        Instant fecha,
        String servicio,
        NivelExcepcion nivel,
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
