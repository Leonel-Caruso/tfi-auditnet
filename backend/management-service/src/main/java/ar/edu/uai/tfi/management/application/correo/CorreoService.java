package ar.edu.uai.tfi.management.application.correo;

import ar.edu.uai.tfi.management.application.port.AvisoDespachoPort;
import ar.edu.uai.tfi.management.application.port.EnvioCorreoPort;
import ar.edu.uai.tfi.management.domain.model.CorreoSaliente;
import ar.edu.uai.tfi.management.domain.model.EstadoCorreo;
import ar.edu.uai.tfi.management.domain.model.OrganizacionCliente;
import ar.edu.uai.tfi.management.domain.model.TipoCorreo;
import ar.edu.uai.tfi.management.domain.repository.CorreoRepository;
import ar.edu.uai.tfi.management.domain.repository.OrganizacionClienteRepository;
import io.quarkus.narayana.jta.runtime.TransactionConfiguration;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Bandeja de salida: los avisos se guardan con el evento y se envían después, de a pocos (el dominio de Azure permite
 * 5 por minuto y 10 por hora). Un envío fallido se reintenta con esperas crecientes; después de
 * {@value #MAXIMO_INTENTOS} intentos, o si el proveedor lo rechaza por inválido, queda FALLIDO.
 */
@ApplicationScoped
public class CorreoService implements BandejaCorreo {

    static final String ORIGEN = "management-service";
    static final int MAXIMO_INTENTOS = 5;
    static final int POR_DESPACHO = 5;
    private static final List<Duration> ESPERAS = List.of(Duration.ofMinutes(5), Duration.ofMinutes(30),
            Duration.ofHours(2), Duration.ofHours(6));

    private final CorreoRepository repository;
    private final EnvioCorreoPort envio;
    private final OrganizacionClienteRepository organizaciones;
    private final AvisoDespachoPort despacho;
    private final PlantillasCorreo plantillas = new PlantillasCorreo();

    @ConfigProperty(name = "auditnet.url-publica", defaultValue = "http://localhost:4321")
    String urlPublica;
    Clock reloj = Clock.systemUTC();

    public CorreoService(CorreoRepository repository, EnvioCorreoPort envio, OrganizacionClienteRepository organizaciones,
                         AvisoDespachoPort despacho) {
        this.repository = repository;
        this.envio = envio;
        this.organizaciones = organizaciones;
        this.despacho = despacho;
    }

    /** Resultado de un despacho; {@code pendientes} son los que quedan para el próximo. */
    public record ResultadoDespacho(String proveedor, int enviados, int reintentos, int fallidos, long pendientes) {
    }

    /** Estado para el panel de administración. */
    public record EstadoBandeja(String proveedor, long pendientes, long enviados24h, long fallidos24h,
                                List<CorreoSaliente> ultimos) {
    }

    @Override
    public void encolar(TipoCorreo tipo, Long organizacionId, String destinatario, String nombre,
                        Map<String, Object> datos) {
        if (destinatario == null || destinatario.isBlank()) {
            return;
        }
        Instant ahora = reloj.instant();
        repository.guardar(new CorreoSaliente(null, ORIGEN, tipo, organizacionId, destinatario.trim(), nombre,
                datos, EstadoCorreo.PENDIENTE, 0, ahora, ahora, null, null, null));
        despacho.solicitarDespacho();
    }

    /**
     * Envía los pendientes cuyo turno llegó. Sin proveedor configurado no hace nada: quedan para más adelante.
     * La transacción admite varios envíos lentos seguidos sin vencer (por defecto vence al minuto).
     */
    @Transactional
    @TransactionConfiguration(timeout = 300)
    public ResultadoDespacho despachar() {
        String proveedor = envio.proveedor();
        Instant ahora = reloj.instant();
        if (proveedor == null) {
            return new ResultadoDespacho(null, 0, 0, 0, repository.resumen(ahora).pendientes());
        }
        int enviados = 0;
        int reintentos = 0;
        int fallidos = 0;
        for (CorreoSaliente correo : repository.tomarPendientes(ahora, POR_DESPACHO)) {
            if (enlaceVencido(correo, ahora)) {
                // Un enlace de recuperación que venció antes de salir no sirve: se pide otro desde el login.
                repository.actualizar(correo.fallido("El enlace venció antes de poder enviarlo."));
                fallidos++;
                continue;
            }
            try {
                MensajeCorreo mensaje = plantillas.armar(correo, nombreOrganizacion(correo.organizacionId()),
                        urlPublica);
                repository.actualizar(correo.enviado(reloj.instant(), envio.enviar(mensaje)));
                enviados++;
            } catch (FalloEnvioCorreo fallo) {
                if (!fallo.reintentable() || correo.intentos() + 1 >= MAXIMO_INTENTOS) {
                    repository.actualizar(correo.fallido(fallo.getMessage()));
                    fallidos++;
                } else {
                    Duration espera = fallo.esperar() != null ? fallo.esperar() : ESPERAS.get(correo.intentos());
                    repository.actualizar(correo.reintentar(reloj.instant().plus(espera), fallo.getMessage()));
                    reintentos++;
                    if (fallo.esperar() != null) {
                        // Límite de envíos del proveedor: el resto espera al próximo despacho.
                        break;
                    }
                }
            } catch (RuntimeException error) {
                repository.actualizar(correo.fallido("No se pudo armar el correo: " + error.getMessage()));
                fallidos++;
            }
        }
        return new ResultadoDespacho(proveedor, enviados, reintentos, fallidos,
                repository.resumen(reloj.instant()).pendientes());
    }

    public EstadoBandeja estado() {
        CorreoRepository.Resumen resumen = repository.resumen(reloj.instant().minus(Duration.ofHours(24)));
        return new EstadoBandeja(envio.proveedor(), resumen.pendientes(), resumen.enviados(), resumen.fallidos(),
                repository.ultimos(20));
    }

    private static boolean enlaceVencido(CorreoSaliente correo, Instant ahora) {
        return correo.tipo() == TipoCorreo.RECUPERACION_CONTRASENA && correo.datos() != null
                && correo.datos().get("minutos") instanceof Number minutos
                && correo.creadoEn().plus(Duration.ofMinutes(minutos.longValue())).isBefore(ahora);
    }

    private String nombreOrganizacion(Long organizacionId) {
        return organizacionId == null ? null : organizaciones.buscarPorId(organizacionId)
                .map(OrganizacionCliente::razonSocial)
                .orElse(null);
    }
}
