package ar.edu.uai.tfi.auditcore.application.report;

import ar.edu.uai.tfi.auditcore.application.storage.RepositorioArchivosService;
import ar.edu.uai.tfi.auditcore.application.port.TrazabilidadPort;
import ar.edu.uai.tfi.auditcore.domain.model.AuditoriaConfiguracion;
import ar.edu.uai.tfi.auditcore.domain.model.BaselineConfiguracion;
import ar.edu.uai.tfi.auditcore.domain.model.ConfiguracionDispositivo;
import ar.edu.uai.tfi.auditcore.domain.model.ContenidoReporte;
import ar.edu.uai.tfi.auditcore.domain.model.ContenidoReporte.AuditoriaIncluida;
import ar.edu.uai.tfi.auditcore.domain.model.ContenidoReporte.HallazgoIncluido;
import ar.edu.uai.tfi.auditcore.domain.model.CriteriosReporte;
import ar.edu.uai.tfi.auditcore.domain.model.DispositivoRed;
import ar.edu.uai.tfi.auditcore.domain.model.EstadoHallazgo;
import ar.edu.uai.tfi.auditcore.domain.model.FiltroAuditorias;
import ar.edu.uai.tfi.auditcore.domain.model.HallazgoAuditoria;
import ar.edu.uai.tfi.auditcore.domain.model.OperacionTransaccion;
import ar.edu.uai.tfi.auditcore.domain.model.ReporteAuditoria;
import ar.edu.uai.tfi.auditcore.domain.model.SeveridadRegla;
import ar.edu.uai.tfi.auditcore.domain.model.Transaccion;
import ar.edu.uai.tfi.auditcore.domain.repository.AuditoriaRepository;
import ar.edu.uai.tfi.auditcore.domain.repository.BaselineRepository;
import ar.edu.uai.tfi.auditcore.domain.repository.ConfiguracionRepository;
import ar.edu.uai.tfi.auditcore.domain.repository.DispositivoRepository;
import ar.edu.uai.tfi.auditcore.domain.repository.HallazgoRepository;
import ar.edu.uai.tfi.auditcore.domain.repository.ReporteRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Reportes de auditoría. Se guardan como foto fija (contenido, criterios, fecha y usuario) y generarlos no
 * modifica ningún dato de origen.
 */
@ApplicationScoped
public class ReporteService {

    static final String ENTIDAD = "REPORTE";
    static final ZoneId ZONA = ZoneId.of("America/Argentina/Buenos_Aires");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final int MAXIMO_AUDITORIAS = 500;

    private final AuditoriaRepository auditoriaRepository;
    private final HallazgoRepository hallazgoRepository;
    private final DispositivoRepository dispositivoRepository;
    private final BaselineRepository baselineRepository;
    private final ConfiguracionRepository configuracionRepository;
    private final ReporteRepository reporteRepository;
    private final TrazabilidadPort trazabilidad;
    private final RepositorioArchivosService repositorioArchivos;

    public ReporteService(AuditoriaRepository auditoriaRepository,
                          HallazgoRepository hallazgoRepository,
                          DispositivoRepository dispositivoRepository,
                          BaselineRepository baselineRepository,
                          ConfiguracionRepository configuracionRepository,
                          ReporteRepository reporteRepository,
                          TrazabilidadPort trazabilidad,
                          RepositorioArchivosService repositorioArchivos) {
        this.auditoriaRepository = auditoriaRepository;
        this.hallazgoRepository = hallazgoRepository;
        this.dispositivoRepository = dispositivoRepository;
        this.baselineRepository = baselineRepository;
        this.configuracionRepository = configuracionRepository;
        this.reporteRepository = reporteRepository;
        this.trazabilidad = trazabilidad;
        this.repositorioArchivos = repositorioArchivos;
    }

    /** Datos del reporte que van a la bitácora (sin el contenido completo). */
    public record RegistroReporte(Long id, String titulo, Long organizacionId, CriteriosReporte criterios,
                           int totalAuditorias, int totalHallazgos) {
    }

