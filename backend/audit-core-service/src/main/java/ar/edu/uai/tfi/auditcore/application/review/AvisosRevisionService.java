package ar.edu.uai.tfi.auditcore.application.review;

import ar.edu.uai.tfi.auditcore.application.dashboard.EvaluadorRiesgo;
import ar.edu.uai.tfi.auditcore.application.port.AvisoCorreoPort;
import ar.edu.uai.tfi.auditcore.application.port.SeguimientoRevisionPort;
import ar.edu.uai.tfi.auditcore.domain.model.AlertaCritica;
import ar.edu.uai.tfi.auditcore.domain.model.AuditoriaConfiguracion;
import ar.edu.uai.tfi.auditcore.domain.model.ConfiguracionDispositivo;
import ar.edu.uai.tfi.auditcore.domain.model.DispositivoRed;
import ar.edu.uai.tfi.auditcore.domain.model.EstadoDispositivo;
import ar.edu.uai.tfi.auditcore.domain.model.FiltroAlertas;
import ar.edu.uai.tfi.auditcore.domain.model.FiltroHallazgos;
import ar.edu.uai.tfi.auditcore.domain.model.HallazgoAuditoria;
import ar.edu.uai.tfi.auditcore.domain.model.PoliticaRevision;
import ar.edu.uai.tfi.auditcore.domain.repository.AlertaRepository;
import ar.edu.uai.tfi.auditcore.domain.repository.AuditoriaRepository;
import ar.edu.uai.tfi.auditcore.domain.repository.ConfiguracionRepository;
import ar.edu.uai.tfi.auditcore.domain.repository.DispositivoRepository;
import ar.edu.uai.tfi.auditcore.domain.repository.HallazgoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Avisos de la revisión periódica de una organización, en una transacción con la organización bloqueada: hallazgos
 * que superaron su plazo, dispositivos sin configuración nueva y el resumen del mes anterior. Cada cosa se avisa una
 * sola vez.
 */
@ApplicationScoped
public class AvisosRevisionService {

    static final ZoneId ZONA = ZoneId.of("America/Argentina/Buenos_Aires");
    /** Elementos que se detallan en el correo; el resto se ve en la consola. */
    static final int ITEMS_EN_AVISO = 15;
    private static final DateTimeFormatter MES = DateTimeFormatter.ofPattern("MMMM 'de' yyyy",
            Locale.forLanguageTag("es-AR"));

    private final SeguimientoRevisionPort seguimiento;
    private final HallazgoRepository hallazgos;
    private final DispositivoRepository dispositivos;
    private final ConfiguracionRepository configuraciones;
    private final AuditoriaRepository auditorias;
    private final AlertaRepository alertas;
    private final AvisoCorreoPort avisos;

    public AvisosRevisionService(SeguimientoRevisionPort seguimiento, HallazgoRepository hallazgos,
                                 DispositivoRepository dispositivos, ConfiguracionRepository configuraciones,
                                 AuditoriaRepository auditorias, AlertaRepository alertas, AvisoCorreoPort avisos) {
        this.seguimiento = seguimiento;
        this.hallazgos = hallazgos;
        this.dispositivos = dispositivos;
        this.configuraciones = configuraciones;
        this.auditorias = auditorias;
        this.alertas = alertas;
        this.avisos = avisos;
    }

    /**
     * @param hallazgosVencidos           problemas (dispositivo y regla) avisados por primera vez
     * @param dispositivosDesactualizados dispositivos avisados por primera vez
     * @param destinatarios               correos encolados
     */
    public record ResultadoOrganizacion(int hallazgosVencidos, int dispositivosDesactualizados, int destinatarios,
                                        boolean resumen) {
    }

