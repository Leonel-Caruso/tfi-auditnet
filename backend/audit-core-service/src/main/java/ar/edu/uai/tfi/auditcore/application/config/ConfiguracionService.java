package ar.edu.uai.tfi.auditcore.application.config;

import ar.edu.uai.tfi.auditcore.application.port.TrazabilidadPort;
import ar.edu.uai.tfi.auditcore.domain.model.ConfiguracionDispositivo;
import ar.edu.uai.tfi.auditcore.domain.model.DispositivoRed;
import ar.edu.uai.tfi.auditcore.domain.model.EstadoDispositivo;
import ar.edu.uai.tfi.auditcore.domain.model.FormatoConfiguracion;
import ar.edu.uai.tfi.auditcore.domain.repository.ConfiguracionRepository;
import ar.edu.uai.tfi.auditcore.domain.repository.DispositivoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;

@ApplicationScoped
public class ConfiguracionService {

    private static final int MAX_CONTENIDO = 250_000;

    private final ConfiguracionRepository configuracionRepository;
    private final DispositivoRepository dispositivoRepository;
    private final NormalizacionConfiguracionService normalizacionService;
    private final TrazabilidadPort trazabilidad;

    public ConfiguracionService(
            ConfiguracionRepository configuracionRepository,
            DispositivoRepository dispositivoRepository,
            NormalizacionConfiguracionService normalizacionService,
            TrazabilidadPort trazabilidad
    ) {
        this.configuracionRepository = configuracionRepository;
        this.dispositivoRepository = dispositivoRepository;
        this.normalizacionService = normalizacionService;
        this.trazabilidad = trazabilidad;
    }

    @Transactional
    public ConfiguracionDispositivo importar(
            Long dispositivoId,
            String contenido,
            String formato,
            String nombreFuente,
            String actor
    ) {
        validarId(dispositivoId, "dispositivoId");

        DispositivoRed dispositivo = dispositivoRepository.buscarPorId(dispositivoId)
                .orElseThrow(() -> new NoSuchElementException("No existe el dispositivo seleccionado."));

        return importarParaDispositivo(dispositivo, contenido, formato, nombreFuente, actor);
    }

    @Transactional
    public ConfiguracionDispositivo importarParaOrganizacion(
            Long dispositivoId,
            Long organizacionId,
            String contenido,
            String formato,
            String nombreFuente,
            String actor
    ) {
        validarId(dispositivoId, "dispositivoId");
        validarId(organizacionId, "organizacionId");

        DispositivoRed dispositivo = dispositivoRepository.buscarPorIdYOrganizacion(dispositivoId, organizacionId)
                .orElseThrow(() -> new NoSuchElementException("No existe el dispositivo seleccionado."));

        return importarParaDispositivo(dispositivo, contenido, formato, nombreFuente, actor);
    }

    public List<ConfiguracionDispositivo> listarAuditables() {
        return configuracionRepository.listarAuditables();
    }

    public List<ConfiguracionDispositivo> listarAuditablesPorOrganizacion(Long organizacionId) {
        validarId(organizacionId, "organizacionId");
        return configuracionRepository.listarAuditablesPorOrganizacion(organizacionId);
    }

    public ConfiguracionDispositivo buscarPorId(Long id) {
        validarId(id, "id");
        return configuracionRepository.buscarPorId(id)
                .orElseThrow(() -> new NoSuchElementException("No existe la configuración solicitada."));
    }

    public ConfiguracionDispositivo buscarPorIdYOrganizacion(Long id, Long organizacionId) {
        validarId(id, "id");
        validarId(organizacionId, "organizacionId");
        return configuracionRepository.buscarPorIdYOrganizacion(id, organizacionId)
                .orElseThrow(() -> new NoSuchElementException("No existe la configuración solicitada."));
    }

    private ConfiguracionDispositivo importarParaDispositivo(
            DispositivoRed dispositivo,
            String contenido,
            String formato,
            String nombreFuente,
            String actor
    ) {
        if (dispositivo.estado() != EstadoDispositivo.ACTIVO) {
            throw new IllegalStateException("El dispositivo seleccionado está inactivo y no admite nuevas configuraciones.");
        }

        if (contenido == null || contenido.isBlank()) {
            throw new IllegalArgumentException("La configuración no puede estar vacía.");
        }

        if (contenido.length() > MAX_CONTENIDO) {
            throw new IllegalArgumentException("La configuración supera el tamaño máximo permitido para el MVP.");
        }

        FormatoConfiguracion formatoValido = parsearFormato(formato);
        String contenidoNormalizado = normalizacionService.normalizar(contenido);
        int version = configuracionRepository.siguienteVersionParaDispositivo(dispositivo.id());

        ConfiguracionDispositivo nueva = new ConfiguracionDispositivo(
                null,
                dispositivo.id(),
                dispositivo.organizacionId(),
                version,
                formatoValido,
                limpiarNombreFuente(nombreFuente, formatoValido, version),
                contenido,
                contenidoNormalizado,
                Instant.now(),
                actorSeguro(actor)
        );

        ConfiguracionDispositivo creada = configuracionRepository.guardar(nueva);
        trazabilidad.registrar(
                actorSeguro(actor),
                "CONFIGURACION_IMPORTADA",
                "configuracionId=" + creada.id()
                        + ", dispositivoId=" + creada.dispositivoId()
                        + ", version=" + creada.version()
        );
        return creada;
    }

    private FormatoConfiguracion parsearFormato(String valor) {
        String candidato = valor == null || valor.isBlank() ? "TEXTO" : valor.trim().toUpperCase();
        try {
            return FormatoConfiguracion.valueOf(candidato);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("El formato debe ser TEXTO, TXT o CFG.");
        }
    }

    private String limpiarNombreFuente(String valor, FormatoConfiguracion formato, int version) {
        if (valor == null || valor.isBlank()) {
            return formato == FormatoConfiguracion.TEXTO
                    ? "entrada-manual-v" + version
                    : "configuracion-v" + version + "." + formato.name().toLowerCase();
        }
        String limpio = valor.trim();
        return limpio.length() <= 180 ? limpio : limpio.substring(0, 180);
    }

    private String actorSeguro(String actor) {
        return actor == null || actor.isBlank() ? "SISTEMA_O_ANONIMO" : actor;
    }

    private void validarId(Long valor, String campo) {
        if (valor == null || valor <= 0) {
            throw new IllegalArgumentException("El campo " + campo + " debe ser un identificador válido.");
        }
    }
}
