package ar.edu.uai.tfi.management.infrastructure.tracing;

import ar.edu.uai.tfi.management.application.port.TrazabilidadPort;
import ar.edu.uai.tfi.management.domain.model.RegistroTransaccion;
import ar.edu.uai.tfi.management.domain.model.Transaccion;
import ar.edu.uai.tfi.management.domain.repository.BitacoraTransaccionesRepository;
import ar.edu.uai.tfi.management.infrastructure.web.ContextoSolicitud;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.arc.Arc;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.time.Instant;

/**
 * Persiste la bitácora de transacciones (tabla bitacora_transacciones) en la MISMA transacción
 * que la operación de negocio: el registro existe solo si la operación se confirmó.
 * Los valores anterior y nuevo se guardan como JSON.
 */
@ApplicationScoped
public class PersistenciaTrazabilidadAdapter implements TrazabilidadPort {

    static final String SERVICIO = "management-service";
    private static final Logger LOG = Logger.getLogger(PersistenciaTrazabilidadAdapter.class);

    private final BitacoraTransaccionesRepository repository;
    private final ContextoSolicitud contexto;
    private final SecurityIdentity identity;
    private final ObjectMapper objectMapper;

    public PersistenciaTrazabilidadAdapter(BitacoraTransaccionesRepository repository,
                                           ContextoSolicitud contexto,
                                           SecurityIdentity identity,
                                           ObjectMapper objectMapper) {
        this.repository = repository;
        this.contexto = contexto;
        this.identity = identity;
        this.objectMapper = objectMapper;
    }

    @Override
    public void registrar(Transaccion transaccion) {
        boolean haySolicitud = Arc.container().requestContext().isActive();

        RegistroTransaccion registro = new RegistroTransaccion(
                null,
                Instant.now(),
                SERVICIO,
                transaccion.entidad(),
                transaccion.entidadId(),
                transaccion.operacion(),
                recortar(resolverActor(transaccion, haySolicitud), 160),
                transaccion.organizacionId(),
                aJson(transaccion.valorAnterior()),
                aJson(transaccion.valorNuevo()),
                haySolicitud ? contexto.correlacion() : null,
                recortar(transaccion.detalle(), 1000)
        );

        LOG.infof("BITACORA_TRANSACCIONES entidad=%s id=%s operacion=%s actor=%s",
                registro.entidad(), registro.entidadId(), registro.operacion(), registro.actor());

        QuarkusTransaction.joiningExisting().run(() -> repository.guardar(registro));
    }

    private String resolverActor(Transaccion transaccion, boolean haySolicitud) {
        if (transaccion.actor() != null && !transaccion.actor().isBlank()) {
            return transaccion.actor().trim();
        }
        if (haySolicitud && !identity.isAnonymous()) {
            return identity.getPrincipal().getName();
        }
        return "SISTEMA";
    }

    private String aJson(Object valor) {
        if (valor == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(valor);
        } catch (JsonProcessingException exception) {
            return String.valueOf(valor);
        }
    }

    private String recortar(String valor, int maximo) {
        if (valor == null) {
            return null;
        }
        return valor.length() <= maximo ? valor : valor.substring(0, maximo);
    }
}