    @Transactional
    public ResultadoOrganizacion procesar(Long organizacionId, PoliticaRevision politica, Instant ahora) {
        seguimiento.bloquearOrganizacion(organizacionId);
        Map<Long, DispositivoRed> porId = dispositivos.listarPorOrganizacion(organizacionId).stream()
                .collect(Collectors.toMap(DispositivoRed::id, Function.identity(), (a, b) -> a));
        List<HallazgoAuditoria> pendientes = hallazgos.buscar(FiltroHallazgos.todos(organizacionId)).stream()
                .filter(hallazgo -> hallazgo.estado().pendiente())
                .toList();

        Set<Long> avisados = seguimiento.hallazgosAvisados(organizacionId);
        List<HallazgoAuditoria> vencidos = pendientes.stream()
                .filter(hallazgo -> politica.vencido(hallazgo, ahora))
                .filter(hallazgo -> !avisados.contains(hallazgo.id()))
                .sorted(Comparator.comparing(HallazgoAuditoria::fechaDeteccion).thenComparing(HallazgoAuditoria::id))
                .toList();
        // El mismo problema repetido en varias auditorías se avisa una vez, por el hallazgo más viejo.
        Map<String, HallazgoAuditoria> problemas = new LinkedHashMap<>();
        vencidos.forEach(hallazgo -> problemas.putIfAbsent(hallazgo.dispositivoId() + "|" + hallazgo.codigoRegla(),
                hallazgo));

        List<Desactualizado> desactualizados = desactualizados(organizacionId, politica, porId, ahora);

        int destinatarios = 0;
        if (!problemas.isEmpty() || !desactualizados.isEmpty()) {
            destinatarios += avisos.avisar(AvisoCorreoPort.Tipo.VENCIMIENTOS, organizacionId,
                    datosVencimientos(problemas.values().stream().toList(), desactualizados, politica, porId, ahora));
            seguimiento.marcarHallazgosAvisados(vencidos.stream().map(HallazgoAuditoria::id).toList(), ahora);
            seguimiento.marcarDispositivosAvisados(
                    desactualizados.stream().map(d -> d.dispositivo().id()).toList(), ahora);
        }

        int delResumen = resumenMensual(organizacionId, politica, porId, pendientes, ahora);
        return new ResultadoOrganizacion(problemas.size(), desactualizados.size(), destinatarios + delResumen,
                delResumen > 0);
    }

    private record Desactualizado(DispositivoRed dispositivo, Instant ultimaConfiguracion) {
    }

    /** Dispositivos activos cuya última configuración superó el plazo y que no se avisaron desde que se cargó. */
    private List<Desactualizado> desactualizados(Long organizacionId, PoliticaRevision politica,
                                                 Map<Long, DispositivoRed> porId, Instant ahora) {
        Map<Long, Instant> ultimas = ultimasConfiguraciones(organizacionId);
        Map<Long, Instant> avisos = seguimiento.avisosConfiguracion(organizacionId);
        Instant limite = ahora.minus(Duration.ofDays(politica.diasConfiguracion()));
        return porId.values().stream()
                .filter(dispositivo -> dispositivo.estado() == EstadoDispositivo.ACTIVO)
                .filter(dispositivo -> ultimas.containsKey(dispositivo.id()))
                .filter(dispositivo -> ultimas.get(dispositivo.id()).isBefore(limite))
                .filter(dispositivo -> {
                    Instant avisado = avisos.get(dispositivo.id());
                    return avisado == null || avisado.isBefore(ultimas.get(dispositivo.id()));
                })
                .sorted(Comparator.comparing((DispositivoRed d) -> ultimas.get(d.id())).thenComparing(DispositivoRed::id))
                .map(dispositivo -> new Desactualizado(dispositivo, ultimas.get(dispositivo.id())))
                .toList();
    }

    private Map<Long, Instant> ultimasConfiguraciones(Long organizacionId) {
        Map<Long, Instant> ultimas = new HashMap<>();
        for (ConfiguracionDispositivo configuracion : configuraciones.listarAuditablesPorOrganizacion(organizacionId)) {
            if (configuracion.fechaImportacion() != null) {
                ultimas.merge(configuracion.dispositivoId(), configuracion.fechaImportacion(),
                        (a, b) -> a.isAfter(b) ? a : b);
            }
        }
        return ultimas;
    }

