package ar.edu.uai.tfi.management.application.plan;

import ar.edu.uai.tfi.management.application.ErrorAplicacion;
import ar.edu.uai.tfi.management.application.ExcepcionAplicacion;
import ar.edu.uai.tfi.management.application.correo.BandejaCorreo;
import ar.edu.uai.tfi.management.application.port.TrazabilidadPort;
import ar.edu.uai.tfi.management.application.port.UsoDispositivosPort;
import ar.edu.uai.tfi.management.application.user.MatrizPermisos;
import ar.edu.uai.tfi.management.domain.model.EstadoRegistro;
import ar.edu.uai.tfi.management.domain.model.EstadoSolicitudPlan;
import ar.edu.uai.tfi.management.domain.model.ModalidadContratacion;
import ar.edu.uai.tfi.management.domain.model.OperacionTransaccion;
import ar.edu.uai.tfi.management.domain.model.OrganizacionCliente;
import ar.edu.uai.tfi.management.domain.model.Plan;
import ar.edu.uai.tfi.management.domain.model.PlanOrganizacion;
import ar.edu.uai.tfi.management.domain.model.SolicitudCambioPlan;
import ar.edu.uai.tfi.management.domain.model.TipoCorreo;
import ar.edu.uai.tfi.management.domain.model.Transaccion;
import ar.edu.uai.tfi.management.domain.repository.OrganizacionClienteRepository;
import ar.edu.uai.tfi.management.domain.repository.PlanRepository;
import ar.edu.uai.tfi.management.domain.repository.SolicitudPlanRepository;
import ar.edu.uai.tfi.management.domain.repository.UsuarioRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Planes comerciales: catálogo, plan de cada organización, solicitudes de cambio y control de usuarios activos.
 * Los dispositivos los controla audit-core-service con el mismo plan. No hay cobros: el precio es de referencia.
 */
@ApplicationScoped
public class PlanService implements ControlPlan {

    static final String ENTIDAD_PLAN = "PLAN";
    static final String ENTIDAD_SOLICITUD = "SOLICITUD_PLAN";
    static final String ENTIDAD_CATALOGO = "CATALOGO_PLAN";
    static final int DIAS_PRUEBA_MINIMO = 60;
    private static final int LIMITE_MAXIMO = 100_000;
    private static final BigDecimal PRECIO_MAXIMO = new BigDecimal("99999999.99");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy")
            .withZone(ZoneId.of("America/Argentina/Buenos_Aires"));

    private final PlanRepository planes;
    private final OrganizacionClienteRepository organizaciones;
    private final SolicitudPlanRepository solicitudes;
    private final UsuarioRepository usuarios;
    private final UsoDispositivosPort dispositivos;
    private final TrazabilidadPort trazabilidad;
    private final BandejaCorreo correos;
    Clock reloj = Clock.systemUTC();

    public PlanService(PlanRepository planes,
                       OrganizacionClienteRepository organizaciones,
                       SolicitudPlanRepository solicitudes,
                       UsuarioRepository usuarios,
                       UsoDispositivosPort dispositivos,
                       TrazabilidadPort trazabilidad,
                       BandejaCorreo correos) {
        this.planes = planes;
        this.organizaciones = organizaciones;
        this.solicitudes = solicitudes;
        this.usuarios = usuarios;
        this.dispositivos = dispositivos;
        this.trazabilidad = trazabilidad;
        this.correos = correos;
    }

    /**
     * Plan, uso y vencimiento de una organización.
     *
     * @param diasRestantes días hasta el vencimiento (redondeado hacia arriba); null si el plan no vence
     * @param pendiente     solicitud de cambio sin resolver; null si no hay
     */
    public record EstadoPlan(PlanOrganizacion plan, long dispositivosActivos, long usuariosActivos, boolean vencido,
                             Long diasRestantes, SolicitudCambioPlan pendiente) {
    }

    public List<Plan> catalogo() {
        return planes.listar();
    }

