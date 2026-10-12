package ar.edu.uai.tfi.auditcore.application.dashboard;

import ar.edu.uai.tfi.auditcore.domain.model.AlertaCritica;
import ar.edu.uai.tfi.auditcore.domain.model.AuditoriaConfiguracion;
import ar.edu.uai.tfi.auditcore.domain.model.CriticidadDispositivo;
import ar.edu.uai.tfi.auditcore.domain.model.DispositivoRed;
import ar.edu.uai.tfi.auditcore.domain.model.EstadoAlerta;
import ar.edu.uai.tfi.auditcore.domain.model.EstadoDispositivo;
import ar.edu.uai.tfi.auditcore.domain.model.EstadoHallazgo;
import ar.edu.uai.tfi.auditcore.domain.model.FiltroAlertas;
import ar.edu.uai.tfi.auditcore.domain.model.FiltroAuditorias;
import ar.edu.uai.tfi.auditcore.domain.model.FiltroHallazgos;
import ar.edu.uai.tfi.auditcore.domain.model.HallazgoAuditoria;
import ar.edu.uai.tfi.auditcore.domain.model.NivelRiesgo;
import ar.edu.uai.tfi.auditcore.domain.model.ReporteAuditoria;
import ar.edu.uai.tfi.auditcore.domain.model.SeveridadRegla;
import ar.edu.uai.tfi.auditcore.domain.repository.AlertaRepository;
import ar.edu.uai.tfi.auditcore.domain.repository.AuditoriaRepository;
import ar.edu.uai.tfi.auditcore.domain.repository.DispositivoRepository;
import ar.edu.uai.tfi.auditcore.domain.repository.HallazgoRepository;
import ar.edu.uai.tfi.auditcore.domain.repository.OrganizacionRepository;
import ar.edu.uai.tfi.auditcore.domain.repository.ReporteRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Tablero de control y estado general del cliente. Solo lectura. Los cálculos se hacen en memoria sobre los datos
 * del alcance, lo que alcanza para el volumen del prototipo.
 */
@ApplicationScoped
public class TableroService {

    /** Las semanas de la evolución se cuentan de lunes a domingo, en hora de Argentina. */
    static final ZoneId ZONA = ZoneId.of("America/Argentina/Buenos_Aires");
    static final int SEMANAS_EVOLUCION = 8;
    static final int LIMITE_RECIENTES = 5;

    private final DispositivoRepository dispositivoRepository;
    private final AuditoriaRepository auditoriaRepository;
    private final HallazgoRepository hallazgoRepository;
    private final AlertaRepository alertaRepository;
    private final OrganizacionRepository organizacionRepository;
    private final ReporteRepository reporteRepository;

    public TableroService(DispositivoRepository dispositivoRepository,
                          AuditoriaRepository auditoriaRepository,
                          HallazgoRepository hallazgoRepository,
                          AlertaRepository alertaRepository,
                          OrganizacionRepository organizacionRepository,
                          ReporteRepository reporteRepository) {
        this.dispositivoRepository = dispositivoRepository;
        this.auditoriaRepository = auditoriaRepository;
        this.hallazgoRepository = hallazgoRepository;
        this.alertaRepository = alertaRepository;
        this.organizacionRepository = organizacionRepository;
        this.reporteRepository = reporteRepository;
    }

    public record IndicadoresDispositivos(long registrados, long activos, long auditados, long activosSinAuditar,
                                          Map<CriticidadDispositivo, Long> porCriticidad) {
    }

    public record IndicadoresHallazgos(long pendientes, long enRevision, long cerrados,
                                       Map<SeveridadRegla, Long> pendientesPorSeveridad,
                                       Map<SeveridadRegla, Long> cerradosPorSeveridad) {
    }

    public record IndicadoresAlertas(long vigentes, long activas, long enAtencion, long criticasVigentes) {
    }

    /**
     * @param cumplimientoActual porcentaje de reglas cumplidas en la última auditoría de cada dispositivo
     *                           (null si no hay auditorías)
     */
    public record IndicadoresAuditorias(long total, long ultimos30Dias, Integer cumplimientoActual) {
    }

    public record RiesgoDispositivo(Long dispositivoId, String identificador, String nombre, Long sedeId,
                                    CriticidadDispositivo criticidad, NivelRiesgo nivel, long pendientes,
                                    long criticosPendientes, long altosPendientes, long alertasVigentes,
                                    Instant ultimaAuditoria) {
    }

    /** Una semana de la evolución: auditorías ejecutadas, hallazgos detectados y hallazgos cerrados. */
    public record PuntoEvolucion(LocalDate semana, long auditorias, long hallazgosDetectados,
                                 long hallazgosCerrados) {
    }

