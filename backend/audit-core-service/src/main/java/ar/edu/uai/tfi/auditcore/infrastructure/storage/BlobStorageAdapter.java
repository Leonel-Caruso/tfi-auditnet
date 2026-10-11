package ar.edu.uai.tfi.auditcore.infrastructure.storage;

import ar.edu.uai.tfi.auditcore.application.port.AlmacenArchivosPort;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Repositorio de archivos sobre Azure Blob Storage con la API REST (Bloque 7, D-35). El contenedor es privado: solo
 * accede el servicio con la clave de la cuenta, guardada como secreto de Container Apps. En local se usa Azurite
 * (emulador oficial) con su cuenta pública de desarrollo.
 *
 * Sin configuración (pruebas automáticas, o antes de crear la cuenta en Azure) el adaptador queda "no configurado":
 * las operaciones de negocio siguen funcionando y los archivos quedan pendientes de subir desde el panel.
 */
@ApplicationScoped
public class BlobStorageAdapter implements AlmacenArchivosPort {

    private static final Logger LOG = Logger.getLogger(BlobStorageAdapter.class);
    static final String VERSION_API = "2021-08-06";
    // Día siempre con dos dígitos ("Tue, 03 Nov 2026 09:05:00 GMT"), como envían los SDK de Azure.
    private static final DateTimeFormatter FECHA_HTTP = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US);
    private static final Pattern RUTA_VALIDA = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9._/-]{0,399}$");
    private static final Pattern CONTENEDOR_VALIDO = Pattern.compile("^[a-z0-9](?:[a-z0-9-]{1,61}[a-z0-9])$");

    private final ConexionBlob conexion;
    private final String contenedor;
    private final HttpClient http;
    private volatile boolean contenedorVerificado;

    public BlobStorageAdapter(
            @ConfigProperty(name = "auditnet.storage.connection-string") Optional<String> cadenaConexion,
            @ConfigProperty(name = "auditnet.storage.emulador-local", defaultValue = "false") boolean emuladorLocal,
            @ConfigProperty(name = "auditnet.storage.contenedor", defaultValue = "auditnet-archivos") String contenedor) {
        String cadena = cadenaConexion.filter(valor -> !valor.isBlank())
                .orElse(emuladorLocal ? ConexionBlob.AZURITE : null);
        ConexionBlob leida = null;
        if (cadena != null) {
            try {
                leida = ConexionBlob.desde(cadena);
            } catch (IllegalArgumentException exception) {
                // No se corta el arranque del servicio: el repositorio queda "no configurado" y la salud lo informa.
                LOG.errorf("Repositorio de archivos: la cadena de conexión no es válida (%s).", exception.getMessage());
            }
        }
        this.conexion = leida;
        if (!CONTENEDOR_VALIDO.matcher(contenedor).matches()) {
            throw new IllegalArgumentException("Nombre de contenedor inválido: " + contenedor);
        }
        this.contenedor = contenedor;
        this.http = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).connectTimeout(Duration.ofSeconds(5)).build();
        if (conexion == null) {
            LOG.warn("Repositorio de archivos sin configurar (auditnet.storage.connection-string): los archivos quedan pendientes.");
        } else {
            LOG.infof("Repositorio de archivos: cuenta %s, contenedor %s", conexion.cuenta(), contenedor);
        }
    }

    @Override
    public boolean configurado() {
        return conexion != null;
    }

    @Override
    public String contenedor() {
        return contenedor;
    }

    @Override
    public void subir(String ruta, byte[] contenido, String tipoContenido, Map<String, String> metadatos) {
        exigirConfiguracion();
        validarRuta(ruta);
        asegurarContenedor();
        Map<String, String> encabezados = new LinkedHashMap<>();
        encabezados.put("Content-Type", tipoContenido);
        encabezados.put("Content-Length", String.valueOf(contenido.length));
        // If-None-Match: * hace que Azure rechace la subida si el archivo ya existe: nunca se reemplaza evidencia.
        encabezados.put("If-None-Match", "*");
        encabezados.put("x-ms-blob-type", "BlockBlob");
        metadatos.forEach((clave, valor) -> encabezados.put("x-ms-meta-" + clave, ascii(valor)));
        HttpResponse<byte[]> respuesta = enviar("PUT", conexion.uri(contenedor, ruta, null), encabezados,
                HttpRequest.BodyPublishers.ofByteArray(contenido), Duration.ofMinutes(5));
        if (respuesta.statusCode() == 404) {
            // El contenedor se borró después de verificarlo: se vuelve a crear en la próxima subida.
            contenedorVerificado = false;
        }
        if (respuesta.statusCode() != 201) {
            throw error("No se pudo guardar el archivo en el repositorio", respuesta);
        }
    }

    @Override
    public byte[] descargar(String ruta) {
        exigirConfiguracion();
        validarRuta(ruta);
        HttpResponse<byte[]> respuesta = enviar("GET", conexion.uri(contenedor, ruta, null), new LinkedHashMap<>(),
                HttpRequest.BodyPublishers.noBody(), Duration.ofMinutes(5));
        if (respuesta.statusCode() != 200) {
            throw error("No se pudo leer el archivo del repositorio", respuesta);
        }
        return respuesta.body();
    }

    @Override
    public void verificar() {
        exigirConfiguracion();
        HttpResponse<byte[]> respuesta = enviar("GET", conexion.uri(contenedor, null, "restype=container"),
                new LinkedHashMap<>(), HttpRequest.BodyPublishers.noBody(), Duration.ofSeconds(10));
        if (respuesta.statusCode() == 404) {
            throw new AlmacenamientoException("El contenedor " + contenedor + " todavía no existe (se crea con el primer archivo).",
                    404, null);
        }
        if (respuesta.statusCode() != 200) {
            throw error("El repositorio de archivos no responde", respuesta);
        }
        contenedorVerificado = true;
    }

    /** Crea el contenedor privado la primera vez (si ya existe, Azure responde 409 y se sigue). */
    private void asegurarContenedor() {
        if (contenedorVerificado) {
            return;
        }
        HttpResponse<byte[]> respuesta = enviar("PUT", conexion.uri(contenedor, null, "restype=container"),
                new LinkedHashMap<>(Map.of("Content-Length", "0")), HttpRequest.BodyPublishers.noBody(), Duration.ofSeconds(15));
        if (respuesta.statusCode() != 201 && respuesta.statusCode() != 409) {
            throw error("No se pudo crear el contenedor del repositorio", respuesta);
        }
        contenedorVerificado = true;
    }

    private void exigirConfiguracion() {
        if (conexion == null) {
            throw new AlmacenamientoException("El repositorio de archivos no está configurado.", 503, null);
        }
    }

    private HttpResponse<byte[]> enviar(String verbo, URI uri, Map<String, String> encabezados,
                                        HttpRequest.BodyPublisher cuerpo, Duration limite) {
        encabezados.put("x-ms-date", FECHA_HTTP.format(ZonedDateTime.now(ZoneOffset.UTC)));
        encabezados.put("x-ms-version", VERSION_API);
        String autorizacion = FirmaSharedKey.autorizacion(conexion.cuenta(), conexion.clave(), verbo, uri, encabezados);

        HttpRequest.Builder solicitud = HttpRequest.newBuilder(uri).timeout(limite).method(verbo, cuerpo);
        // Content-Length lo pone el cliente HTTP (es un encabezado restringido): se usa solo para la firma.
        encabezados.forEach((nombre, valor) -> {
            if (!"Content-Length".equalsIgnoreCase(nombre)) {
                solicitud.header(nombre, valor);
            }
        });
        solicitud.header("Authorization", autorizacion);
        try {
            return http.send(solicitud.build(), HttpResponse.BodyHandlers.ofByteArray());
        } catch (IOException exception) {
            throw new AlmacenamientoException("El repositorio de archivos no responde (" + exception.getClass().getSimpleName() + ").",
                    503, exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AlmacenamientoException("Se interrumpió la operación con el repositorio de archivos.", 503, exception);
        }
    }

    private static AlmacenamientoException error(String mensaje, HttpResponse<byte[]> respuesta) {
        String codigo = respuesta.headers().firstValue("x-ms-error-code").orElse("HTTP " + respuesta.statusCode());
        LOG.warnf("%s: %s", mensaje, codigo);
        return new AlmacenamientoException(mensaje + " (" + codigo + ").", respuesta.statusCode(), null);
    }

    private static void validarRuta(String ruta) {
        if (ruta == null || !RUTA_VALIDA.matcher(ruta).matches() || ruta.contains("..") || ruta.contains("//")) {
            throw new IllegalArgumentException("Ruta de archivo inválida.");
        }
    }

    /** Los metadatos de Azure solo admiten ASCII. */
    private static String ascii(String valor) {
        return valor == null ? "" : valor.replaceAll("[^\\x20-\\x7E]", "_");
    }
}