    /** Precio de referencia y condiciones: la carpeta los revisa cada año, por eso no están fijos en el código. */
    @Transactional
    public Plan actualizarCatalogo(String codigo, BigDecimal precioMensualUsd, String condiciones) {
        Plan anterior = buscarPlan(codigo);
        if (precioMensualUsd != null && (precioMensualUsd.signum() < 0 || precioMensualUsd.scale() > 2
                || precioMensualUsd.compareTo(PRECIO_MAXIMO) > 0)) {
            throw invalida("El precio debe ser un importe positivo con hasta dos decimales.");
        }
        Plan actualizado = planes.actualizar(new Plan(anterior.id(), anterior.codigo(), anterior.nombre(),
                anterior.maxDispositivos(), anterior.maxUsuarios(), anterior.diasVigencia(),
                precioMensualUsd == null ? null : precioMensualUsd.setScale(2),
                textoOpcional(condiciones, 400, "El texto de las condiciones"), anterior.orden()));
        trazabilidad.registrar(new Transaccion(ENTIDAD_CATALOGO, anterior.id(), OperacionTransaccion.MODIFICACION,
                null, anterior, actualizado, null, "plan=" + anterior.codigo()));
        return actualizado;
    }

    public EstadoPlan estado(Long organizacionId) {
        PlanOrganizacion plan = organizaciones.buscarPlan(organizacionId)
                .orElseThrow(() -> new ExcepcionAplicacion(ErrorAplicacion.NO_ENCONTRADO,
                        "La organización cliente no fue encontrada."));
        Instant ahora = reloj.instant();
        Long diasRestantes = null;
        if (plan.vence() != null) {
            long segundos = Math.max(0, Duration.between(ahora, plan.vence()).toSeconds());
            diasRestantes = (segundos + 86_399) / 86_400;
        }
        return new EstadoPlan(plan, dispositivos.dispositivosActivos(organizacionId), usuariosActivos(organizacionId),
                plan.vencido(ahora), diasRestantes, solicitudes.buscarPendiente(organizacionId).orElse(null));
    }

    /** Plan con el que empieza una organización nueva (todavía sin dispositivos ni usuarios). */
    public PlanOrganizacion planInicial(String codigo, Integer limiteDispositivos, Integer limiteUsuarios,
                                        Integer diasPrueba) {
        return armar(null, buscarPlan(codigo), limiteDispositivos, limiteUsuarios, diasPrueba);
    }

    /** Asignación directa del administrador del sistema (también renueva un período de prueba). */
    @Transactional
    public EstadoPlan asignar(Long organizacionId, String codigo, Integer limiteDispositivos, Integer limiteUsuarios,
                              Integer diasPrueba) {
        aplicar(organizacionId, buscarPlan(codigo), limiteDispositivos, limiteUsuarios, diasPrueba);
        return estado(organizacionId);
    }

    @Transactional
    public SolicitudCambioPlan solicitar(Long organizacionId, String codigo, String modalidad, String comentario,
                                         String usuario) {
        OrganizacionCliente organizacion = buscarOrganizacion(organizacionId);
        if (organizacion.estado() != EstadoRegistro.ACTIVO) {
            throw conflicto("La organización está deshabilitada.");
        }
        PlanOrganizacion actual = planParaActualizar(organizacionId);
        Plan pedido = buscarPlan(codigo);
        if (pedido.conVencimiento()) {
            throw invalida("El plan " + pedido.nombre() + " lo asigna el administrador del sistema: elegí otro plan.");
        }
        if (pedido.codigo().equals(actual.plan().codigo()) && !pedido.aMedida()) {
            throw conflicto("La organización ya tiene el plan " + pedido.nombre() + ".");
        }
        if (solicitudes.buscarPendiente(organizacionId).isPresent()) {
            throw conflicto("Ya hay una solicitud pendiente: esperá la respuesta o cancelala para pedir otro plan.");
        }
        if (!pedido.aMedida()) {
            verificarUso(organizacionId, new PlanOrganizacion(organizacionId, pedido, null, null, null, null));
        }
        SolicitudCambioPlan guardada = solicitudes.guardar(new SolicitudCambioPlan(null, organizacionId,
                actual.plan().codigo(), pedido.codigo(), parsearModalidad(modalidad), EstadoSolicitudPlan.PENDIENTE,
                textoOpcional(comentario, 500, "El comentario"), usuario, reloj.instant(), null, null, null));
        trazabilidad.registrar(Transaccion.alta(ENTIDAD_SOLICITUD, guardada.id(), organizacionId, guardada));
        avisarAdministradores(guardada, actual.plan(), pedido);
        return guardada;
    }

