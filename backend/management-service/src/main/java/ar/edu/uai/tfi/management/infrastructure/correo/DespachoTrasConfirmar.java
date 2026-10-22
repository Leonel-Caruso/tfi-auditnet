package ar.edu.uai.tfi.management.infrastructure.correo;

import ar.edu.uai.tfi.management.application.port.AvisoDespachoPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;

/** Publica un evento que se atiende recién cuando se confirma la transacción (ver {@link DespachoAsincrono}). */
@ApplicationScoped
public class DespachoTrasConfirmar implements AvisoDespachoPort {

    /** Pedido de despachar la bandeja de salida. */
    public record SolicitudDespacho() {
    }

    private final Event<SolicitudDespacho> evento;

    public DespachoTrasConfirmar(Event<SolicitudDespacho> evento) {
        this.evento = evento;
    }

    @Override
    public void solicitarDespacho() {
        evento.fire(new SolicitudDespacho());
    }
}
