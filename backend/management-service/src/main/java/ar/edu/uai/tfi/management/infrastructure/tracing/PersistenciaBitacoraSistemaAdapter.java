package ar.edu.uai.tfi.management.infrastructure.tracing;

import ar.edu.uai.tfi.management.application.port.BitacoraSistemaPort;
import ar.edu.uai.tfi.management.domain.model.EventoSistema;
import ar.edu.uai.tfi.management.domain.model.RegistroBitacoraSistema;
import ar.edu.uai.tfi.management.domain.model.ResultadoEvento;
import ar.edu.uai.tfi.management.domain.repository.BitacoraSistemaRepository;
import ar.edu.uai.tfi.management.infrastructure.web.ContextoSolicitud;
import io.quarkus.arc.Arc;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.time.Instant;

/**
 * Persiste la bitácora de auditoría de sistema en PostgreSQL (tabla bitacora_sistema).
 *
 * Regla de transacciones:
 * <ul>
 *   <li>FALLO (por ejemplo, un login rechazado): se guarda en una transacción nueva, para que quede
 *       registrado aunque la operación termine con error. Si no se puede guardar, se deja constancia
 *       en el log y la solicitud sigue su curso.</li>
 *   <li>EXITO: se guarda en la misma transacción de la operación. Así el registro existe solo si la
 *       operación se confirmó, y si el registro no se puede guardar la operación tampoco se confirma.</li>
 * </ul>
 */
@ApplicationScoped
public class PersistenciaBitacoraSistemaAdapter implements BitacoraSistemaPort {

    static final String ACTOR_SISTEMA = "SISTEMA";
    private static final Logger LOG = Logger.getLogger(PersistenciaBitacoraSistemaAdapter.class);

    private final BitacoraSistemaRepository repository;
    private final ContextoSolicitud contexto;
    private final SecurityIdentity identity;

    public PersistenciaBitacoraSistemaAdapter(BitacoraSistemaRepository repository,
                                              ContextoSolicitud contexto,
                                              SecurityIdentity identity) {
        this.repository = repository;
        this.contexto = contexto;
        this.identity = identity;
    }

    @Override
    public void registrar(EventoSistema evento) {
        // Fuera de una solicitud HTTP (por ejemplo, el bootstrap al iniciar) no hay contexto web.
        boolean haySolicitud = Arc.container().requestContext().isActive();

        RegistroBitacoraSistema registro = new RegistroBitacoraSistema(
                null,
                Instant.now(),
                evento.tipo(),
                evento.resultado(),
                recortar(resolverActor(evento, haySolicitud), 160),
                evento.usuarioAfectadoId(),
                evento.organizacionId(),
                haySolicitud ? recortar(contexto.origenIp(), 200) : null,
                haySolicitud ? recortar(contexto.userAgent(), 300) : null,
                haySolicitud ? contexto.correlacion() : null,
                recortar(evento.detalle(), 1000)
        );

        LOG.infof("BITACORA_SISTEMA evento=%s resultado=%s actor=%s correlacion=%s",
                registro.tipo(), registro.resultado(), registro.actor(), registro.correlacion());

        if (evento.resultado() == ResultadoEvento.FALLO) {
            try {
                QuarkusTransaction.requiringNew().run(() -> repository.guardar(registro));
            } catch (RuntimeException exception) {
                LOG.errorf(exception, "No se pudo guardar en la bitácora de sistema el evento %s", registro.tipo());
            }
        } else {
            QuarkusTransaction.joiningExisting().run(() -> repository.guardar(registro));
        }
    }

    private String resolverActor(EventoSistema evento, boolean haySolicitud) {
        if (evento.actor() != null && !evento.actor().isBlank()) {
            return evento.actor().trim();
        }
        if (haySolicitud && !identity.isAnonymous()) {
            return identity.getPrincipal().getName();
        }
        return ACTOR_SISTEMA;
    }

    private String recortar(String valor, int maximo) {
        if (valor == null) {
            return null;
        }
        return valor.length() <= maximo ? valor : valor.substring(0, maximo);
    }
}