    /**
     * @param criterios la organización ya viene resuelta según el usuario (null = todas, solo administrador)
     * @return vacío si los criterios no devuelven auditorías; en ese caso no se guarda nada
     */
    @Transactional
    public Optional<ReporteAuditoria> generar(CriteriosReporte criterios, String actor, Instant ahora) {
        validar(criterios);
        List<AuditoriaConfiguracion> auditorias = seleccionarAuditorias(criterios);
        if (auditorias.isEmpty()) {
            return Optional.empty();
        }
        if (auditorias.size() > MAXIMO_AUDITORIAS) {
            throw new IllegalArgumentException("Los criterios incluyen " + auditorias.size()
                    + " auditorías; el máximo por reporte es " + MAXIMO_AUDITORIAS + ". Acotá el rango de fechas.");
        }

        ContenidoReporte contenido = armarContenido(auditorias, criterios);
        String usuario = recortar(actor == null || actor.isBlank() ? "SISTEMA_O_ANONIMO" : actor, 120);
        ReporteAuditoria reporte = reporteRepository.guardar(new ReporteAuditoria(
                null,
                criterios.organizacionId(),
                titulo(criterios, auditorias),
                criterios,
                contenido,
                contenido.auditorias().size(),
                contenido.hallazgos().size(),
                usuario,
                ahora
        ), auditorias.stream().map(AuditoriaConfiguracion::id).toList());

        trazabilidad.registrar(new Transaccion(ENTIDAD, reporte.id(), OperacionTransaccion.ALTA,
                reporte.organizacionId(), null,
                new RegistroReporte(reporte.id(), reporte.titulo(), reporte.organizacionId(), criterios,
                        reporte.totalAuditorias(), reporte.totalHallazgos()),
                usuario,
                recortar(reporte.titulo() + ": " + reporte.totalAuditorias() + " auditorías, "
                        + reporte.totalHallazgos() + " hallazgos", 1000)));
        repositorioArchivos.guardarReporte(reporte, usuario);
        return Optional.of(reporte);
    }

    /** Reportes disponibles, sin contenido. {@code organizacionId} null = todos (administrador). */
    public List<ReporteAuditoria> listar(Long organizacionId) {
        return reporteRepository.listar(organizacionId);
    }

