package ar.edu.uai.tfi.management.api.rest.dto.plan;

/** Los límites solo se indican para un plan a medida y los días, para un plan de prueba. */
public record AsignarPlanRequest(String plan, Integer limiteDispositivos, Integer limiteUsuarios, Integer diasPrueba) {
}
