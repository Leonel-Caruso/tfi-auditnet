package ar.edu.uai.tfi.auditcore.application.audit;

import ar.edu.uai.tfi.auditcore.application.alert.AlertaService;
import ar.edu.uai.tfi.auditcore.domain.model.OperacionTransaccion;
import ar.edu.uai.tfi.auditcore.domain.model.Transaccion;
import ar.edu.uai.tfi.auditcore.application.port.TrazabilidadPort;
import ar.edu.uai.tfi.auditcore.domain.model.*;
import ar.edu.uai.tfi.auditcore.domain.repository.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@ApplicationScoped
public class AuditoriaService {

    private final ConfiguracionRepository configuracionRepository;
    private final DispositivoRepository dispositivoRepository;
    private final BaselineRepository baselineRepository;
    private final ReglaBaselineRepository reglaRepository;
    private final AuditoriaRepository auditoriaRepository;
    private final EvaluacionReglaRepository evaluacionRepository;
    private final HallazgoRepository hallazgoRepository;
    private final MotorAuditoriaService motor;
    private final TrazabilidadPort trazabilidad;
    private final AlertaService alertaService;
    private final HallazgoService hallazgoService;

    public AuditoriaService(
            ConfiguracionRepository configuracionRepository,
            DispositivoRepository dispositivoRepository,
            BaselineRepository baselineRepository,
            ReglaBaselineRepository reglaRepository,
            AuditoriaRepository auditoriaRepository,
            EvaluacionReglaRepository evaluacionRepository,
            HallazgoRepository hallazgoRepository,
            MotorAuditoriaService motor,
            TrazabilidadPort trazabilidad,
            AlertaService alertaService,
            HallazgoService hallazgoService
    ) {
        this.configuracionRepository = configuracionRepository;
        this.dispositivoRepository = dispositivoRepository;
        this.baselineRepository = baselineRepository;
        this.reglaRepository = reglaRepository;
        this.auditoriaRepository = auditoriaRepository;
        this.evaluacionRepository = evaluacionRepository;
        this.hallazgoRepository = hallazgoRepository;
        this.motor = motor;
        this.trazabilidad = trazabilidad;
        this.alertaService = alertaService;
        this.hallazgoService = hallazgoService;
    }

    @Transactional
    public AuditoriaDetalle ejecutar(Long configuracionId, String actor) {
        validarId(configuracionId, "configuracionId");
        ConfiguracionDispositivo configuracion = configuracionRepository.buscarPorId(configuracionId)
                .orElseThrow(() -> new NoSuchElementException("No existe la configuración seleccionada."));
        return ejecutarConfiguracion(configuracion, actor);
    }

    @Transactional
    public AuditoriaDetalle ejecutarPorOrganizacion(Long configuracionId, Long organizacionId, String actor) {
        validarId(configuracionId, "configuracionId");
        validarId(organizacionId, "organizacionId");
        ConfiguracionDispositivo configuracion = configuracionRepository
                .buscarPorIdYOrganizacion(configuracionId, organizacionId)
                .orElseThrow(() -> new NoSuchElementException("No existe la configuración seleccionada."));
        return ejecutarConfiguracion(configuracion, actor);
    }

    public List<AuditoriaConfiguracion> listarHistorial() {
        return auditoriaRepository.listarHistorial();
    }

    public List<AuditoriaConfiguracion> listarHistorialPorOrganizacion(Long organizacionId) {
        validarId(organizacionId, "organizacionId");
        return auditoriaRepository.listarHistorialPorOrganizacion(organizacionId);
    }

    public AuditoriaDetalle buscarDetalle(Long id) {
        validarId(id, "id");
        AuditoriaConfiguracion auditoria = auditoriaRepository.buscarPorId(id)
                .orElseThrow(() -> new NoSuchElementException("No existe la auditoría solicitada."));
        return armarDetalle(auditoria);
    }

    public AuditoriaDetalle buscarDetallePorOrganizacion(Long id, Long organizacionId) {
        validarId(id, "id");
        validarId(organizacionId, "organizacionId");
        AuditoriaConfiguracion auditoria = auditoriaRepository.buscarPorIdYOrganizacion(id, organizacionId)
                .orElseThrow(() -> new NoSuchElementException("No existe la auditoría solicitada."));
        return armarDetalle(auditoria);
    }