    @Transactional
    public SolicitudCambioPlan cancelar(Long organizacionId, Long solicitudId, String usuario) {
        SolicitudCambioPlan solicitud = solicitudPendiente(solicitudId);
        if (!solicitud.organizacionId().equals(organizacionId)) {
            throw new ExcepcionAplicacion(ErrorAplicacion.NO_ENCONTRADO, "La solicitud no fue encontrada.");
        }
        return resolver(solicitud, EstadoSolicitudPlan.CANCELADA, usuario, null);
    }

    /** Aplica el plan pedido; los límites solo se indican para un plan a medida. */
    @Transactional
    public SolicitudCambioPlan aprobar(Long solicitudId, Integer limiteDispositivos, Integer limiteUsuarios,
                                       String respuesta, String usuario) {
        SolicitudCambioPlan solicitud = solicitudPendiente(solicitudId);
        aplicar(solicitud.organizacionId(), buscarPlan(solicitud.planSolicitado()), limiteDispositivos,
                limiteUsuarios, null);
        SolicitudCambioPlan aprobada = resolver(solicitud, EstadoSolicitudPlan.APROBADA, usuario,
                textoOpcional(respuesta, 500, "La respuesta"));
        avisarSolicitante(aprobada);
        return aprobada;
    }

    @Transactional
    public SolicitudCambioPlan rechazar(Long solicitudId, String respuesta, String usuario) {
        SolicitudCambioPlan solicitud = solicitudPendiente(solicitudId);
        String motivo = textoOpcional(respuesta, 500, "La respuesta");
        if (motivo == null) {
            throw invalida("Indicá el motivo del rechazo: lo ve el cliente.");
        }
        SolicitudCambioPlan rechazada = resolver(solicitud, EstadoSolicitudPlan.RECHAZADA, usuario, motivo);
        avisarSolicitante(rechazada);
        return rechazada;
    }

