package ar.edu.uai.tfi.auditcore.application.audit;

import ar.edu.uai.tfi.auditcore.application.alert.AlertaService;
import ar.edu.uai.tfi.auditcore.application.port.TrazabilidadPort;
import ar.edu.uai.tfi.auditcore.domain.model.AuditoriaConfiguracion;
import ar.edu.uai.tfi.auditcore.domain.model.CriticidadDispositivo;
import ar.edu.uai.tfi.auditcore.domain.model.DispositivoRed;
import ar.edu.uai.tfi.auditcore.domain.model.EstadoHallazgo;
import ar.edu.uai.tfi.auditcore.domain.model.FiltroHallazgos;
import ar.edu.uai.tfi.auditcore.domain.model.HallazgoAuditoria;
import ar.edu.uai.tfi.auditcore.domain.model.OperacionTransaccion;
import ar.edu.uai.tfi.auditcore.domain.model.SeguimientoHallazgo;
import ar.edu.uai.tfi.auditcore.domain.model.Transaccion;
import ar.edu.uai.tfi.auditcore.domain.repository.DispositivoRepository;
import ar.edu.uai.tfi.auditcore.domain.repository.HallazgoRepository;
import ar.edu.uai.tfi.auditcore.domain.repository.SeguimientoHallazgoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

/**
 * Consulta, priorización y seguimiento de hallazgos (RF-009, CU-005-001).
 *
 * Priorización: primero los pendientes (ABIERTO, EN_REVISION), después por severidad, por criticidad del
 * dispositivo (CU-002-001, regla 3) y por fecha de detección, de la más reciente a la más vieja.
 */
@ApplicationScoped
public class HallazgoService {

    static final String ENTIDAD = "HALLAZGO";
    /** Usuario con que se registran los cambios automáticos (re-auditoría que cumple la regla, D-26). */
    public static final String USUARIO_SISTEMA = "SISTEMA";

    private final HallazgoRepository repository;
    private final SeguimientoHallazgoRepository seguimientoRepository;
    private final DispositivoRepository dispositivoRepository;
    private final TrazabilidadPort trazabilidad;
    private final AlertaService alertaService;

    public HallazgoService(HallazgoRepository repository,
                           SeguimientoHallazgoRepository seguimientoRepository,
                           DispositivoRepository dispositivoRepository,
                           TrazabilidadPort trazabilidad,
                           AlertaService alertaService) {
        this.repository = repository;
        this.seguimientoRepository = seguimientoRepository;
        this.dispositivoRepository = dispositivoRepository;
        this.trazabilidad = trazabilidad;
        this.alertaService = alertaService;
    }

    /** Hallazgo con la criticidad actual de su dispositivo, usada para priorizar. */
    public record HallazgoPriorizado(HallazgoAuditoria hallazgo, CriticidadDispositivo criticidadDispositivo) {
    }

    public List<HallazgoPriorizado> buscarPriorizados(FiltroHallazgos filtro) {
        FiltroHallazgos criterios = filtro == null ? FiltroHallazgos.todos(null) : filtro;
        if (criterios.desde() != null && criterios.hasta() != null && criterios.desde().isAfter(criterios.hasta())) {
            throw new IllegalArgumentException("La fecha 'desde' no puede ser posterior a 'hasta'.");
        }
        Map<Long, CriticidadDispositivo> criticidades = criticidades(criterios.organizacionId());
        return repository.buscar(criterios).stream()
                .map(hallazgo -> new HallazgoPriorizado(hallazgo, criticidades.get(hallazgo.dispositivoId())))
                .sorted(PRIORIDAD)
                .toList();
    }

    public HallazgoPriorizado buscar(Long id, Long organizacionId) {
        HallazgoAuditoria hallazgo = obtener(id, organizacionId);
        return new HallazgoPriorizado(hallazgo, criticidadDe(hallazgo.dispositivoId()));
    }

    /**
     * Cambia el estado de seguimiento (PATCH /api/findings/{id}/status). Registra quién y cuándo en el
     * hallazgo, el cambio con su comentario en el historial y la transacción en la bitácora. La evidencia,
     * la severidad y la regla del hallazgo no se modifican.
     *
     * @param organizacionId null para el administrador; si no, el hallazgo debe ser de esa organización
     */
    @Transactional
    public HallazgoPriorizado cambiarEstado(Long id, Long organizacionId, String estado, String comentario,
                                            String actor) {
        HallazgoAuditoria anterior = obtener(id, organizacionId);
        EstadoHallazgo nuevoEstado = parsearEstado(estado);
        String comentarioLimpio = comentario == null || comentario.isBlank() ? null : comentario.trim();
        String usuario = recortar(actor == null || actor.isBlank() ? "SISTEMA_O_ANONIMO" : actor, 120);

        if (anterior.estado() == nuevoEstado) {
            throw new IllegalStateException("El hallazgo ya está " + nuevoEstado + ".");
        }
        if (!anterior.estado().siguientesPermitidos().contains(nuevoEstado)) {
            throw new IllegalStateException("No se puede pasar de " + anterior.estado() + " a " + nuevoEstado
                    + ". Un hallazgo " + anterior.estado() + " solo puede pasar a "
                    + anterior.estado().siguientesPermitidos() + ".");
        }
        if (nuevoEstado.requiereComentario() && comentarioLimpio == null) {
            throw new IllegalArgumentException("Para pasar el hallazgo a " + nuevoEstado
                    + " hay que indicar un comentario que lo justifique.");
        }
        if (comentarioLimpio != null && comentarioLimpio.length() > 1000) {
            throw new IllegalArgumentException("El comentario no puede superar los 1000 caracteres.");
        }

        Instant ahora = Instant.now();
        HallazgoAuditoria actualizado = repository.actualizarEstado(anterior.id(), anterior.estado(), nuevoEstado,
                usuario, ahora);
        seguimientoRepository.guardar(new SeguimientoHallazgo(null, anterior.id(), anterior.estado(), nuevoEstado,
                comentarioLimpio, usuario, ahora));
        trazabilidad.registrar(new Transaccion(ENTIDAD, actualizado.id(), OperacionTransaccion.CAMBIO_ESTADO,
                actualizado.organizacionId(), anterior, actualizado, usuario,
                recortar(anterior.estado() + " -> " + nuevoEstado
                        + (comentarioLimpio == null ? "" : ": " + comentarioLimpio), 1000)));
        // Si el hallazgo tiene alerta crítica, se cierra o se reactiva con él (CU-006-002).
        alertaService.sincronizarConHallazgo(actualizado, usuario, comentarioLimpio);

        return new HallazgoPriorizado(actualizado, criticidadDe(actualizado.dispositivoId()));
    }