    /**
     * Reporte completo. Un usuario que no es administrador solo ve los de su organización (404 para otros).
     *
     * @param organizacionId null para el administrador
     */
    public ReporteAuditoria buscar(Long id, Long organizacionId) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("El campo id debe ser un identificador válido.");
        }
        return reporteRepository.buscarPorId(id)
                .filter(reporte -> organizacionId == null || organizacionId.equals(reporte.organizacionId()))
                .orElseThrow(() -> new NoSuchElementException("No existe el reporte solicitado."));
    }

    private static void validar(CriteriosReporte criterios) {
        if (criterios == null) {
            throw new IllegalArgumentException("Los criterios del reporte son obligatorios.");
        }
        if (criterios.auditoriaIds().isEmpty() && (criterios.desde() == null || criterios.hasta() == null)) {
            throw new IllegalArgumentException("Indicá el rango de fechas (desde y hasta) o auditorías específicas.");
        }
        if (criterios.desde() != null && criterios.hasta() != null && criterios.desde().isAfter(criterios.hasta())) {
            throw new IllegalArgumentException("La fecha 'desde' no puede ser posterior a 'hasta'.");
        }
        if (criterios.auditoriaIds().stream().anyMatch(id -> id == null || id <= 0)
                || criterios.dispositivoIds().stream().anyMatch(id -> id == null || id <= 0)) {
            throw new IllegalArgumentException("Los identificadores de auditorías y dispositivos deben ser válidos.");
        }
    }

    private List<AuditoriaConfiguracion> seleccionarAuditorias(CriteriosReporte criterios) {
        Long organizacion = criterios.organizacionId();
        List<AuditoriaConfiguracion> candidatas;
        if (!criterios.auditoriaIds().isEmpty()) {
            candidatas = new ArrayList<>();
            for (Long id : new LinkedHashSet<>(criterios.auditoriaIds())) {
                AuditoriaConfiguracion auditoria = (organizacion == null
                        ? auditoriaRepository.buscarPorId(id)
                        : auditoriaRepository.buscarPorIdYOrganizacion(id, organizacion))
                        .orElseThrow(() -> new IllegalArgumentException(
                                "No existe la auditoría " + id + " o no pertenece a la organización."));
                candidatas.add(auditoria);
            }
        } else {
            candidatas = auditoriaRepository.buscarHistorial(new FiltroAuditorias(organizacion, null, null, null,
                    null, null, criterios.desde(), criterios.hasta()));
        }
        Set<Long> dispositivos = Set.copyOf(criterios.dispositivoIds());
        return candidatas.stream()
                .filter(a -> dispositivos.isEmpty() || dispositivos.contains(a.dispositivoId()))
                .filter(a -> criterios.desde() == null || !a.fechaEjecucion().isBefore(criterios.desde()))
                .filter(a -> criterios.hasta() == null || !a.fechaEjecucion().isAfter(criterios.hasta()))
                .sorted(Comparator.comparing(AuditoriaConfiguracion::fechaEjecucion).reversed()
                        .thenComparing(AuditoriaConfiguracion::id, Comparator.reverseOrder()))
                .toList();
    }

    private ContenidoReporte armarContenido(List<AuditoriaConfiguracion> auditorias, CriteriosReporte criterios) {
        Map<Long, Optional<DispositivoRed>> dispositivos = new HashMap<>();
        Map<Long, Optional<BaselineConfiguracion>> baselines = new HashMap<>();
        List<AuditoriaIncluida> incluidas = new ArrayList<>();
        List<HallazgoIncluido> hallazgos = new ArrayList<>();
        List<String> inconsistencias = new ArrayList<>();
        Set<SeveridadRegla> severidades = Set.copyOf(criterios.severidades());
        Set<EstadoHallazgo> estados = Set.copyOf(criterios.estados());

        for (AuditoriaConfiguracion auditoria : auditorias) {
            DispositivoRed dispositivo = dispositivos
                    .computeIfAbsent(auditoria.dispositivoId(), dispositivoRepository::buscarPorId).orElse(null);
            BaselineConfiguracion baseline = baselines
                    .computeIfAbsent(auditoria.baselineId(), baselineRepository::buscarPorId).orElse(null);
            ConfiguracionDispositivo configuracion = configuracionRepository
                    .buscarPorId(auditoria.configuracionId()).orElse(null);
            String codigo = String.format("AUD-%03d", auditoria.id());
            // Si falta algún dato de origen se informa la inconsistencia y se incluye lo disponible.
            if (dispositivo == null) {
                inconsistencias.add(codigo + ": no se encontró el dispositivo " + auditoria.dispositivoId() + ".");
            }
            if (baseline == null) {
                inconsistencias.add(codigo + ": no se encontró la baseline " + auditoria.baselineId() + ".");
            }
            if (configuracion == null) {
                inconsistencias.add(codigo + ": no se encontró la configuración " + auditoria.configuracionId() + ".");
            }

            incluidas.add(new AuditoriaIncluida(
                    auditoria.id(),
                    auditoria.fechaEjecucion(),
                    auditoria.ejecutadoPor(),
                    auditoria.dispositivoId(),
                    dispositivo == null ? null : dispositivo.identificador(),
                    dispositivo == null ? null : dispositivo.nombre(),
                    dispositivo == null || dispositivo.criticidad() == null ? null : dispositivo.criticidad().name(),
                    dispositivo == null ? null : dispositivo.sedeId(),
                    auditoria.configuracionId(),
                    configuracion == null ? null : configuracion.version(),
                    auditoria.baselineId(),
                    baseline == null ? null : baseline.nombre(),
                    baseline == null ? null : baseline.version(),
                    valor(auditoria.totalReglas()),
                    valor(auditoria.reglasCumplidas()),
                    valor(auditoria.totalHallazgos()),
                    auditoria.severidadMaxima(),
                    auditoria.resultado()));

            for (HallazgoAuditoria hallazgo : hallazgoRepository.listarPorAuditoria(auditoria.id())) {
                if ((!severidades.isEmpty() && !severidades.contains(hallazgo.severidad()))
                        || (!estados.isEmpty() && !estados.contains(hallazgo.estado()))) {
                    continue;
                }
                hallazgos.add(new HallazgoIncluido(
                        hallazgo.id(),
                        hallazgo.auditoriaId(),
                        hallazgo.dispositivoId(),
                        dispositivo == null ? null : dispositivo.identificador(),
                        hallazgo.reglaId(),
                        hallazgo.codigoRegla(),
                        hallazgo.nombreRegla(),
                        hallazgo.severidad().name(),
                        hallazgo.impacto(),
                        hallazgo.evidencia(),
                        hallazgo.recomendacion(),
                        hallazgo.estado().name(),
                        hallazgo.fechaDeteccion()));
            }
        }
        hallazgos.sort(Comparator
                .comparing((HallazgoIncluido h) -> -SeveridadRegla.valueOf(h.severidad()).ordinal())
                .thenComparing(HallazgoIncluido::auditoriaId, Comparator.reverseOrder())
                .thenComparing(HallazgoIncluido::id));

        int reglas = incluidas.stream().mapToInt(AuditoriaIncluida::totalReglas).sum();
        int cumplidas = incluidas.stream().mapToInt(AuditoriaIncluida::reglasCumplidas).sum();
        Map<String, Long> porSeveridad = new LinkedHashMap<>();
        for (int i = SeveridadRegla.values().length - 1; i >= 0; i--) {
            String severidad = SeveridadRegla.values()[i].name();
            porSeveridad.put(severidad, hallazgos.stream().filter(h -> h.severidad().equals(severidad)).count());
        }
        Map<String, Long> porEstado = new LinkedHashMap<>();
        for (EstadoHallazgo estado : EstadoHallazgo.values()) {
            porEstado.put(estado.name(), hallazgos.stream().filter(h -> h.estado().equals(estado.name())).count());
        }
        String severidadMaxima = porSeveridad.entrySet().stream()
                .filter(e -> e.getValue() > 0).map(Map.Entry::getKey).findFirst().orElse("NINGUNA");

        ContenidoReporte.Resumen resumen = new ContenidoReporte.Resumen(
                incluidas.size(),
                (int) incluidas.stream().map(AuditoriaIncluida::dispositivoId).distinct().count(),
                reglas,
                cumplidas,
                reglas == 0 ? null : (int) Math.round(cumplidas * 100.0 / reglas),
                hallazgos.size(),
                (int) hallazgos.stream().filter(h -> EstadoHallazgo.valueOf(h.estado()).pendiente()).count(),
                porSeveridad,
                porEstado,
                severidadMaxima,
                hallazgos.isEmpty() ? "SIN_DESVIOS" : "CON_DESVIOS");
        return new ContenidoReporte(resumen, incluidas, hallazgos, inconsistencias);
    }

    private static String titulo(CriteriosReporte criterios, List<AuditoriaConfiguracion> auditorias) {
        if (!criterios.auditoriaIds().isEmpty()) {
            String lista = auditorias.stream().map(a -> String.format("AUD-%03d", a.id()))
                    .collect(Collectors.joining(", "));
            return recortar("Reporte de auditoría " + lista, 160);
        }
        return "Reporte de auditorías del " + FECHA.format(LocalDate.ofInstant(criterios.desde(), ZONA))
                + " al " + FECHA.format(LocalDate.ofInstant(criterios.hasta(), ZONA));
    }

    private static int valor(Integer numero) {
        return numero == null ? 0 : numero;
    }

    private static String recortar(String valor, int maximo) {
        return valor.length() <= maximo ? valor : valor.substring(0, maximo);
    }
}
