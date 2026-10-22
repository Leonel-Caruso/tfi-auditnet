package ar.edu.uai.tfi.management.infrastructure.correo;

import ar.edu.uai.tfi.management.application.correo.CorreoService;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import org.jboss.logging.Logger;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Despacha la bandeja de salida en segundo plano después de confirmar la transacción que guardó el aviso: si la
 * transacción se revierte no se envía nada, y la respuesta al usuario no espera al proveedor de correo. Los pedidos
 * que llegan mientras hay uno en espera se juntan en un solo despacho; si quedan avisos, sigue al minuto siguiente
 * (el dominio de Azure admite 5 envíos por minuto).
 */
@ApplicationScoped
public class DespachoAsincrono {

    private static final Logger LOG = Logger.getLogger(DespachoAsincrono.class);
    private static final long SEGUNDOS_ENTRE_TANDAS = 61;

    private final CorreoService correos;
    private final ScheduledExecutorService hilo = Executors.newSingleThreadScheduledExecutor(tarea -> {
        Thread thread = new Thread(tarea, "despacho-correo");
        thread.setDaemon(true);
        return thread;
    });
    private final AtomicBoolean enEspera = new AtomicBoolean();

    public DespachoAsincrono(CorreoService correos) {
        this.correos = correos;
    }

    void alConfirmar(@Observes(during = TransactionPhase.AFTER_SUCCESS) DespachoTrasConfirmar.SolicitudDespacho pedido) {
        programar(0);
    }

    private void programar(long segundos) {
        if (enEspera.compareAndSet(false, true)) {
            hilo.schedule(this::despachar, segundos, TimeUnit.SECONDS);
        }
    }

    private void despachar() {
        enEspera.set(false);
        try {
            CorreoService.ResultadoDespacho resultado = correos.despachar();
            if (resultado.enviados() > 0 && resultado.pendientes() > 0) {
                programar(SEGUNDOS_ENTRE_TANDAS);
            }
        } catch (RuntimeException error) {
            // Los avisos quedan pendientes y se reintentan en el próximo despacho.
            LOG.warnf("No se pudo despachar la bandeja de salida: %s", error.getMessage());
        }
    }

    @PreDestroy
    void cerrar() {
        hilo.shutdownNow();
    }
}