    private AuditoriaDetalle ejecutarConfiguracion(ConfiguracionDispositivo configuracion, String actor) {
        // Bloquea el dispositivo: dos auditorías simultáneas del mismo equipo se ejecutan en orden y no generan dos
        // alertas para la misma regla (D-26).
        DispositivoRed dispositivo = dispositivoRepository.buscarParaActualizar(configuracion.dispositivoId())
                .orElseThrow(() -> new NoSuchElementException("No existe el dispositivo asociado a la configuración."));
        if (dispositivo.estado() != EstadoDispositivo.ACTIVO) {
            throw new IllegalStateException("El dispositivo " + dispositivo.identificador()
                    + " está inactivo: no se pueden ejecutar auditorías sobre él.");
        }

        BaselineConfiguracion baseline = baselineRepository
                .buscarActivaAplicable(configuracion.organizacionId(), dispositivo.tipoDispositivoId())
                .orElseThrow(() -> new IllegalStateException(
                        "No existe una baseline activa aplicable al tipo de dispositivo seleccionado."
                ));

        List<ReglaBaseline> reglas = reglaRepository.listarPorBaseline(baseline.id()).stream()
                .filter(regla -> regla.estado() == EstadoRegla.ACTIVA)
                .toList();

        if (reglas.isEmpty()) {
            throw new IllegalStateException("La baseline aplicable no posee reglas activas para ejecutar la auditoría.");
        }

        List<MotorAuditoriaService.EvaluacionCalculada> calculadas =
                motor.evaluar(configuracion.contenidoNormalizado(), reglas);

        int cumplidas = (int) calculadas.stream().filter(MotorAuditoriaService.EvaluacionCalculada::cumple).count();
        int hallazgos = calculadas.size() - cumplidas;
        String severidadMaxima = calculadas.stream()
                .filter(e -> !e.cumple())
                .map(MotorAuditoriaService.EvaluacionCalculada::severidad)
                .max(Comparator.comparingInt(this::rangoSeveridad))
                .map(Enum::name)
                .orElse("NINGUNA");

        Instant ahora = Instant.now();
        AuditoriaConfiguracion auditoria = auditoriaRepository.guardar(new AuditoriaConfiguracion(
                null,
                configuracion.id(),
                configuracion.dispositivoId(),
                baseline.id(),
                configuracion.organizacionId(),
                ahora,
                actorSeguro(actor),
                calculadas.size(),
                cumplidas,
                hallazgos,
                severidadMaxima,
                hallazgos == 0 ? "CUMPLE" : "CON_HALLAZGOS",
                EstadoAuditoria.FINALIZADA
        ));

        for (MotorAuditoriaService.EvaluacionCalculada calculada : calculadas) {
            evaluacionRepository.guardar(new ResultadoReglaAuditoria(
                    null,
                    auditoria.id(),
                    calculada.reglaId(),
                    calculada.codigoRegla(),
                    calculada.nombreRegla(),
                    calculada.tipo(),
                    calculada.patron(),
                    calculada.severidad(),
                    calculada.cumple(),
                    calculada.evidencia()
            ));

            if (!calculada.cumple()) {
                HallazgoAuditoria hallazgo = hallazgoRepository.guardar(new HallazgoAuditoria(
                        null,
                        auditoria.id(),
                        calculada.reglaId(),
                        configuracion.dispositivoId(),
                        baseline.id(),
                        configuracion.organizacionId(),
                        calculada.codigoRegla(),
                        calculada.nombreRegla(),
                        calculada.tipo(),
                        calculada.patron(),
                        calculada.severidad(),
                        calculada.evidencia(),
                        calculada.recomendacion(),
                        calculada.impacto(),
                        EstadoHallazgo.ABIERTO,
                        ahora,
                        null,
                        null
                ));
                // CRITICA o ALTA: alerta interna (RF-012, CU-006-002).
                alertaService.generar(hallazgo, actorSeguro(actor));
            }
        }

        int resueltos = resolverHallazgosAnteriores(auditoria, configuracion, calculadas);

        trazabilidad.registrar(new Transaccion(
                "AUDITORIA",
                auditoria.id(),
                OperacionTransaccion.EJECUCION,
                auditoria.organizacionId(),
                null,
                auditoria,
                actorSeguro(actor),
                "auditoriaId=" + auditoria.id()
                        + ", configuracionId=" + configuracion.id()
                        + ", baselineId=" + baseline.id()
                        + ", reglas=" + calculadas.size()
                        + ", hallazgos=" + hallazgos
                        + ", resueltosAutomaticamente=" + resueltos
        ));

        return armarDetalle(auditoria);
    }

