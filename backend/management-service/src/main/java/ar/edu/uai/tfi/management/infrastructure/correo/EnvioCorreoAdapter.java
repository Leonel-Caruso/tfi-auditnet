package ar.edu.uai.tfi.management.infrastructure.correo;

import ar.edu.uai.tfi.management.application.correo.FalloEnvioCorreo;
import ar.edu.uai.tfi.management.application.correo.MensajeCorreo;
import ar.edu.uai.tfi.management.application.port.EnvioCorreoPort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Envía por Azure Communication Services (API REST firmada con la clave de acceso) cuando hay cadena de conexión y
 * remitente; si no, por Mailpit (correo de prueba en local). Sin ninguno de los dos, el envío queda apagado.
 */
@ApplicationScoped
public class EnvioCorreoAdapter implements EnvioCorreoPort {

    static final String API_ACS = "/emails:send?api-version=2023-03-31";
    private static final Duration ESPERA_LIMITE = Duration.ofMinutes(60);

    private final ObjectMapper objectMapper;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    @ConfigProperty(name = "auditnet.correo.conexion")
    Optional<String> conexion;
    @ConfigProperty(name = "auditnet.correo.remitente")
    Optional<String> remitente;
    @ConfigProperty(name = "auditnet.correo.mailpit-url")
    Optional<String> mailpitUrl;

    public EnvioCorreoAdapter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String proveedor() {
        if (conexion.isPresent() && remitente.isPresent()) {
            return "Azure Communication Services";
        }
        return mailpitUrl.isPresent() ? "Mailpit" : null;
    }

    @Override
    public String enviar(MensajeCorreo mensaje) {
        if (conexion.isPresent() && remitente.isPresent()) {
            return enviarPorAcs(mensaje);
        }
        if (mailpitUrl.isPresent()) {
            return enviarPorMailpit(mensaje);
        }
        throw new FalloEnvioCorreo("El envío de correo no está configurado.", true, null);
    }

    private String enviarPorAcs(MensajeCorreo mensaje) {
        Map<String, String> partes = ConexionAcs.leer(conexion.get());
        URI uri = URI.create(partes.get("endpoint").replaceAll("/+$", "") + API_ACS);
        String operacion = UUID.randomUUID().toString();
        String cuerpo = json(Map.of(
                "senderAddress", remitente.get(),
                "content", Map.of("subject", mensaje.asunto(), "plainText", mensaje.texto(), "html", mensaje.html()),
                "recipients", Map.of("to", List.of(destinatario(mensaje, "address", "displayName"))),
                "userEngagementTrackingDisabled", true));
        HttpRequest.Builder pedido = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/json")
                .header("Operation-Id", operacion)
                .POST(HttpRequest.BodyPublishers.ofString(cuerpo));
        FirmaAcs.encabezados("POST", uri, cuerpo, partes.get("accesskey"), ZonedDateTime.now(ZoneOffset.UTC))
                .forEach(pedido::header);
        HttpResponse<String> respuesta = ejecutar(pedido.build());
        if (respuesta.statusCode() == 202) {
            return operacion;
        }
        throw fallo("Azure Communication Services", respuesta);
    }

    private String enviarPorMailpit(MensajeCorreo mensaje) {
        String cuerpo = json(Map.of(
                "From", Map.of("Email", remitente.orElse("no-responder@auditnet.local"), "Name", "AuditNet"),
                "To", List.of(destinatario(mensaje, "Email", "Name")),
                "Subject", mensaje.asunto(),
                "Text", mensaje.texto(),
                "HTML", mensaje.html()));
        HttpRequest pedido = HttpRequest.newBuilder(URI.create(mailpitUrl.get().replaceAll("/+$", "") + "/api/v1/send"))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(cuerpo))
                .build();
        HttpResponse<String> respuesta = ejecutar(pedido);
        if (respuesta.statusCode() / 100 == 2) {
            return leer(respuesta.body()).path("ID").asText("mailpit");
        }
        throw fallo("Mailpit", respuesta);
    }

    private HttpResponse<String> ejecutar(HttpRequest pedido) {
        try {
            return http.send(pedido, HttpResponse.BodyHandlers.ofString());
        } catch (IOException exception) {
            throw new FalloEnvioCorreo("Sin respuesta del proveedor de correo: " + exception.getMessage(), true, null);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new FalloEnvioCorreo("Envío interrumpido.", true, null);
        }
    }

    /** 429: límite de envíos (se espera lo que indique el proveedor); 5xx y 408 se reintentan; el resto, no. */
    private FalloEnvioCorreo fallo(String proveedor, HttpResponse<String> respuesta) {
        int codigo = respuesta.statusCode();
        String detalle = leer(respuesta.body()).path("error").path("message").asText("");
        String mensaje = proveedor + " respondió HTTP " + codigo + (detalle.isBlank() ? "" : ": " + detalle);
        if (codigo == 429) {
            Duration espera = respuesta.headers().firstValue("retry-after")
                    .map(valor -> {
                        try {
                            return Duration.ofSeconds(Math.max(60, Long.parseLong(valor.trim())));
                        } catch (NumberFormatException exception) {
                            return ESPERA_LIMITE;
                        }
                    })
                    .orElse(ESPERA_LIMITE);
            return new FalloEnvioCorreo(mensaje, true, espera);
        }
        boolean reintentable = codigo >= 500 || codigo == 408 || codigo == 401 || codigo == 403;
        return new FalloEnvioCorreo(mensaje, reintentable, null);
    }

    private static Map<String, String> destinatario(MensajeCorreo mensaje, String campoDireccion, String campoNombre) {
        return mensaje.nombreDestinatario() == null
                ? Map.of(campoDireccion, mensaje.destinatario())
                : Map.of(campoDireccion, mensaje.destinatario(), campoNombre, mensaje.nombreDestinatario());
    }

    private String json(Object valor) {
        try {
            return objectMapper.writeValueAsString(valor);
        } catch (JsonProcessingException exception) {
            throw new FalloEnvioCorreo("No se pudo armar el pedido: " + exception.getMessage(), false, null);
        }
    }

    private JsonNode leer(String cuerpo) {
        try {
            return objectMapper.readTree(cuerpo == null || cuerpo.isBlank() ? "{}" : cuerpo);
        } catch (JsonProcessingException exception) {
            return objectMapper.createObjectNode();
        }
    }
}