    public record ResumenTablero(Instant generado, NivelRiesgo nivelRiesgo,
                                 IndicadoresDispositivos dispositivos, IndicadoresHallazgos hallazgos,
                                 IndicadoresAlertas alertas, IndicadoresAuditorias auditorias,
                                 List<AuditoriaConfiguracion> auditoriasRecientes,
                                 List<RiesgoDispositivo> dispositivosEnRiesgo,
                                 List<PuntoEvolucion> evolucion) {
    }

    /** sedeId null agrupa los dispositivos sin sede asignada. */
    public record RiesgoSede(Long sedeId, NivelRiesgo nivel, long dispositivos, long auditados, long pendientes,
                             long criticosPendientes, long alertasVigentes) {
    }

    /** @param reportes últimos reportes disponibles de la organización (sin contenido) */
    public record EstadoCliente(Long organizacionId, ResumenTablero resumen, List<RiesgoSede> sedes,
                                List<RiesgoDispositivo> dispositivos, List<ReporteAuditoria> reportes) {
    }

    /** @param organizacionId null para el administrador sin filtro (todas las organizaciones) */
    public ResumenTablero resumen(Long organizacionId, Instant ahora) {
        return armarResumen(cargar(organizacionId), ahora);
    }

    public EstadoCliente estadoCliente(Long organizacionId, Instant ahora) {
        if (organizacionId == null || organizacionId <= 0) {
            throw new IllegalArgumentException("El campo organizationId debe ser un identificador válido.");
        }
        if (!organizacionRepository.existe(organizacionId)) {
            throw new NoSuchElementException("No existe la organización cliente solicitada.");
        }
        Datos datos = cargar(organizacionId);
        return new EstadoCliente(organizacionId, armarResumen(datos, ahora), riesgoPorSede(datos),
                riesgoPorDispositivo(datos).stream().sorted(MAYOR_RIESGO).toList(),
                reporteRepository.listar(organizacionId).stream().limit(LIMITE_RECIENTES).toList());
    }

    private record Datos(List<DispositivoRed> dispositivos, List<AuditoriaConfiguracion> auditorias,
                         List<HallazgoAuditoria> hallazgos, List<AlertaCritica> alertas) {

        Map<Long, DispositivoRed> dispositivosPorId() {
            return dispositivos.stream()
                    .collect(Collectors.toMap(DispositivoRed::id, Function.identity(), (a, b) -> a));
        }

        Set<Long> dispositivosAuditados() {
            return auditorias.stream().map(AuditoriaConfiguracion::dispositivoId).collect(Collectors.toSet());
        }
    }

    private Datos cargar(Long organizacionId) {
        return new Datos(
                organizacionId == null
                        ? dispositivoRepository.listar()
                        : dispositivoRepository.listarPorOrganizacion(organizacionId),
                auditoriaRepository.buscarHistorial(
                        new FiltroAuditorias(organizacionId, null, null, null, null, null, null, null)),
                hallazgoRepository.buscar(FiltroHallazgos.todos(organizacionId)),
                alertaRepository.buscar(FiltroAlertas.todas(organizacionId))
        );
    }