    /**
     * Re-auditoría (D-26): los hallazgos pendientes del mismo dispositivo cuya regla (mismo código) ahora se cumple
     * pasan a RESUELTO automáticamente. Solo los detectados sobre una versión de configuración igual o anterior a
     * la auditada: re-auditar una versión vieja no "resuelve" lo que falla en una más nueva.
     */
    private int resolverHallazgosAnteriores(AuditoriaConfiguracion auditoria, ConfiguracionDispositivo configuracion,
                                            List<MotorAuditoriaService.EvaluacionCalculada> calculadas) {
        java.util.Set<String> cumplen = calculadas.stream()
                .filter(MotorAuditoriaService.EvaluacionCalculada::cumple)
                .map(MotorAuditoriaService.EvaluacionCalculada::codigoRegla)
                .collect(java.util.stream.Collectors.toSet());
        if (cumplen.isEmpty()) {
            return 0;
        }
        int resueltos = 0;
        java.util.Map<Long, Integer> versionPorAuditoria = new java.util.HashMap<>();
        for (HallazgoAuditoria previo : hallazgoRepository.buscar(new FiltroHallazgos(auditoria.organizacionId(),
                null, null, auditoria.dispositivoId(), null, null, null, null))) {
            if (previo.auditoriaId().equals(auditoria.id()) || !previo.estado().pendiente()
                    || !cumplen.contains(previo.codigoRegla())) {
                continue;
            }
            Integer versionPrevia = versionPorAuditoria.computeIfAbsent(previo.auditoriaId(), id ->
                    auditoriaRepository.buscarPorId(id)
                            .flatMap(previa -> configuracionRepository.buscarPorId(previa.configuracionId()))
                            .map(ConfiguracionDispositivo::version)
                            .orElse(null));
            if (versionPrevia != null && configuracion.version() != null && versionPrevia <= configuracion.version()
                    && hallazgoService.resolverPorReauditoria(previo, auditoria)) {
                resueltos++;
            }
        }
        return resueltos;
    }

    private AuditoriaDetalle armarDetalle(AuditoriaConfiguracion auditoria) {
        // La configuración evaluada y la baseline usada se conservan siempre (CU-005-002, reglas 2 y 3).
        return new AuditoriaDetalle(
                auditoria,
                evaluacionRepository.listarPorAuditoria(auditoria.id()),
                hallazgoRepository.listarPorAuditoria(auditoria.id()),
                configuracionRepository.buscarPorId(auditoria.configuracionId()).orElse(null),
                baselineRepository.buscarPorId(auditoria.baselineId()).orElse(null)
        );
    }

    /**
     * Historial filtrado (GET /api/audits/history con filtros). Solo lectura: no modifica la evidencia.
     */
    public List<AuditoriaConfiguracion> buscarHistorial(FiltroAuditorias filtro) {
        FiltroAuditorias criterios = filtro == null
                ? new FiltroAuditorias(null, null, null, null, null, null, null, null)
                : filtro;
        if (criterios.desde() != null && criterios.hasta() != null && criterios.desde().isAfter(criterios.hasta())) {
            throw new IllegalArgumentException("La fecha 'desde' no puede ser posterior a 'hasta'.");
        }
        return auditoriaRepository.buscarHistorial(criterios);
    }

    /** Resultado de comparar una auditoría con la anterior del mismo dispositivo. */
    public record ComparacionAuditoria(
            AuditoriaConfiguracion actual,
            AuditoriaConfiguracion anterior,
            List<ComparadorAuditorias.CambioRegla> cambios,
            Map<ComparadorAuditorias.Categoria, Long> resumen
    ) {
    }

    /**
     * Compara la auditoría con la inmediatamente anterior del mismo dispositivo (evolución en el tiempo,
     * CU-005-002). Si es la primera del dispositivo, {@code anterior} es null y todas las reglas figuran
     * como nuevas.
     *
     * @param organizacionId null para el administrador
     */
    public ComparacionAuditoria compararConAnterior(Long id, Long organizacionId) {
        validarId(id, "id");
        AuditoriaConfiguracion actual = (organizacionId == null
                ? auditoriaRepository.buscarPorId(id)
                : auditoriaRepository.buscarPorIdYOrganizacion(id, organizacionId))
                .orElseThrow(() -> new NoSuchElementException("No existe la auditoría solicitada."));

        AuditoriaConfiguracion anterior = auditoriaRepository
                .buscarHistorial(FiltroAuditorias.delDispositivo(actual.organizacionId(), actual.dispositivoId()))
                .stream()
                .filter(otra -> esAnterior(otra, actual))
                .max(Comparator.comparing(AuditoriaConfiguracion::fechaEjecucion)
                        .thenComparing(AuditoriaConfiguracion::id))
                .orElse(null);

        List<ComparadorAuditorias.CambioRegla> cambios = ComparadorAuditorias.comparar(
                anterior == null ? List.of() : evaluacionRepository.listarPorAuditoria(anterior.id()),
                evaluacionRepository.listarPorAuditoria(actual.id()));
        return new ComparacionAuditoria(actual, anterior, cambios, ComparadorAuditorias.resumen(cambios));
    }

    private static boolean esAnterior(AuditoriaConfiguracion otra, AuditoriaConfiguracion actual) {
        int porFecha = otra.fechaEjecucion().compareTo(actual.fechaEjecucion());
        return porFecha < 0 || (porFecha == 0 && otra.id() < actual.id());
    }

    private int rangoSeveridad(SeveridadRegla severidad) {
        return switch (severidad) {
            case BAJA -> 1;
            case MEDIA -> 2;
            case ALTA -> 3;
            case CRITICA -> 4;
        };
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
