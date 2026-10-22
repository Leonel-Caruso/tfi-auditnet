package ar.edu.uai.tfi.management.api.rest.dto.plan;

import ar.edu.uai.tfi.management.domain.model.Plan;

import java.math.BigDecimal;

/** Plan del catálogo. Un máximo nulo indica límite a medida; un precio nulo, que se cotiza o se bonifica. */
public record PlanResponse(String codigo, String nombre, Integer maxDispositivos, Integer maxUsuarios,
                           Integer diasVigencia, BigDecimal precioMensualUsd, String condiciones) {

    public static PlanResponse de(Plan plan) {
        return new PlanResponse(plan.codigo(), plan.nombre(), plan.maxDispositivos(), plan.maxUsuarios(),
                plan.diasVigencia(), plan.precioMensualUsd(), plan.condiciones());
    }
}
