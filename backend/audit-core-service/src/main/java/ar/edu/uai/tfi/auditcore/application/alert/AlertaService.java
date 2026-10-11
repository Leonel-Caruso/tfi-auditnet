package ar.edu.uai.tfi.auditcore.application.alert;

import ar.edu.uai.tfi.auditcore.application.port.TrazabilidadPort;
import ar.edu.uai.tfi.auditcore.domain.model.AlertaCritica;
import ar.edu.uai.tfi.auditcore.domain.model.CriticidadDispositivo;
import ar.edu.uai.tfi.auditcore.domain.model.DispositivoRed;
import ar.edu.uai.tfi.auditcore.domain.model.EstadoAlerta;
import ar.edu.uai.tfi.auditcore.domain.model.FiltroAlertas;
import ar.edu.uai.tfi.auditcore.domain.model.FiltroHallazgos;
import ar.edu.uai.tfi.auditcore.domain.model.HallazgoAuditoria;
import ar.edu.uai.tfi.auditcore.domain.model.OperacionTransaccion;
import ar.edu.uai.tfi.auditcore.domain.model.PerfilTecnico;
import ar.edu.uai.tfi.auditcore.domain.model.Transaccion;
import ar.edu.uai.tfi.auditcore.domain.repository.AlertaRepository;
import ar.edu.uai.tfi.auditcore.domain.repository.DispositivoRepository;
import ar.edu.uai.tfi.auditcore.domain.repository.HallazgoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Alertas críticas internas (RF-012, CU-006-002).
 *
 * <ul>
 *   <li>Se generan al auditar, una por cada hallazgo CRITICA o ALTA.</li>
 *   <li>Siguen al hallazgo: cuando se resuelve o se acepta el riesgo, la alerta se cierra; si se reabre, se
 *       reactiva.</li>
 *   <li>Desde la pantalla de alertas se derivan a un perfil técnico (EN_ATENCION) o se devuelven (ACTIVA).</li>
 *   <li>Priorización: vigentes primero, después severidad, criticidad del dispositivo y fecha.</li>
 * </ul>
 */
@ApplicationScoped
public class AlertaService {

    static final String ENTIDAD = "ALERTA";

    private final AlertaRepository repository;
    private final HallazgoRepository hallazgoRepository;
    private final DispositivoRepository dispositivoRepository;
    private final TrazabilidadPort trazabilidad;

    public AlertaService(AlertaRepository repository,
                         HallazgoRepository hallazgoRepository,
                         DispositivoRepository dispositivoRepository,
                         TrazabilidadPort trazabilidad) {
        this.repository = repository;
        this.hallazgoRepository = hallazgoRepository;
        this.dispositivoRepository = dispositivoRepository;
        this.trazabilidad = trazabilidad;
    }

    /** Alerta con el dispositivo afectado y el hallazgo de origen (null si no se encontraron). */
    public record AlertaPriorizada(AlertaCritica alerta, DispositivoRed dispositivo, HallazgoAuditoria hallazgo) {
    }

    /** Detalle: alerta, dispositivo y hallazgo de origen (evidencia, impacto, auditoría). */
    public record AlertaDetalle(AlertaCritica alerta, DispositivoRed dispositivo, HallazgoAuditoria hallazgo) {
    }

    /**
     * Genera la alerta de un hallazgo recién detectado si es CRITICA o ALTA y no hay otra alerta vigente del mismo
     * dispositivo y regla. La llama el motor de auditoría dentro de la misma transacción.
     */
    public Optional<AlertaCritica> generar(HallazgoAuditoria hallazgo, String actor) {
        if (hallazgo == null || !AlertaCritica.corresponde(hallazgo.severidad())) {
            return Optional.empty();
        }
        // Una sola alerta vigente por dispositivo y regla: si una re-auditoría vuelve a fallar, el problema ya
        // tiene su alerta (D-26). Si ahora la severidad es mayor (la regla pasó de ALTA a CRITICA), la alerta
        // anterior se cierra y la nueva toma su lugar, para que se priorice como corresponde.
        Optional<AlertaCritica> vigente = alertaVigenteDeLaRegla(hallazgo);
        if (vigente.isPresent()) {
            if (hallazgo.severidad().ordinal() <= vigente.get().severidad().ordinal()) {
                return Optional.empty();
            }
            cambiar(vigente.get(), EstadoAlerta.CERRADA, vigente.get().perfilAsignado(),
                    "Reemplazada por la alerta del hallazgo " + hallazgo.id() + " (severidad " + hallazgo.severidad()
                            + ")", actorSeguro(actor));
        }
        AlertaCritica alerta = repository.guardar(new AlertaCritica(
                null,
                hallazgo.id(),
                hallazgo.auditoriaId(),
                hallazgo.dispositivoId(),
                hallazgo.organizacionId(),
                hallazgo.severidad(),
                EstadoAlerta.ACTIVA,
                hallazgo.fechaDeteccion() == null ? Instant.now() : hallazgo.fechaDeteccion(),
                null,
                null,
                null,
                null
        ));
        trazabilidad.registrar(new Transaccion(ENTIDAD, alerta.id(), OperacionTransaccion.ALTA,
                alerta.organizacionId(), null, alerta, actorSeguro(actor),
                "Generada por el hallazgo " + hallazgo.id() + " (" + hallazgo.codigoRegla() + ", "
                        + hallazgo.severidad() + ") de la auditoría " + hallazgo.auditoriaId()));
        return Optional.of(alerta);
    }