    private ResumenTablero armarResumen(Datos datos, Instant ahora) {
        Set<Long> auditados = datos.dispositivosAuditados();
        List<HallazgoAuditoria> pendientes = datos.hallazgos().stream().filter(h -> h.estado().pendiente()).toList();
        List<AlertaCritica> vigentes = datos.alertas().stream().filter(a -> a.estado().vigente()).toList();

        Map<CriticidadDispositivo, Long> porCriticidad = contar(CriticidadDispositivo.class,
                datos.dispositivos().stream().map(DispositivoRed::criticidad).toList());
        long activos = datos.dispositivos().stream().filter(d -> d.estado() == EstadoDispositivo.ACTIVO).count();
        IndicadoresDispositivos dispositivos = new IndicadoresDispositivos(
                datos.dispositivos().size(),
                activos,
                datos.dispositivos().stream().filter(d -> auditados.contains(d.id())).count(),
                datos.dispositivos().stream()
                        .filter(d -> d.estado() == EstadoDispositivo.ACTIVO && !auditados.contains(d.id())).count(),
                porCriticidad);

        IndicadoresHallazgos hallazgos = new IndicadoresHallazgos(
                pendientes.size(),
                datos.hallazgos().stream().filter(h -> h.estado() == EstadoHallazgo.EN_REVISION).count(),
                datos.hallazgos().size() - pendientes.size(),
                contar(SeveridadRegla.class, pendientes.stream().map(HallazgoAuditoria::severidad).toList()),
                contar(SeveridadRegla.class, datos.hallazgos().stream()
                        .filter(h -> !h.estado().pendiente()).map(HallazgoAuditoria::severidad).toList()));

        IndicadoresAlertas alertas = new IndicadoresAlertas(
                vigentes.size(),
                vigentes.stream().filter(a -> a.estado() == EstadoAlerta.ACTIVA).count(),
                vigentes.stream().filter(a -> a.estado() == EstadoAlerta.EN_ATENCION).count(),
                vigentes.stream().filter(a -> a.severidad() == SeveridadRegla.CRITICA).count());

        Instant hace30Dias = ahora.minus(Duration.ofDays(30));
        IndicadoresAuditorias auditorias = new IndicadoresAuditorias(
                datos.auditorias().size(),
                datos.auditorias().stream()
                        .filter(a -> a.fechaEjecucion() != null && !a.fechaEjecucion().isBefore(hace30Dias)).count(),
                cumplimientoActual(datos.auditorias()));

        List<AuditoriaConfiguracion> recientes = datos.auditorias().stream()
                .sorted(MAS_RECIENTE)
                .limit(LIMITE_RECIENTES)
                .toList();

        List<RiesgoDispositivo> enRiesgo = riesgoPorDispositivo(datos).stream()
                .filter(r -> r.nivel() == NivelRiesgo.ALTO || r.nivel() == NivelRiesgo.MEDIO)
                .sorted(MAYOR_RIESGO)
                .limit(LIMITE_RECIENTES)
                .toList();

        NivelRiesgo nivel = EvaluadorRiesgo.evaluar(!datos.auditorias().isEmpty(), pendientes, vigentes,
                datos.dispositivosPorId());

        return new ResumenTablero(ahora, nivel, dispositivos, hallazgos, alertas, auditorias, recientes, enRiesgo,
                evolucion(datos, ahora));
    }

    /** Porcentaje de reglas cumplidas, tomando solo la última auditoría de cada dispositivo (estado actual). */
    static Integer cumplimientoActual(List<AuditoriaConfiguracion> auditorias) {
        Map<Long, AuditoriaConfiguracion> ultimas = new LinkedHashMap<>();
        auditorias.stream().sorted(MAS_RECIENTE)
                .forEach(auditoria -> ultimas.putIfAbsent(auditoria.dispositivoId(), auditoria));
        long total = ultimas.values().stream().mapToLong(a -> a.totalReglas() == null ? 0 : a.totalReglas()).sum();
        long cumplidas = ultimas.values().stream()
                .mapToLong(a -> a.reglasCumplidas() == null ? 0 : a.reglasCumplidas()).sum();
        return total == 0 ? null : (int) Math.round(cumplidas * 100.0 / total);
    }

    private List<RiesgoDispositivo> riesgoPorDispositivo(Datos datos) {
        Map<Long, DispositivoRed> porId = datos.dispositivosPorId();
        Map<Long, List<HallazgoAuditoria>> pendientes = datos.hallazgos().stream()
                .filter(h -> h.estado().pendiente())
                .collect(Collectors.groupingBy(HallazgoAuditoria::dispositivoId));
        Map<Long, List<AlertaCritica>> vigentes = datos.alertas().stream()
                .filter(a -> a.estado().vigente())
                .collect(Collectors.groupingBy(AlertaCritica::dispositivoId));
        Map<Long, Instant> ultimaAuditoria = new LinkedHashMap<>();
        datos.auditorias().stream().sorted(MAS_RECIENTE)
                .forEach(a -> ultimaAuditoria.putIfAbsent(a.dispositivoId(), a.fechaEjecucion()));

        List<RiesgoDispositivo> resultado = new ArrayList<>();
        for (DispositivoRed dispositivo : datos.dispositivos()) {
            List<HallazgoAuditoria> propios = pendientes.getOrDefault(dispositivo.id(), List.of());
            List<AlertaCritica> alertas = vigentes.getOrDefault(dispositivo.id(), List.of());
            resultado.add(new RiesgoDispositivo(
                    dispositivo.id(),
                    dispositivo.identificador(),
                    dispositivo.nombre(),
                    dispositivo.sedeId(),
                    dispositivo.criticidad(),
                    EvaluadorRiesgo.evaluar(ultimaAuditoria.containsKey(dispositivo.id()), propios, alertas, porId),
                    propios.size(),
                    propios.stream().filter(h -> h.severidad() == SeveridadRegla.CRITICA).count(),
                    propios.stream().filter(h -> h.severidad() == SeveridadRegla.ALTA).count(),
                    alertas.size(),
                    ultimaAuditoria.get(dispositivo.id())));
        }
        return resultado;
    }

