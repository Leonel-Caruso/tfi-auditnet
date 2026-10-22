package ar.edu.uai.tfi.management.domain.repository;

import ar.edu.uai.tfi.management.domain.model.CorreoSaliente;

import java.time.Instant;
import java.util.List;

public interface CorreoRepository {

    CorreoSaliente guardar(CorreoSaliente correo);

    /**
     * Pendientes cuyo próximo intento ya llegó, los más viejos primero. Los bloquea hasta el fin de la transacción
     * y saltea los que otro despacho tiene tomados: un aviso no se envía dos veces.
     */
    List<CorreoSaliente> tomarPendientes(Instant ahora, int maximo);

    /** Actualiza estado, intentos, próximo intento, datos y resultado del envío. */
    void actualizar(CorreoSaliente correo);

    /** Cantidades para el panel de administración. */
    record Resumen(long pendientes, long enviados, long fallidos) {
    }

    /** Pendientes totales; enviados desde {@code desde} y fallidos entre los creados desde {@code desde}. */
    Resumen resumen(Instant desde);

    /** Los más recientes primero. */
    List<CorreoSaliente> ultimos(int maximo);
}
