package ar.edu.uai.tfi.auditcore.application.review;

import ar.edu.uai.tfi.auditcore.application.audit.AuditoriaService;
import ar.edu.uai.tfi.auditcore.application.plan.RestriccionPlanException;
import ar.edu.uai.tfi.auditcore.application.port.SeguimientoRevisionPort;
import ar.edu.uai.tfi.auditcore.application.port.TrazabilidadPort;
import ar.edu.uai.tfi.auditcore.domain.model.AuditoriaConfiguracion;
import ar.edu.uai.tfi.auditcore.domain.model.ConfiguracionDispositivo;
import ar.edu.uai.tfi.auditcore.domain.model.DispositivoRed;
import ar.edu.uai.tfi.auditcore.domain.model.EstadoDispositivo;
import ar.edu.uai.tfi.auditcore.domain.model.FiltroHallazgos;
import ar.edu.uai.tfi.auditcore.domain.model.OperacionTransaccion;
import ar.edu.uai.tfi.auditcore.domain.model.PoliticaRevision;
import ar.edu.uai.tfi.auditcore.domain.model.Transaccion;
import ar.edu.uai.tfi.auditcore.domain.repository.AuditoriaRepository;
import ar.edu.uai.tfi.auditcore.domain.repository.ConfiguracionRepository;
import ar.edu.uai.tfi.auditcore.domain.repository.DispositivoRepository;
import ar.edu.uai.tfi.auditcore.domain.repository.HallazgoRepository;
import ar.edu.uai.tfi.auditcore.domain.repository.OrganizacionRepository;
import ar.edu.uai.tfi.auditcore.domain.repository.PoliticaRevisionRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Revisión periódica (la dispara un trabajo programado una vez por día, o el administrador). Por cada organización
 * activa: vuelve a auditar la última configuración de los equipos cuya última auditoría superó la frecuencia de la
 * política, y después avisa vencimientos, configuraciones desactualizadas y el resumen mensual. Ejecutarla dos veces
 * el mismo día no repite auditorías ni avisos.
 */
@ApplicationScoped
public class RevisionPeriodicaService {

    public static final String ACTOR = "revision-periodica";
    static final String ENTIDAD = "REVISION_PERIODICA";
    static final String ENTIDAD_POLITICA = "POLITICA_REVISION";
    static final int MAXIMO_OMISIONES = 20;

    private final SeguimientoRevisionPort seguimiento;
    private final PoliticaRevisionRepository politicas;
    private final OrganizacionRepository organizaciones;
    private final DispositivoRepository dispositivos;
    private final ConfiguracionRepository configuraciones;
    private final AuditoriaRepository auditorias;
    private final HallazgoRepository hallazgos;
    private final AuditoriaService auditoriaService;
    private final AvisosRevisionService avisosRevision;
    private final TrazabilidadPort trazabilidad;
    Clock reloj = Clock.systemUTC();

    public RevisionPeriodicaService(SeguimientoRevisionPort seguimiento, PoliticaRevisionRepository politicas,
                                    OrganizacionRepository organizaciones, DispositivoRepository dispositivos,
                                    ConfiguracionRepository configuraciones, AuditoriaRepository auditorias,
                                    HallazgoRepository hallazgos, AuditoriaService auditoriaService,
                                    AvisosRevisionService avisosRevision, TrazabilidadPort trazabilidad) {
        this.seguimiento = seguimiento;
        this.politicas = politicas;
        this.organizaciones = organizaciones;
        this.dispositivos = dispositivos;
        this.configuraciones = configuraciones;
        this.auditorias = auditorias;
        this.hallazgos = hallazgos;
        this.auditoriaService = auditoriaService;
        this.avisosRevision = avisosRevision;
        this.trazabilidad = trazabilidad;
    }

    /**
     * @param omitidas lo que no se pudo hacer (equipos sin baseline, plan vencido...), con el motivo
     * @param correos  avisos encolados
     */
    public record Resultado(Instant ejecutada, int organizaciones, int reauditorias, int hallazgosVencidos,
                            int dispositivosDesactualizados, int correos, int resumenes, List<String> omitidas) {
    }