    private List<RiesgoSede> riesgoPorSede(Datos datos) {
        Map<Long, DispositivoRed> porId = datos.dispositivosPorId();
        Set<Long> auditados = datos.dispositivosAuditados();
        Map<Long, List<DispositivoRed>> porSede = new LinkedHashMap<>();
        datos.dispositivos().stream()
                .sorted(Comparator.comparing(DispositivoRed::sedeId, Comparator.nullsLast(Comparator.naturalOrder())))
                .forEach(d -> porSede.computeIfAbsent(d.sedeId(), clave -> new ArrayList<>()).add(d));

        List<RiesgoSede> resultado = new ArrayList<>();
        porSede.forEach((sedeId, dispositivos) -> {
            Set<Long> ids = new HashSet<>();
            dispositivos.forEach(d -> ids.add(d.id()));
            List<HallazgoAuditoria> pendientes = datos.hallazgos().stream()
                    .filter(h -> h.estado().pendiente() && ids.contains(h.dispositivoId())).toList();
            List<AlertaCritica> vigentes = datos.alertas().stream()
                    .filter(a -> a.estado().vigente() && ids.contains(a.dispositivoId())).toList();
            long auditadosSede = ids.stream().filter(auditados::contains).count();
            resultado.add(new RiesgoSede(
                    sedeId,
                    EvaluadorRiesgo.evaluar(auditadosSede > 0, pendientes, vigentes, porId),
                    dispositivos.size(),
                    auditadosSede,
                    pendientes.size(),
                    pendientes.stream().filter(h -> h.severidad() == SeveridadRegla.CRITICA).count(),
                    vigentes.size()));
        });
        return resultado;
    }

    /** Últimas {@link #SEMANAS_EVOLUCION} semanas, de la más vieja a la actual. */
    static List<PuntoEvolucion> evolucion(Datos datos, Instant ahora) {
        LocalDate semanaActual = inicioSemana(ahora);
        LocalDate primera = semanaActual.minusWeeks(SEMANAS_EVOLUCION - 1L);
        Map<LocalDate, long[]> conteos = new LinkedHashMap<>();
        for (int i = 0; i < SEMANAS_EVOLUCION; i++) {
            conteos.put(primera.plusWeeks(i), new long[3]);
        }
        datos.auditorias().forEach(a -> sumar(conteos, a.fechaEjecucion(), 0));
        datos.hallazgos().forEach(h -> sumar(conteos, h.fechaDeteccion(), 1));
        datos.hallazgos().stream()
                .filter(h -> !h.estado().pendiente())
                .forEach(h -> sumar(conteos, h.fechaEstado(), 2));
        return conteos.entrySet().stream()
                .map(e -> new PuntoEvolucion(e.getKey(), e.getValue()[0], e.getValue()[1], e.getValue()[2]))
                .toList();
    }

    private static void sumar(Map<LocalDate, long[]> conteos, Instant fecha, int posicion) {
        if (fecha == null) {
            return;
        }
        long[] semana = conteos.get(inicioSemana(fecha));
        if (semana != null) {
            semana[posicion]++;
        }
    }

    static LocalDate inicioSemana(Instant instante) {
        return LocalDate.ofInstant(instante, ZONA).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    private static <E extends Enum<E>> Map<E, Long> contar(Class<E> tipo, List<E> valores) {
        Map<E, Long> resultado = new EnumMap<>(tipo);
        for (E valor : tipo.getEnumConstants()) {
            resultado.put(valor, 0L);
        }
        valores.stream().filter(v -> v != null).forEach(v -> resultado.merge(v, 1L, Long::sum));
        return resultado;
    }

    static final Comparator<AuditoriaConfiguracion> MAS_RECIENTE = Comparator
            .comparing(AuditoriaConfiguracion::fechaEjecucion, Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(AuditoriaConfiguracion::id, Comparator.nullsLast(Comparator.reverseOrder()));

    static final Comparator<RiesgoDispositivo> MAYOR_RIESGO = Comparator
            .comparing((RiesgoDispositivo r) -> -r.nivel().ordinal())
            .thenComparing((RiesgoDispositivo r) -> -r.criticosPendientes())
            .thenComparing((RiesgoDispositivo r) -> -r.altosPendientes())
            .thenComparing((RiesgoDispositivo r) -> r.criticidad() == null ? 1 : -r.criticidad().ordinal())
            .thenComparing(RiesgoDispositivo::identificador, Comparator.nullsLast(Comparator.naturalOrder()));
}