    /** Solicitudes de todas las organizaciones; con estado vacío devuelve todas. */
    public List<SolicitudCambioPlan> solicitudes(String estado) {
        if (estado == null || estado.isBlank()) {
            return solicitudes.listarPorEstado(null);
        }
        try {
            return solicitudes.listarPorEstado(EstadoSolicitudPlan.valueOf(estado.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException exception) {
            throw invalida("El estado debe ser PENDIENTE, APROBADA, RECHAZADA o CANCELADA.");
        }
    }

    public List<SolicitudCambioPlan> solicitudesDe(Long organizacionId) {
        buscarOrganizacion(organizacionId);
        return solicitudes.listarPorOrganizacion(organizacionId);
    }

    @Override
    public void verificarAltaUsuario(Long organizacionId) {
        PlanOrganizacion plan = organizaciones.buscarPlanParaActualizar(organizacionId).orElse(null);
        if (plan == null) {
            return;
        }
        verificarVigente(plan);
        Integer maximo = plan.maxUsuarios();
        long activos = usuariosActivos(organizacionId);
        if (maximo != null && activos >= maximo) {
            throw conflicto("El plan " + plan.plan().nombre() + " permite hasta " + maximo
                    + " usuarios activos y la organización ya tiene " + activos
                    + ". Para sumar más, pedí un cambio de plan desde Mi plan.");
        }
    }

    private void aplicar(Long organizacionId, Plan plan, Integer limiteDispositivos, Integer limiteUsuarios,
                         Integer diasPrueba) {
        PlanOrganizacion anterior = planParaActualizar(organizacionId);
        PlanOrganizacion nuevo = armar(organizacionId, plan, limiteDispositivos, limiteUsuarios, diasPrueba);
        boolean mismoPlan = anterior.plan().codigo().equals(plan.codigo())
                && Objects.equals(anterior.limiteDispositivos(), nuevo.limiteDispositivos())
                && Objects.equals(anterior.limiteUsuarios(), nuevo.limiteUsuarios());
        if (mismoPlan && !plan.conVencimiento()) {
            throw conflicto("La organización ya tiene el plan " + plan.nombre() + " con esos límites.");
        }
        verificarUso(organizacionId, nuevo);
        organizaciones.asignarPlan(organizacionId, nuevo);
        trazabilidad.registrar(new Transaccion(ENTIDAD_PLAN, organizacionId, OperacionTransaccion.MODIFICACION,
                organizacionId, resumen(anterior), resumen(nuevo), null,
                anterior.plan().codigo() + " -> " + nuevo.plan().codigo()));
    }

    private PlanOrganizacion armar(Long organizacionId, Plan plan, Integer limiteDispositivos, Integer limiteUsuarios,
                                   Integer diasPrueba) {
        Instant ahora = reloj.instant();
        Integer dispositivosAcordados = null;
        Integer usuariosAcordados = null;
        if (plan.aMedida()) {
            dispositivosAcordados = validarLimite(limiteDispositivos, "dispositivos", plan);
            usuariosAcordados = validarLimite(limiteUsuarios, "usuarios", plan);
        }
        Instant vence = null;
        if (plan.conVencimiento()) {
            int dias = diasPrueba == null ? plan.diasVigencia() : diasPrueba;
            if (dias < DIAS_PRUEBA_MINIMO || dias > plan.diasVigencia()) {
                throw invalida("El plan " + plan.nombre() + " dura entre " + DIAS_PRUEBA_MINIMO + " y "
                        + plan.diasVigencia() + " días.");
            }
            vence = ahora.plus(dias, ChronoUnit.DAYS);
        }
        return new PlanOrganizacion(organizacionId, plan, ahora, vence, dispositivosAcordados, usuariosAcordados);
    }

    private static Integer validarLimite(Integer valor, String recurso, Plan plan) {
        if (valor == null || valor < 1 || valor > LIMITE_MAXIMO) {
            throw invalida("El plan " + plan.nombre() + " se acuerda con cada cliente: indicá la cantidad de "
                    + recurso + " (entre 1 y " + LIMITE_MAXIMO + ").");
        }
        return valor;
    }

    /** Bajar de plan no puede dejar a la organización por encima de los límites nuevos. */
    private void verificarUso(Long organizacionId, PlanOrganizacion nuevo) {
        Integer maxDispositivos = nuevo.maxDispositivos();
        long dispositivosActivos = dispositivos.dispositivosActivos(organizacionId);
        if (maxDispositivos != null && dispositivosActivos > maxDispositivos) {
            throw conflicto("La organización tiene " + dispositivosActivos + " dispositivos activos y el plan "
                    + nuevo.plan().nombre() + " permite " + maxDispositivos
                    + ": hay que deshabilitar dispositivos o elegir otro plan.");
        }
        Integer maxUsuarios = nuevo.maxUsuarios();
        long usuariosActivos = usuariosActivos(organizacionId);
        if (maxUsuarios != null && usuariosActivos > maxUsuarios) {
            throw conflicto("La organización tiene " + usuariosActivos + " usuarios activos y el plan "
                    + nuevo.plan().nombre() + " permite " + maxUsuarios
                    + ": hay que deshabilitar usuarios o elegir otro plan.");
        }
    }

    private void verificarVigente(PlanOrganizacion plan) {
        if (plan.vencido(reloj.instant())) {
            throw conflicto("El período de prueba venció el " + FECHA.format(plan.vence())
                    + ": la organización quedó en modo consulta hasta que se apruebe un plan. Pedilo desde Mi plan.");
        }
    }

    /** Los administradores del sistema son de la plataforma: no cuentan para el plan. */
    private long usuariosActivos(Long organizacionId) {
        return usuarios.listar().stream()
                .filter(usuario -> organizacionId.equals(usuario.organizacionId()))
                .filter(usuario -> usuario.estado() == EstadoRegistro.ACTIVO)
                .filter(usuario -> !usuario.roles().contains(MatrizPermisos.ADMIN))
                .count();
    }

    /** Aviso por correo a los administradores del sistema activos, que son quienes la resuelven. */
    private void avisarAdministradores(SolicitudCambioPlan solicitud, Plan actual, Plan pedido) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("planActual", actual.nombre());
        datos.put("planSolicitado", pedido.nombre());
        datos.put("modalidad", solicitud.modalidad().name());
        datos.put("solicitadoPor", solicitud.solicitadoPor());
        if (solicitud.comentario() != null) {
            datos.put("comentario", solicitud.comentario());
        }
        usuarios.listar().stream()
                .filter(usuario -> usuario.estado() == EstadoRegistro.ACTIVO)
                .filter(usuario -> usuario.roles().contains(MatrizPermisos.ADMIN))
                .forEach(admin -> correos.encolar(TipoCorreo.SOLICITUD_PLAN, solicitud.organizacionId(),
                        admin.email(), admin.nombre(), datos));
    }

    private void avisarSolicitante(SolicitudCambioPlan solicitud) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("estado", solicitud.estado().name());
        datos.put("planSolicitado", planes.buscarPorCodigo(solicitud.planSolicitado())
                .map(Plan::nombre).orElse(solicitud.planSolicitado()));
        if (solicitud.respuesta() != null) {
            datos.put("respuesta", solicitud.respuesta());
        }
        usuarios.buscarPorNombreUsuario(solicitud.solicitadoPor())
                .filter(usuario -> usuario.estado() == EstadoRegistro.ACTIVO)
                .ifPresent(usuario -> correos.encolar(TipoCorreo.SOLICITUD_PLAN_RESUELTA, solicitud.organizacionId(),
                        usuario.email(), usuario.nombre(), datos));
    }