    /**
     * Mantiene la alerta alineada con su hallazgo después de un cambio de estado (CU-006-002, flujo
     * alternativo 4): hallazgo cerrado → alerta CERRADA; hallazgo reabierto → alerta ACTIVA. Equivale a
     * {@code sincronizarConHallazgo(hallazgo, usuario, comentario, true)}.
     */
    public void sincronizarConHallazgo(HallazgoAuditoria hallazgo, String usuario, String comentario) {
        sincronizarConHallazgo(hallazgo, usuario, comentario, true);
    }

    /**
     * Sincroniza la alerta con el hallazgo y mantiene una sola alerta vigente por dispositivo y regla (D-26):
     * <ul>
     *   <li>Hallazgo cerrado: su alerta se cierra. Si {@code trasladar} y queda otro hallazgo pendiente del mismo
     *       dispositivo y regla sin alerta (una re-auditoría que siguió fallando), se genera la alerta para ese.</li>
     *   <li>Hallazgo pendiente (reabierto): si no hay otra alerta vigente del mismo dispositivo y regla, su
     *       alerta se reactiva (o se genera, si no tenía).</li>
     * </ul>
     *
     * @param trasladar false cuando el cierre es automático por una re-auditoría que cumple la regla: en ese caso
     *                  se cierran todos los pendientes de la regla y no hay a quién trasladarla
     */
    public void sincronizarConHallazgo(HallazgoAuditoria hallazgo, String usuario, String comentario,
                                       boolean trasladar) {
        if (hallazgo == null || hallazgo.id() == null) {
            return;
        }
        Optional<AlertaCritica> propia = repository.buscarPorHallazgo(hallazgo.id());
        String motivo = comentario == null ? "" : ": " + comentario;

        if (!hallazgo.estado().pendiente()) {
            if (propia.isEmpty() || !propia.get().estado().vigente()) {
                return;
            }
            cambiar(propia.get(), EstadoAlerta.CERRADA, propia.get().perfilAsignado(),
                    "Hallazgo " + hallazgo.estado() + motivo, usuario);
            if (trasladar) {
                otroPendienteSinAlertaVigente(hallazgo).ifPresent(siguiente -> {
                    Optional<AlertaCritica> anterior = repository.buscarPorHallazgo(siguiente.id());
                    if (anterior.isPresent()) {
                        // Ya tuvo alerta (cerrada): se reactiva, porque la base admite una sola por hallazgo.
                        cambiar(anterior.get(), EstadoAlerta.ACTIVA, null,
                                "Sigue pendiente después de cerrar el hallazgo " + hallazgo.id(), usuario);
                    } else {
                        generar(siguiente, usuario);
                    }
                });
            }
            return;
        }

        if (alertaVigenteDeLaRegla(hallazgo).isPresent()) {
            return;
        }
        if (propia.isEmpty()) {
            generar(hallazgo, usuario);
        } else if (!propia.get().estado().vigente()) {
            cambiar(propia.get(), EstadoAlerta.ACTIVA, null, "Hallazgo reabierto" + motivo, usuario);
        }
    }

    private void cambiar(AlertaCritica alerta, EstadoAlerta nuevo, PerfilTecnico perfil, String detalle,
                         String usuario) {
        // Sin verificar el estado esperado: si otro usuario la derivó mientras tanto, igual sigue vigente y el
        // cierre del hallazgo debe cerrarla (no tiene sentido rechazar el cambio del hallazgo por eso).
        AlertaCritica actualizada = repository.actualizarEstado(alerta.id(), null, nuevo, perfil,
                recortar(detalle, 1000), usuario, Instant.now());
        registrarCambio(alerta, actualizada, usuario, detalle);
    }

