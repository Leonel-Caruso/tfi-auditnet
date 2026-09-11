package ar.edu.uai.tfi.management.infrastructure.tracing;

import ar.edu.uai.tfi.management.application.port.TrazabilidadPort;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

@ApplicationScoped
public class LogTrazabilidadAdapter implements TrazabilidadPort {
    private static final Logger LOG = Logger.getLogger(LogTrazabilidadAdapter.class);
    private final SecurityIdentity identity;

    public LogTrazabilidadAdapter(SecurityIdentity identity) {
        this.identity = identity;
    }

    @Override
    public void registrar(String accion, String detalle) {
        String actor = identity == null || identity.isAnonymous()
                ? "SISTEMA_O_ANONIMO"
                : identity.getPrincipal().getName();
        LOG.infof("TRAZABILIDAD actor=%s accion=%s detalle=%s", actor, accion, detalle);
    }
}