    private Map<String, Object> datosVencimientos(List<HallazgoAuditoria> problemas, List<Desactualizado> desactualizados,
                                                  PoliticaRevision politica, Map<Long, DispositivoRed> porId,
                                                  Instant ahora) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("totalVencidos", problemas.size());
        datos.put("totalDesactualizados", desactualizados.size());
        datos.put("vencidos", problemas.stream().limit(ITEMS_EN_AVISO).map(hallazgo -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("codigo", hallazgo.codigoRegla());
            item.put("nombre", hallazgo.nombreRegla());
            item.put("severidad", hallazgo.severidad().name());
            item.put("dispositivo", identificador(porId.get(hallazgo.dispositivoId()), hallazgo.dispositivoId()));
            item.put("dias", Math.max(1, Duration.between(politica.vence(hallazgo), ahora).toDays()));
            return item;
        }).toList());
        datos.put("desactualizados", desactualizados.stream().limit(ITEMS_EN_AVISO).map(desactualizado -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("identificador", desactualizado.dispositivo().identificador());
            item.put("nombre", desactualizado.dispositivo().nombre());
            item.put("dias", Duration.between(desactualizado.ultimaConfiguracion(), ahora).toDays());
            return item;
        }).toList());
        return datos;
    }

    /**
     * Resumen del mes anterior (en hora de Argentina), una vez por organización. Sin actividad en el mes no se envía,
     * pero queda registrado para no recalcularlo.
     *
     * @return destinatarios
     */
    private int resumenMensual(Long organizacionId, PoliticaRevision politica, Map<Long, DispositivoRed> porId,
                               List<HallazgoAuditoria> pendientes, Instant ahora) {
        YearMonth mes = YearMonth.from(ahora.atZone(ZONA)).minusMonths(1);
        String periodo = mes.toString();
        if (seguimiento.resumenEnviado(organizacionId, periodo)) {
            return 0;
        }
        Instant desde = mes.atDay(1).atStartOfDay(ZONA).toInstant();
        Instant hasta = mes.plusMonths(1).atDay(1).atStartOfDay(ZONA).toInstant();

        List<AuditoriaConfiguracion> historial = auditorias.listarHistorialPorOrganizacion(organizacionId);
        long ejecutadas = historial.stream().filter(a -> entre(a.fechaEjecucion(), desde, hasta)).count();
        List<HallazgoAuditoria> todos = hallazgos.buscar(FiltroHallazgos.todos(organizacionId));
        long detectados = todos.stream().filter(h -> entre(h.fechaDeteccion(), desde, hasta)).count();
        long cerrados = todos.stream()
                .filter(h -> !h.estado().pendiente() && entre(h.fechaEstado(), desde, hasta))
                .count();

        int destinatarios = 0;
        if (ejecutadas > 0 || detectados > 0 || cerrados > 0) {
            List<AlertaCritica> vigentes = alertas.buscar(FiltroAlertas.vigentes(organizacionId));
            Map<String, Object> datos = new LinkedHashMap<>();
            datos.put("periodo", MES.format(mes));
            datos.put("auditorias", ejecutadas);
            datos.put("detectados", detectados);
            datos.put("cerrados", cerrados);
            datos.put("pendientes", pendientes.size());
            datos.put("vencidos", pendientes.stream().filter(h -> politica.vencido(h, ahora)).count());
            datos.put("alertasVigentes", vigentes.size());
            datos.put("nivelRiesgo", EvaluadorRiesgo.evaluar(!historial.isEmpty(), pendientes, vigentes, porId).name());
            destinatarios = avisos.avisar(AvisoCorreoPort.Tipo.RESUMEN_MENSUAL, organizacionId, datos);
        }
        seguimiento.registrarResumen(organizacionId, periodo, ahora, destinatarios);
        return destinatarios;
    }

    private static boolean entre(Instant fecha, Instant desde, Instant hasta) {
        return fecha != null && !fecha.isBefore(desde) && fecha.isBefore(hasta);
    }

    private static String identificador(DispositivoRed dispositivo, Long id) {
        return dispositivo == null ? "dispositivo " + id : dispositivo.identificador();
    }
}