    /** Alerta vigente de otro hallazgo del mismo dispositivo y la misma regla (código), si existe. */
    private Optional<AlertaCritica> alertaVigenteDeLaRegla(HallazgoAuditoria hallazgo) {
        return repository.buscar(new FiltroAlertas(hallazgo.organizacionId(), null, true, null,
                        hallazgo.dispositivoId())).stream()
                .filter(alerta -> !alerta.hallazgoId().equals(hallazgo.id()))
                .filter(alerta -> hallazgoRepository.buscarPorId(alerta.hallazgoId())
                        .map(otro -> otro.codigoRegla().equals(hallazgo.codigoRegla()))
                        .orElse(false))
                .findFirst();
    }

    /** El hallazgo pendiente más reciente del mismo dispositivo y regla que no tiene una alerta vigente. */
    private Optional<HallazgoAuditoria> otroPendienteSinAlertaVigente(HallazgoAuditoria hallazgo) {
        return hallazgoRepository.buscar(new FiltroHallazgos(hallazgo.organizacionId(), null, null,
                        hallazgo.dispositivoId(), null, null, null, null)).stream()
                .filter(otro -> !otro.id().equals(hallazgo.id()))
                .filter(otro -> otro.estado().pendiente())
                .filter(otro -> otro.codigoRegla().equals(hallazgo.codigoRegla()))
                .filter(otro -> AlertaCritica.corresponde(otro.severidad()))
                .filter(otro -> repository.buscarPorHallazgo(otro.id())
                        .map(alerta -> !alerta.estado().vigente())
                        .orElse(true))
                .max(Comparator.comparing(HallazgoAuditoria::fechaDeteccion).thenComparing(HallazgoAuditoria::id));
    }

    /**
     * Alertas priorizadas (GET /api/alerts/critical). La sede y la criticidad se filtran con los datos del
     * dispositivo afectado.
     */
    public List<AlertaPriorizada> buscarPriorizadas(FiltroAlertas filtro, Long sedeId,
                                                   CriticidadDispositivo criticidad) {
        FiltroAlertas criterios = filtro == null ? FiltroAlertas.vigentes(null) : filtro;
        Map<Long, DispositivoRed> dispositivos = dispositivos(criterios.organizacionId());
        Map<Long, HallazgoAuditoria> hallazgos = hallazgoRepository
                .buscar(FiltroHallazgos.todos(criterios.organizacionId())).stream()
                .collect(Collectors.toMap(HallazgoAuditoria::id, Function.identity(), (a, b) -> a));
        return repository.buscar(criterios).stream()
                .map(alerta -> new AlertaPriorizada(alerta, dispositivos.get(alerta.dispositivoId()),
                        hallazgos.get(alerta.hallazgoId())))
                .filter(priorizada -> sedeId == null
                        || (priorizada.dispositivo() != null && sedeId.equals(priorizada.dispositivo().sedeId())))
                .filter(priorizada -> criticidad == null
                        || (priorizada.dispositivo() != null && priorizada.dispositivo().criticidad() == criticidad))
                .sorted(PRIORIDAD)
                .toList();
    }

    /** Detalle de la alerta (GET /api/alerts/{id}). */
    public AlertaDetalle detalle(Long id, Long organizacionId) {
        AlertaCritica alerta = obtener(id, organizacionId);
        return new AlertaDetalle(
                alerta,
                dispositivoRepository.buscarPorId(alerta.dispositivoId()).orElse(null),
                hallazgoRepository.buscarPorId(alerta.hallazgoId()).orElse(null)
        );
    }

