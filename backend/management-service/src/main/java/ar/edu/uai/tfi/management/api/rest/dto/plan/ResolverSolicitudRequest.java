package ar.edu.uai.tfi.management.api.rest.dto.plan;

/** Respuesta para el cliente; los límites solo se indican al aprobar un plan a medida. */
public record ResolverSolicitudRequest(String respuesta, Integer limiteDispositivos, Integer limiteUsuarios) {
}