    /** Situación actual para el tablero. {@code politica} es null cuando abarca todas las organizaciones. */
    public record Estado(PoliticaRevision politica, long hallazgosVencidos, long dispositivosDesactualizados) {
    }

    /** No es transaccional: cada auditoría y los avisos de cada organización se confirman por separado. */
    public Resultado ejecutar(String actor) {
        Instant ahora = reloj.instant();
        String quien = actor == null || actor.isBlank() ? ACTOR : actor;
        List<Long> activas = seguimiento.organizacionesActivas();
        List<String> omitidas = new ArrayList<>();
        int reauditorias = 0;
        int vencidos = 0;
        int desactualizados = 0;
        int correos = 0;
        int resumenes = 0;
        for (Long organizacionId : activas) {
            PoliticaRevision politica = politicas.obtener(organizacionId);
            if (politica.reauditoriaAutomatica()) {
                reauditorias += reauditar(organizacionId, politica, ahora, quien, omitidas);
            }
            try {
                AvisosRevisionService.ResultadoOrganizacion avisos =
                        avisosRevision.procesar(organizacionId, politica, ahora);
                vencidos += avisos.hallazgosVencidos();
                desactualizados += avisos.dispositivosDesactualizados();
                correos += avisos.destinatarios();
                resumenes += avisos.resumen() ? 1 : 0;
            } catch (RuntimeException exception) {
                // Una organización con problemas no frena la revisión de las demás; se reintenta mañana.
                omitidas.add("Avisos de la organización " + organizacionId + ": " + exception.getMessage());
            }
        }
        List<String> detalleOmitidas = omitidas.size() <= MAXIMO_OMISIONES ? omitidas
                : List.copyOf(omitidas.subList(0, MAXIMO_OMISIONES));
        Resultado resultado = new Resultado(ahora, activas.size(), reauditorias, vencidos, desactualizados, correos,
                resumenes, detalleOmitidas);
        trazabilidad.registrar(new Transaccion(ENTIDAD, null, OperacionTransaccion.EJECUCION, null, null, resultado,
                quien, "organizaciones=" + activas.size() + ", reauditorias=" + reauditorias
                + ", omitidas=" + omitidas.size() + ", vencidos=" + vencidos
                + ", desactualizados=" + desactualizados + ", correos=" + correos + ", resumenes=" + resumenes));
        return resultado;
    }

    private int reauditar(Long organizacionId, PoliticaRevision politica, Instant ahora, String actor,
                          List<String> omitidas) {
        Instant umbral = ahora.minus(Duration.ofDays(politica.diasReauditoria()));
        Map<Long, Instant> ultimaAuditoria = new HashMap<>();
        for (AuditoriaConfiguracion auditoria : auditorias.listarHistorialPorOrganizacion(organizacionId)) {
            ultimaAuditoria.merge(auditoria.dispositivoId(), auditoria.fechaEjecucion(), (a, b) -> a.isAfter(b) ? a : b);
        }
        Map<Long, ConfiguracionDispositivo> ultimaConfiguracion = ultimasConfiguraciones(organizacionId);
        List<DispositivoRed> pendientes = dispositivos.listarPorOrganizacion(organizacionId).stream()
                .filter(dispositivo -> dispositivo.estado() == EstadoDispositivo.ACTIVO)
                .filter(dispositivo -> ultimaAuditoria.containsKey(dispositivo.id())
                        && ultimaAuditoria.get(dispositivo.id()).isBefore(umbral))
                .filter(dispositivo -> ultimaConfiguracion.containsKey(dispositivo.id()))
                .sorted(Comparator.comparing(DispositivoRed::id))
                .toList();

        int ejecutadas = 0;
        for (DispositivoRed dispositivo : pendientes) {
            try {
                if (auditoriaService.ejecutarAutomatica(ultimaConfiguracion.get(dispositivo.id()).id(), umbral, actor)
                        .isPresent()) {
                    ejecutadas++;
                }
            } catch (RestriccionPlanException exception) {
                // Con el plan vencido no se audita ningún equipo de la organización.
                omitidas.add(dispositivo.identificador() + ": " + exception.getMessage());
                break;
            } catch (RuntimeException exception) {
                omitidas.add(dispositivo.identificador() + ": " + exception.getMessage());
            }
        }
        return ejecutadas;
    }