    /**
     * Deriva la alerta a un perfil técnico (EN_ATENCION) o la devuelve (ACTIVA). PATCH /api/alerts/{id}/status.
     * El cierre no se hace acá: la alerta se cierra cuando el hallazgo se resuelve o se acepta el riesgo.
     *
     * @param organizacionId null para el administrador
     */
    @Transactional
    public AlertaDetalle cambiarEstado(Long id, Long organizacionId, String estado, String perfil,
                                       String comentario, String actor) {
        AlertaCritica anterior = obtener(id, organizacionId);
        EstadoAlerta nuevoEstado = parsear(EstadoAlerta.class, estado, "estado",
                "El estado debe ser EN_ATENCION (derivar) o ACTIVA (devolver).");
        String comentarioLimpio = comentario == null || comentario.isBlank() ? null : comentario.trim();
        String usuario = recortar(actorSeguro(actor), 120);

        if (comentarioLimpio != null && comentarioLimpio.length() > 1000) {
            throw new IllegalArgumentException("El comentario no puede superar los 1000 caracteres.");
        }
        if (nuevoEstado == EstadoAlerta.CERRADA) {
            throw new IllegalStateException("Una alerta se cierra cuando su hallazgo se resuelve o se acepta el "
                    + "riesgo. Hacelo desde la pantalla de hallazgos.");
        }
        if (anterior.estado() == EstadoAlerta.CERRADA) {
            throw new IllegalStateException("La alerta está cerrada porque su hallazgo ya no está pendiente. "
                    + "Para reactivarla, reabrí el hallazgo.");
        }

        PerfilTecnico perfilNuevo = null;
        if (nuevoEstado == EstadoAlerta.EN_ATENCION) {
            if (perfil == null || perfil.isBlank()) {
                throw new IllegalArgumentException("Indicá a qué perfil se deriva: ANALISTA_RED o AUDITOR_TECNICO.");
            }
            perfilNuevo = parsear(PerfilTecnico.class, perfil, "perfil",
                    "El perfil debe ser ANALISTA_RED o AUDITOR_TECNICO.");
            if (anterior.estado() == EstadoAlerta.EN_ATENCION && anterior.perfilAsignado() == perfilNuevo) {
                throw new IllegalStateException("La alerta ya está en atención por " + perfilNuevo + ".");
            }
        } else {
            if (anterior.estado() == EstadoAlerta.ACTIVA) {
                throw new IllegalStateException("La alerta ya está ACTIVA.");
            }
            if (comentarioLimpio == null) {
                throw new IllegalArgumentException("Para devolver la alerta a ACTIVA hay que indicar el motivo.");
            }
        }

        AlertaCritica actualizada = repository.actualizarEstado(anterior.id(), anterior.estado(), nuevoEstado,
                perfilNuevo, comentarioLimpio, usuario, Instant.now());
        registrarCambio(anterior, actualizada, usuario, comentarioLimpio);
        return new AlertaDetalle(
                actualizada,
                dispositivoRepository.buscarPorId(actualizada.dispositivoId()).orElse(null),
                hallazgoRepository.buscarPorId(actualizada.hallazgoId()).orElse(null)
        );
    }

    static final Comparator<AlertaPriorizada> PRIORIDAD = Comparator
            .comparing((AlertaPriorizada a) -> a.alerta().estado().vigente() ? 0 : 1)
            .thenComparing((AlertaPriorizada a) -> -a.alerta().severidad().ordinal())
            .thenComparing((AlertaPriorizada a) -> a.dispositivo() == null || a.dispositivo().criticidad() == null
                    ? 1 : -a.dispositivo().criticidad().ordinal())
            .thenComparing((AlertaPriorizada a) -> a.alerta().fechaGeneracion(),
                    Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing((AlertaPriorizada a) -> a.alerta().id(), Comparator.nullsLast(Comparator.reverseOrder()));

    private void registrarCambio(AlertaCritica anterior, AlertaCritica actualizada, String usuario, String detalle) {
        String transicion = anterior.estado() + " -> " + actualizada.estado()
                + (actualizada.perfilAsignado() != null && actualizada.estado() == EstadoAlerta.EN_ATENCION
                ? " (" + actualizada.perfilAsignado() + ")" : "");
        trazabilidad.registrar(new Transaccion(ENTIDAD, actualizada.id(), OperacionTransaccion.CAMBIO_ESTADO,
                actualizada.organizacionId(), anterior, actualizada, usuario,
                recortar(transicion + (detalle == null ? "" : ": " + detalle), 1000)));
    }

    private AlertaCritica obtener(Long id, Long organizacionId) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("El campo id debe ser un identificador válido.");
        }
        return (organizacionId == null
                ? repository.buscarPorId(id)
                : repository.buscarPorIdYOrganizacion(id, organizacionId))
                .orElseThrow(() -> new NoSuchElementException("No existe la alerta solicitada."));
    }

    private Map<Long, DispositivoRed> dispositivos(Long organizacionId) {
        List<DispositivoRed> lista = organizacionId == null
                ? dispositivoRepository.listar()
                : dispositivoRepository.listarPorOrganizacion(organizacionId);
        return lista.stream().filter(Objects::nonNull)
                .collect(Collectors.toMap(DispositivoRed::id, Function.identity(), (a, b) -> a));
    }

    private static <E extends Enum<E>> E parsear(Class<E> tipo, String valor, String campo, String mensaje) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("El campo " + campo + " es obligatorio.");
        }
        try {
            return Enum.valueOf(tipo, valor.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(mensaje);
        }
    }

    private static String actorSeguro(String actor) {
        return actor == null || actor.isBlank() ? "SISTEMA_O_ANONIMO" : actor;
    }

    private static String recortar(String valor, int maximo) {
        return valor.length() <= maximo ? valor : valor.substring(0, maximo);
    }
}
