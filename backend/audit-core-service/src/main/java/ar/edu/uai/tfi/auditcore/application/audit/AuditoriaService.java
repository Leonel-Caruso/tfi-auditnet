package ar.edu.uai.tfi.auditcore.application.audit;

import ar.edu.uai.tfi.auditcore.application.port.TrazabilidadPort;
import ar.edu.uai.tfi.auditcore.domain.model.*;
import ar.edu.uai.tfi.auditcore.domain.repository.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
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

    public AuditoriaService(
            ConfiguracionRepository configuracionRepository,
            DispositivoRepository dispositivoRepository,
            BaselineRepository baselineRepository,
            ReglaBaselineRepository reglaRepository,
            AuditoriaRepository auditoriaRepository,
            EvaluacionReglaRepository evaluacionRepository,
            HallazgoRepository hallazgoRepository,
            MotorAuditoriaService motor,
            TrazabilidadPort trazabilidad
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
        DispositivoRed dispositivo = dispositivoRepository.buscarPorId(configuracion.dispositivoId())
                .orElseThrow(() -> new NoSuchElementException("No existe el dispositivo asociado a la configuración."));

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
                hallazgoRepository.guardar(new HallazgoAuditoria(
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
                        EstadoHallazgo.ABIERTO,
                        ahora
                ));
            }
        }

        trazabilidad.registrar(
                actorSeguro(actor),
                "AUDITORIA_EJECUTADA",
                "auditoriaId=" + auditoria.id()
                        + ", configuracionId=" + configuracion.id()
                        + ", baselineId=" + baseline.id()
                        + ", reglas=" + calculadas.size()
                        + ", hallazgos=" + hallazgos
        );

        return armarDetalle(auditoria);
    }

    private AuditoriaDetalle armarDetalle(AuditoriaConfiguracion auditoria) {
        return new AuditoriaDetalle(
                auditoria,
                evaluacionRepository.listarPorAuditoria(auditoria.id()),
                hallazgoRepository.listarPorAuditoria(auditoria.id())
        );
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
