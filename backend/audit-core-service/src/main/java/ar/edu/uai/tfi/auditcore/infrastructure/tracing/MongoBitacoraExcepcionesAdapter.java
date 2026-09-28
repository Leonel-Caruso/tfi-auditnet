package ar.edu.uai.tfi.auditcore.infrastructure.tracing;

import ar.edu.uai.tfi.auditcore.application.port.BitacoraExcepcionesPort;
import ar.edu.uai.tfi.auditcore.domain.model.NivelExcepcion;
import ar.edu.uai.tfi.auditcore.domain.model.RegistroExcepcion;
import ar.edu.uai.tfi.auditcore.domain.repository.BitacoraExcepcionesRepository;
import ar.edu.uai.tfi.auditcore.infrastructure.web.ContextoSolicitud;
import io.quarkus.arc.Arc;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.jboss.logging.Logger;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.Locale;

/**
 * Registra errores en la bitácora de excepciones (MongoDB).
 *
 * Nivel configurable con la variable BITACORA_EXCEPCIONES_NIVEL:
 * <ul>
 *   <li>ERROR (por defecto): solo fallas inesperadas.</li>
 *   <li>WARN: además, errores de uso rechazados (4xx).</li>
 *   <li>OFF: no persiste nada (los errores siguen en el log del servicio).</li>
 * </ul>
 * La escritura es asíncrona: si MongoDB está lento o caído, la respuesta al usuario no se demora.
 */
@ApplicationScoped
public class MongoBitacoraExcepcionesAdapter implements BitacoraExcepcionesPort {

    static final String SERVICIO = "audit-core-service";
    private static final int MAXIMO_TRAZA = 4000;
    private static final Logger LOG = Logger.getLogger(MongoBitacoraExcepcionesAdapter.class);

    private final BitacoraExcepcionesRepository repository;
    private final ContextoSolicitud contexto;
    private final SecurityIdentity identity;
    private final ManagedExecutor executor;
    private final String nivelConfigurado;

    public MongoBitacoraExcepcionesAdapter(BitacoraExcepcionesRepository repository,
                                           ContextoSolicitud contexto,
                                           SecurityIdentity identity,
                                           ManagedExecutor executor,
                                           @ConfigProperty(name = "auditnet.bitacora.excepciones.nivel",
                                                   defaultValue = "ERROR") String nivelConfigurado) {
        this.repository = repository;
        this.contexto = contexto;
        this.identity = identity;
        this.executor = executor;
        this.nivelConfigurado = nivelConfigurado.trim().toUpperCase(Locale.ROOT);
    }

    @Override
    public void registrar(Throwable error, NivelExcepcion nivel, int estadoHttp) {
        if (nivel == NivelExcepcion.ERROR) {
            LOG.errorf(error, "Error inesperado (HTTP %d)", estadoHttp);
        }
        if (!debePersistir(nivel)) {
            return;
        }

        boolean haySolicitud = Arc.container().requestContext().isActive();
        RegistroExcepcion registro = new RegistroExcepcion(
                Instant.now(),
                SERVICIO,
                nivel,
                error.getClass().getName(),
                recortar(error.getMessage(), 1000),
                recortar(traza(error), MAXIMO_TRAZA),
                haySolicitud ? contexto.metodoHttp() : null,
                haySolicitud ? contexto.ruta() : null,
                estadoHttp,
                haySolicitud && !identity.isAnonymous() ? identity.getPrincipal().getName() : null,
                haySolicitud ? contexto.correlacion() : null
        );

        executor.runAsync(() -> {
            try {
                repository.guardar(registro);
            } catch (RuntimeException falla) {
                LOG.errorf(falla, "No se pudo guardar en la bitácora de excepciones (correlacion=%s)",
                        registro.correlacion());
            }
        });
    }

    boolean debePersistir(NivelExcepcion nivel) {
        return switch (nivelConfigurado) {
            case "OFF" -> false;
            case "WARN" -> true;
            default -> nivel == NivelExcepcion.ERROR;
        };
    }

    private static String traza(Throwable error) {
        StringWriter texto = new StringWriter();
        error.printStackTrace(new PrintWriter(texto));
        return texto.toString();
    }

    private static String recortar(String valor, int maximo) {
        if (valor == null) {
            return null;
        }
        return valor.length() <= maximo ? valor : valor.substring(0, maximo);
    }
}