    private SolicitudCambioPlan resolver(SolicitudCambioPlan solicitud, EstadoSolicitudPlan estado, String usuario,
                                         String respuesta) {
        SolicitudCambioPlan resuelta = solicitudes.actualizar(
                solicitud.resolver(estado, usuario, reloj.instant(), respuesta));
        trazabilidad.registrar(new Transaccion(ENTIDAD_SOLICITUD, solicitud.id(), OperacionTransaccion.CAMBIO_ESTADO,
                solicitud.organizacionId(), solicitud, resuelta, null, solicitud.estado() + " -> " + estado));
        return resuelta;
    }

    private SolicitudCambioPlan solicitudPendiente(Long solicitudId) {
        if (solicitudId == null) {
            throw invalida("El id de la solicitud es obligatorio.");
        }
        SolicitudCambioPlan solicitud = solicitudes.buscarParaResolver(solicitudId)
                .orElseThrow(() -> new ExcepcionAplicacion(ErrorAplicacion.NO_ENCONTRADO,
                        "La solicitud no fue encontrada."));
        if (solicitud.estado() != EstadoSolicitudPlan.PENDIENTE) {
            throw conflicto("La solicitud ya fue resuelta (" + solicitud.estado() + ").");
        }
        return solicitud;
    }

    private PlanOrganizacion planParaActualizar(Long organizacionId) {
        buscarOrganizacion(organizacionId);
        return organizaciones.buscarPlanParaActualizar(organizacionId)
                .orElseThrow(() -> conflicto("La organización no tiene un plan asignado."));
    }

    private OrganizacionCliente buscarOrganizacion(Long organizacionId) {
        if (organizacionId == null) {
            throw invalida("El id de organización es obligatorio.");
        }
        return organizaciones.buscarPorId(organizacionId)
                .orElseThrow(() -> new ExcepcionAplicacion(ErrorAplicacion.NO_ENCONTRADO,
                        "La organización cliente no fue encontrada."));
    }

    private Plan buscarPlan(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            throw invalida("Elegí un plan.");
        }
        String normalizado = codigo.trim().toUpperCase(Locale.ROOT);
        return planes.buscarPorCodigo(normalizado)
                .orElseThrow(() -> invalida("El plan " + normalizado + " no existe."));
    }

    private static ModalidadContratacion parsearModalidad(String modalidad) {
        if (modalidad == null || modalidad.isBlank()) {
            return ModalidadContratacion.MENSUAL;
        }
        try {
            return ModalidadContratacion.valueOf(modalidad.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw invalida("La modalidad debe ser MENSUAL o ANUAL.");
        }
    }

    private static String textoOpcional(String valor, int maximo, String campo) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String recortado = valor.trim();
        if (recortado.length() > maximo) {
            throw invalida(campo + " no puede superar los " + maximo + " caracteres.");
        }
        return recortado;
    }

    /** Lo que queda en la bitácora: el plan y sus límites, sin el catálogo completo. */
    private static Map<String, Object> resumen(PlanOrganizacion plan) {
        Map<String, Object> valores = new LinkedHashMap<>();
        valores.put("plan", plan.plan().codigo());
        valores.put("maxDispositivos", plan.maxDispositivos());
        valores.put("maxUsuarios", plan.maxUsuarios());
        valores.put("desde", plan.desde() == null ? null : plan.desde().toString());
        valores.put("vence", plan.vence() == null ? null : plan.vence().toString());
        return valores;
    }

    private static ExcepcionAplicacion invalida(String mensaje) {
        return new ExcepcionAplicacion(ErrorAplicacion.SOLICITUD_INVALIDA, mensaje);
    }

    private static ExcepcionAplicacion conflicto(String mensaje) {
        return new ExcepcionAplicacion(ErrorAplicacion.CONFLICTO, mensaje);
    }
}
