package ar.edu.uai.tfi.management.domain.model;

import java.math.BigDecimal;

/**
 * Plan comercial del catálogo. Un máximo nulo significa que el límite se acuerda con cada cliente (plan a medida).
 *
 * @param diasVigencia     días que dura el plan desde su asignación; null si no vence
 * @param precioMensualUsd precio de referencia; null si se cotiza o se bonifica
 * @param condiciones      condiciones comerciales que se muestran al cliente
 */
public record Plan(
        Long id,
        String codigo,
        String nombre,
        Integer maxDispositivos,
        Integer maxUsuarios,
        Integer diasVigencia,
        BigDecimal precioMensualUsd,
        String condiciones,
        int orden
) {

    /** Los límites se acuerdan con cada cliente y se guardan en la organización. */
    public boolean aMedida() {
        return maxDispositivos == null || maxUsuarios == null;
    }

    /** Plan de prueba: vence a los {@link #diasVigencia} días. */
    public boolean conVencimiento() {
        return diasVigencia != null;
    }
}
