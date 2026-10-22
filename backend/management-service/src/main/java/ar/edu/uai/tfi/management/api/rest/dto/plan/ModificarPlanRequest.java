package ar.edu.uai.tfi.management.api.rest.dto.plan;

import java.math.BigDecimal;

public record ModificarPlanRequest(BigDecimal precioMensualUsd, String condiciones) {
}
