package ar.edu.uai.tfi.management.domain.repository;

import ar.edu.uai.tfi.management.domain.model.RecuperacionContrasena;

import java.time.Instant;
import java.util.Optional;

public interface RecuperacionRepository {

    RecuperacionContrasena guardar(RecuperacionContrasena recuperacion);

    /** Bloquea el pedido hasta el fin de la transacción: el mismo enlace no se usa dos veces. */
    Optional<RecuperacionContrasena> buscarParaUsar(String tokenHash);

    /** Fecha del último pedido del usuario, para no enviar correos seguidos. */
    Optional<Instant> ultimoPedido(Long usuarioId);

    /** Deja sin efecto los enlaces pendientes del usuario. */
    void anularPendientes(Long usuarioId, Instant ahora);
}