    /** Última versión de configuración de cada dispositivo. */
    private Map<Long, ConfiguracionDispositivo> ultimasConfiguraciones(Long organizacionId) {
        Map<Long, ConfiguracionDispositivo> ultimas = new HashMap<>();
        Comparator<ConfiguracionDispositivo> masNueva = Comparator
                .comparing(ConfiguracionDispositivo::version, Comparator.nullsFirst(Comparator.naturalOrder()))
                .thenComparing(ConfiguracionDispositivo::id);
        for (ConfiguracionDispositivo configuracion : configuraciones.listarAuditablesPorOrganizacion(organizacionId)) {
            ultimas.merge(configuracion.dispositivoId(), configuracion,
                    (a, b) -> masNueva.compare(a, b) >= 0 ? a : b);
        }
        return ultimas;
    }

    /** @param organizacionId null para el administrador: suma todas las organizaciones activas */
    public Estado estado(Long organizacionId) {
        Instant ahora = reloj.instant();
        if (organizacionId != null) {
            PoliticaRevision politica = politicas.obtener(organizacionId);
            return new Estado(politica, vencidos(organizacionId, politica, ahora),
                    desactualizados(organizacionId, politica, ahora));
        }
        long vencidos = 0;
        long desactualizados = 0;
        for (Long id : seguimiento.organizacionesActivas()) {
            PoliticaRevision politica = politicas.obtener(id);
            vencidos += vencidos(id, politica, ahora);
            desactualizados += desactualizados(id, politica, ahora);
        }
        return new Estado(null, vencidos, desactualizados);
    }

    private long vencidos(Long organizacionId, PoliticaRevision politica, Instant ahora) {
        return hallazgos.buscar(FiltroHallazgos.todos(organizacionId)).stream()
                .filter(hallazgo -> politica.vencido(hallazgo, ahora))
                .count();
    }

    private long desactualizados(Long organizacionId, PoliticaRevision politica, Instant ahora) {
        Instant limite = ahora.minus(Duration.ofDays(politica.diasConfiguracion()));
        Map<Long, ConfiguracionDispositivo> ultimas = ultimasConfiguraciones(organizacionId);
        return dispositivos.listarPorOrganizacion(organizacionId).stream()
                .filter(dispositivo -> dispositivo.estado() == EstadoDispositivo.ACTIVO)
                .map(dispositivo -> ultimas.get(dispositivo.id()))
                .filter(configuracion -> configuracion != null && configuracion.fechaImportacion() != null
                        && configuracion.fechaImportacion().isBefore(limite))
                .count();
    }

    public PoliticaRevision politica(Long organizacionId) {
        verificarOrganizacion(organizacionId);
        return politicas.obtener(organizacionId);
    }

    @Transactional
    public PoliticaRevision guardarPolitica(Long organizacionId, PoliticaRevision cambios, String actor) {
        verificarOrganizacion(organizacionId);
        if (cambios == null) {
            throw new IllegalArgumentException("El cuerpo de la solicitud es obligatorio.");
        }
        PoliticaRevision nueva = new PoliticaRevision(organizacionId, cambios.diasCritica(), cambios.diasAlta(),
                cambios.diasMedia(), cambios.diasBaja(), cambios.diasReauditoria(), cambios.diasConfiguracion(),
                cambios.reauditoriaAutomatica(), reloj.instant(), actor == null || actor.isBlank() ? ACTOR : actor);
        nueva.validar();
        PoliticaRevision anterior = politicas.obtener(organizacionId);
        PoliticaRevision guardada = politicas.guardar(nueva);
        trazabilidad.registrar(new Transaccion(ENTIDAD_POLITICA, organizacionId, OperacionTransaccion.MODIFICACION,
                organizacionId, anterior, guardada, actor, "Plazos de revisión"));
        return guardada;
    }

    private void verificarOrganizacion(Long organizacionId) {
        if (organizacionId == null || organizacionId <= 0) {
            throw new IllegalArgumentException("El campo organizationId debe ser un identificador válido.");
        }
        if (!organizaciones.existe(organizacionId)) {
            throw new NoSuchElementException("No existe la organización cliente solicitada.");
        }
    }
}
