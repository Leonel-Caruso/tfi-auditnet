package ar.edu.uai.tfi.auditcore.infrastructure.tracing;

import ar.edu.uai.tfi.auditcore.application.port.TrazabilidadPort;
import ar.edu.uai.tfi.auditcore.domain.model.Transaccion;
import ar.edu.uai.tfi.auditcore.infrastructure.web.ContextoSolicitud;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.arc.Arc;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.persistence.EntityManager;
import org.hibernate.Session;
import org.jboss.logging.Logger;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Escribe en bitacora_transacciones dentro de la misma transacción que la operación de negocio. La tabla es de
 * management-service, por eso no se mapea como entidad y se inserta con SQL explícito. El EntityManager se
 * obtiene recién al registrar para que el bean no exija persistencia al iniciar (tests sin base).
 */
@ApplicationScoped
public class PersistenciaTrazabilidadAdapter implements TrazabilidadPort {

    static final String SERVICIO = "audit-core-service";
    private static final Logger LOG = Logger.getLogger(PersistenciaTrazabilidadAdapter.class);

    private static final String INSERT = """
            INSERT INTO bitacora_transacciones
                (fecha, servicio, entidad, id_entidad, operacion, actor, id_organizacion,
                 valor_anterior, valor_nuevo, correlacion, detalle)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private final Instance<EntityManager> entityManager;
    private final ContextoSolicitud contexto;
    private final SecurityIdentity identity;
    private final ObjectMapper objectMapper;

    public PersistenciaTrazabilidadAdapter(Instance<EntityManager> entityManager,
                                           ContextoSolicitud contexto,
                                           SecurityIdentity identity,
                                           ObjectMapper objectMapper) {
        this.entityManager = entityManager;
        this.contexto = contexto;
        this.identity = identity;
        this.objectMapper = objectMapper;
    }

    @Override
    public void registrar(Transaccion transaccion) {
        boolean haySolicitud = Arc.container().requestContext().isActive();
        String actor = recortar(resolverActor(transaccion, haySolicitud), 160);
        String correlacion = haySolicitud ? contexto.correlacion() : null;
        String valorAnterior = aJson(transaccion.valorAnterior());
        String valorNuevo = aJson(transaccion.valorNuevo());
        String detalle = recortar(transaccion.detalle(), 1000);

        LOG.infof("BITACORA_TRANSACCIONES entidad=%s id=%s operacion=%s actor=%s",
                transaccion.entidad(), transaccion.entidadId(), transaccion.operacion(), actor);

        QuarkusTransaction.joiningExisting().run(() ->
                entityManager.get().unwrap(Session.class).doWork(conexion -> {
                    try (PreparedStatement sentencia = conexion.prepareStatement(INSERT)) {
                        sentencia.setObject(1, OffsetDateTime.ofInstant(Instant.now(), ZoneOffset.UTC));
                        sentencia.setString(2, SERVICIO);
                        sentencia.setString(3, transaccion.entidad());
                        setLong(sentencia, 4, transaccion.entidadId());
                        sentencia.setString(5, transaccion.operacion().name());
                        sentencia.setString(6, actor);
                        setLong(sentencia, 7, transaccion.organizacionId());
                        setTexto(sentencia, 8, valorAnterior);
                        setTexto(sentencia, 9, valorNuevo);
                        setTexto(sentencia, 10, correlacion);
                        setTexto(sentencia, 11, detalle);
                        sentencia.executeUpdate();
                    }
                }));
    }

    private static void setLong(PreparedStatement sentencia, int posicion, Long valor) throws SQLException {
        if (valor == null) {
            sentencia.setNull(posicion, Types.BIGINT);
        } else {
            sentencia.setLong(posicion, valor);
        }
    }

    private static void setTexto(PreparedStatement sentencia, int posicion, String valor) throws SQLException {
        if (valor == null) {
            sentencia.setNull(posicion, Types.VARCHAR);
        } else {
            sentencia.setString(posicion, valor);
        }
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

    private static String recortar(String valor, int maximo) {
        if (valor == null) {
            return null;
        }
        return valor.length() <= maximo ? valor : valor.substring(0, maximo);
    }
}
