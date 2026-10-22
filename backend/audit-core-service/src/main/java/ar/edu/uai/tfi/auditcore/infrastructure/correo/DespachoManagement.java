package ar.edu.uai.tfi.auditcore.infrastructure.correo;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;

/**
 * Después de confirmar una transacción que guardó avisos, le pide a management-service que despache la bandeja de
 * salida (POST /internal/mail/dispatch con el secreto de tareas). Es asincrónico: la respuesta al usuario no espera.
 * Si el pedido falla, los avisos quedan pendientes hasta el próximo despacho.
 */
@ApplicationScoped
public class DespachoManagement {

    static final String ENCABEZADO = "X-AuditNet-Tarea";
    private static final Logger LOG = Logger.getLogger(DespachoManagement.class);

    /** Pedido de despachar la bandeja de salida. */
    public record SolicitudDespacho() {
    }

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    @ConfigProperty(name = "auditnet.management.url")
    Optional<String> managementUrl;
    @ConfigProperty(name = "auditnet.tareas.secreto")
    Optional<String> secreto;

    void alConfirmar(@Observes(during = TransactionPhase.AFTER_SUCCESS) SolicitudDespacho pedido) {
        if (managementUrl.isEmpty() || secreto.isEmpty()) {
            return;
        }
        HttpRequest solicitud = HttpRequest.newBuilder(
                        URI.create(managementUrl.get().replaceAll("/+$", "") + "/internal/mail/dispatch"))
                .timeout(Duration.ofSeconds(60))
                .header(ENCABEZADO, secreto.get().trim())
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        http.sendAsync(solicitud, HttpResponse.BodyHandlers.discarding())
                .whenComplete((respuesta, error) -> {
                    if (error != null) {
                        LOG.warnf("No se pudo pedir el despacho de correos: %s", error.getMessage());
                    } else if (respuesta.statusCode() / 100 != 2) {
                        LOG.warnf("management-service respondió HTTP %d al pedir el despacho de correos",
                                respuesta.statusCode());
                    }
                });
    }
}
