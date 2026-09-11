package ar.edu.uai.tfi.auditcore.infrastructure.tracing;

import ar.edu.uai.tfi.auditcore.application.port.TrazabilidadPort;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

@ApplicationScoped
public class LogTrazabilidadAdapter implements TrazabilidadPort {

    private static final Logger LOG = Logger.getLogger(LogTrazabilidadAdapter.class);

    @Override
    public void registrar(String actor, String accion, String detalle) {
        LOG.infof(
                "TRAZABILIDAD actor=%s accion=%s detalle=%s",
                actor,
                accion,
                detalle
        );
    }
}