    /**
     * Resuelve automáticamente un hallazgo pendiente porque una auditoría posterior del mismo dispositivo cumple
     * la regla (CU-005-001: "RESUELTO se confirma con una nueva auditoría"; decisión D-26). Queda en el historial
     * de seguimiento y en la bitácora con el usuario SISTEMA, y su alerta se cierra.
     *
     * No es @Transactional a propósito: corre dentro de la transacción de la auditoría y, si otro usuario cambió
     * el hallazgo en ese momento, simplemente no lo toca (devuelve false) sin anular la auditoría.
     *
     * @return true si lo resolvió
     */
    public boolean resolverPorReauditoria(HallazgoAuditoria pendiente, AuditoriaConfiguracion auditoria) {
        if (pendiente == null || !pendiente.estado().pendiente()) {
            return false;
        }
        String comentario = recortar("Resuelto automáticamente: la regla " + pendiente.codigoRegla()
                + " se cumple en la auditoría " + String.format("AUD-%03d", auditoria.id())
                + " ejecutada por " + auditoria.ejecutadoPor() + ".", 1000);
        Instant ahora = Instant.now();
        HallazgoAuditoria actualizado;
        try {
            actualizado = repository.actualizarEstado(pendiente.id(), pendiente.estado(), EstadoHallazgo.RESUELTO,
                    USUARIO_SISTEMA, ahora);
        } catch (IllegalStateException cambiadoMientrasTanto) {
            return false;
        }
        seguimientoRepository.guardar(new SeguimientoHallazgo(null, pendiente.id(), pendiente.estado(),
                EstadoHallazgo.RESUELTO, comentario, USUARIO_SISTEMA, ahora));
        trazabilidad.registrar(new Transaccion(ENTIDAD, actualizado.id(), OperacionTransaccion.CAMBIO_ESTADO,
                actualizado.organizacionId(), pendiente, actualizado, USUARIO_SISTEMA,
                recortar(pendiente.estado() + " -> RESUELTO: " + comentario, 1000)));
        alertaService.sincronizarConHallazgo(actualizado, USUARIO_SISTEMA, comentario, false);
        return true;
    }

    /** Historial de cambios de estado del hallazgo, del más viejo al más nuevo. */
    public List<SeguimientoHallazgo> seguimiento(Long id, Long organizacionId) {
        HallazgoAuditoria hallazgo = obtener(id, organizacionId);
        return seguimientoRepository.listarPorHallazgo(hallazgo.id());
    }

    static final Comparator<HallazgoPriorizado> PRIORIDAD = Comparator
            .comparing((HallazgoPriorizado h) -> h.hallazgo().estado().pendiente() ? 0 : 1)
            .thenComparing((HallazgoPriorizado h) -> -h.hallazgo().severidad().ordinal())
            .thenComparing((HallazgoPriorizado h) -> h.criticidadDispositivo() == null
                    ? 1 : -h.criticidadDispositivo().ordinal())
            .thenComparing((HallazgoPriorizado h) -> h.hallazgo().fechaDeteccion(),
                    Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing((HallazgoPriorizado h) -> h.hallazgo().id(), Comparator.nullsLast(Comparator.reverseOrder()));

    private HallazgoAuditoria obtener(Long id, Long organizacionId) {
        validarId(id, "id");
        return (organizacionId == null
                ? repository.buscarPorId(id)
                : repository.buscarPorIdYOrganizacion(id, organizacionId))
                .orElseThrow(() -> new NoSuchElementException("No existe el hallazgo solicitado."));
    }

    private Map<Long, CriticidadDispositivo> criticidades(Long organizacionId) {
        List<DispositivoRed> dispositivos = organizacionId == null
                ? dispositivoRepository.listar()
                : dispositivoRepository.listarPorOrganizacion(organizacionId);
        return dispositivos.stream()
                .collect(Collectors.toMap(DispositivoRed::id, DispositivoRed::criticidad, (a, b) -> a));
    }

    private CriticidadDispositivo criticidadDe(Long dispositivoId) {
        return dispositivoRepository.buscarPorId(dispositivoId).map(DispositivoRed::criticidad).orElse(null);
    }

    private static EstadoHallazgo parsearEstado(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("El campo estado es obligatorio.");
        }
        try {
            return EstadoHallazgo.valueOf(valor.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("El estado debe ser ABIERTO, EN_REVISION, RESUELTO o ACEPTADO.");
        }
    }

    private static String recortar(String valor, int maximo) {
        return valor.length() <= maximo ? valor : valor.substring(0, maximo);
    }

    private static void validarId(Long valor, String campo) {
        if (valor == null || valor <= 0) {
            throw new IllegalArgumentException("El campo " + campo + " debe ser un identificador válido.");
        }
    }
}
