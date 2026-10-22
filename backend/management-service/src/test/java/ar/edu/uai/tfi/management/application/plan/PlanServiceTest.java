package ar.edu.uai.tfi.management.application.plan;

import ar.edu.uai.tfi.management.application.ErrorAplicacion;
import ar.edu.uai.tfi.management.application.ExcepcionAplicacion;
import ar.edu.uai.tfi.management.domain.model.EstadoRegistro;
import ar.edu.uai.tfi.management.domain.model.EstadoSolicitudPlan;
import ar.edu.uai.tfi.management.domain.model.ModalidadContratacion;
import ar.edu.uai.tfi.management.domain.model.OperacionTransaccion;
import ar.edu.uai.tfi.management.domain.model.Plan;
import ar.edu.uai.tfi.management.domain.model.PlanOrganizacion;
import ar.edu.uai.tfi.management.domain.model.SolicitudCambioPlan;
import ar.edu.uai.tfi.management.domain.model.TipoCorreo;
import ar.edu.uai.tfi.management.domain.model.Transaccion;
import ar.edu.uai.tfi.management.domain.model.Usuario;
import ar.edu.uai.tfi.management.support.EnMemoriaGestion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Catálogo, asignación, solicitudes de cambio y vencimiento del piloto. */
class PlanServiceTest {

    private static final Instant AHORA = Instant.parse("2026-10-22T12:00:00Z");
    private static final long DIA = 86_400;

    private final List<Transaccion> transacciones = new ArrayList<>();
    private final EnMemoriaGestion.Organizaciones organizaciones = new EnMemoriaGestion.Organizaciones();
    private final EnMemoriaGestion.Usuarios usuarios = new EnMemoriaGestion.Usuarios();
    private final EnMemoriaGestion.Dispositivos dispositivos = new EnMemoriaGestion.Dispositivos();
    private final EnMemoriaGestion.Solicitudes solicitudes = new EnMemoriaGestion.Solicitudes();
    private final EnMemoriaGestion.Planes catalogo = new EnMemoriaGestion.Planes();
    private final EnMemoriaGestion.Avisos avisos = new EnMemoriaGestion.Avisos();
    private final PlanService service = new PlanService(catalogo, organizaciones, solicitudes, usuarios, dispositivos,
            transacciones::add, avisos);

    private final EnMemoriaGestion.RelojAjustable reloj = new EnMemoriaGestion.RelojAjustable(AHORA);

    PlanServiceTest() {
        organizaciones.agregar(2L, "ACME", "ACME S.A.", EstadoRegistro.ACTIVO);
        service.reloj = reloj;
    }

    @Test
    void elEstadoInformaPlanUsoYDiasDelPiloto() {
        service.asignar(2L, "piloto", null, null, 60);
        dispositivos.activos.put(2L, 7L);
        usuarios.agregar(usuario(10L, "ana", EstadoRegistro.ACTIVO, "ANALISTA_RED"));
        usuarios.agregar(usuario(11L, "beto", EstadoRegistro.DESHABILITADO, "ANALISTA_RED"));
        usuarios.agregar(usuario(12L, "soporte", EstadoRegistro.ACTIVO, "ADMINISTRADOR_SISTEMA"));
        reloj.avanzar(Duration.ofHours(30));

        PlanService.EstadoPlan estado = service.estado(2L);

        assertEquals("PILOTO", estado.plan().plan().codigo());
        assertEquals(10, estado.plan().maxDispositivos());
        assertEquals(5, estado.plan().maxUsuarios());
        assertEquals(7, estado.dispositivosActivos());
        assertEquals(1, estado.usuariosActivos(), "Ni los deshabilitados ni los administradores del sistema cuentan");
        assertEquals(AHORA.plusSeconds(60 * DIA), estado.plan().vence());
        assertEquals(59, estado.diasRestantes());
        assertFalse(estado.vencido());
    }

    @Test
    void elPilotoVenceAlCumplirseElPlazo() {
        service.asignar(2L, "PILOTO", null, null, null);
        reloj.avanzar(Duration.ofDays(90));

        PlanService.EstadoPlan estado = service.estado(2L);

        assertTrue(estado.vencido());
        assertEquals(0, estado.diasRestantes());
        assertConflicto(() -> service.verificarAltaUsuario(2L));
    }

    @Test
    void elPilotoDuraEntre60Y90Dias() {
        assertInvalido(() -> service.asignar(2L, "PILOTO", null, null, 59));
        assertInvalido(() -> service.asignar(2L, "PILOTO", null, null, 91));
    }

    @Test
    void asignarRegistraElCambioEnLaBitacora() {
        service.asignar(2L, "PROFESIONAL", 999, 999, null);

        PlanOrganizacion plan = organizaciones.buscarPlan(2L).orElseThrow();
        assertEquals("PROFESIONAL", plan.plan().codigo());
        assertNull(plan.limiteDispositivos(), "Los límites propios solo valen para el plan a medida");
        Transaccion cambio = transacciones.get(0);
        assertEquals("PLAN", cambio.entidad());
        assertEquals(OperacionTransaccion.MODIFICACION, cambio.operacion());
        assertEquals("INICIAL -> PROFESIONAL", cambio.detalle());
        assertConflicto(() -> service.asignar(2L, "PROFESIONAL", null, null, null));
    }

    @Test
    void elPlanAMedidaExigeLimitesAcordados() {
        assertInvalido(() -> service.asignar(2L, "EMPRESA", null, 50, null));
        assertInvalido(() -> service.asignar(2L, "EMPRESA", 300, 0, null));

        service.asignar(2L, "EMPRESA", 300, 80, null);

        PlanOrganizacion plan = organizaciones.buscarPlan(2L).orElseThrow();
        assertEquals(300, plan.maxDispositivos());
        assertEquals(80, plan.maxUsuarios());
    }

    @Test
    void bajarDePlanSeRechazaSiElUsoSuperaElLimiteNuevo() {
        service.asignar(2L, "PROFESIONAL", null, null, null);
        dispositivos.activos.put(2L, 51L);

        ExcepcionAplicacion error = assertConflicto(() -> service.asignar(2L, "INICIAL", null, null, null));

        assertTrue(error.getMessage().contains("51 dispositivos activos"), error.getMessage());
        dispositivos.activos.put(2L, 50L);
        service.asignar(2L, "INICIAL", null, null, null);
        assertEquals("INICIAL", organizaciones.buscarPlan(2L).orElseThrow().plan().codigo());
    }

    @Test
    void circuitoCompletoDeSolicitudAprobada() {
        SolicitudCambioPlan pedida = service.solicitar(2L, "profesional", "anual", "Sumamos sedes", "jefa");

        assertEquals(EstadoSolicitudPlan.PENDIENTE, pedida.estado());
        assertEquals("INICIAL", pedida.planActual());
        assertEquals(ModalidadContratacion.ANUAL, pedida.modalidad());
        assertEquals(pedida, service.estado(2L).pendiente());
        assertConflicto(() -> service.solicitar(2L, "EMPRESA", "MENSUAL", null, "jefa"));

        SolicitudCambioPlan aprobada = service.aprobar(pedida.id(), null, null, "Bienvenidos", "admin");

        assertEquals(EstadoSolicitudPlan.APROBADA, aprobada.estado());
        assertEquals("admin", aprobada.resueltoPor());
        assertEquals("PROFESIONAL", organizaciones.buscarPlan(2L).orElseThrow().plan().codigo());
        assertNull(service.estado(2L).pendiente());
        assertConflicto(() -> service.rechazar(pedida.id(), "Tarde", "admin"));
        assertEquals(List.of("SOLICITUD_PLAN", "PLAN", "SOLICITUD_PLAN"),
                transacciones.stream().map(Transaccion::entidad).toList());
    }

    @Test
    void rechazarExigeMotivoYCancelarSoloLaPropia() {
        SolicitudCambioPlan pedida = service.solicitar(2L, "PROFESIONAL", null, null, "jefa");
        organizaciones.agregar(3L, "BETA", "Beta SRL", EstadoRegistro.ACTIVO);

        assertInvalido(() -> service.rechazar(pedida.id(), " ", "admin"));
        assertEquals(ErrorAplicacion.NO_ENCONTRADO,
                assertThrows(ExcepcionAplicacion.class, () -> service.cancelar(3L, pedida.id(), "otro")).tipo());

        SolicitudCambioPlan cancelada = service.cancelar(2L, pedida.id(), "jefa");

        assertEquals(EstadoSolicitudPlan.CANCELADA, cancelada.estado());
        assertEquals(ModalidadContratacion.MENSUAL, cancelada.modalidad());
        assertEquals("INICIAL", organizaciones.buscarPlan(2L).orElseThrow().plan().codigo());
        service.solicitar(2L, "PROFESIONAL", null, null, "jefa");
    }

    @Test
    void noSePideElPilotoNiElMismoPlanNiUnoMasChicoQueElUso() {
        usuarios.agregar(usuario(10L, "ana", EstadoRegistro.ACTIVO, "ANALISTA_RED"));
        dispositivos.activos.put(2L, 11L);

        assertInvalido(() -> service.solicitar(2L, "PILOTO", null, null, "jefa"));
        assertConflicto(() -> service.solicitar(2L, "INICIAL", null, null, "jefa"));
        service.asignar(2L, "PROFESIONAL", null, null, null);
        dispositivos.activos.put(2L, 60L);
        assertConflicto(() -> service.solicitar(2L, "INICIAL", null, null, "jefa"));
        assertInvalido(() -> service.solicitar(2L, "EMPRESA", "TRIMESTRAL", null, "jefa"));
    }

    @Test
    void aprobarUnPlanAMedidaPideLosLimites() {
        SolicitudCambioPlan pedida = service.solicitar(2L, "EMPRESA", "ANUAL", "Somos 400 equipos", "jefa");

        assertInvalido(() -> service.aprobar(pedida.id(), null, null, null, "admin"));
        service.aprobar(pedida.id(), 400, 120, null, "admin");

        assertEquals(400, organizaciones.buscarPlan(2L).orElseThrow().maxDispositivos());
    }

    @Test
    void elPrecioDelCatalogoSeEditaYQuedaEnLaBitacora() {
        Plan actualizado = service.actualizarCatalogo("inicial", new BigDecimal("168"), "Año 2: +12 %");

        assertEquals(new BigDecimal("168.00"), actualizado.precioMensualUsd());
        assertEquals(50, actualizado.maxDispositivos(), "Los límites del catálogo no cambian");
        assertEquals("CATALOGO_PLAN", transacciones.get(0).entidad());
        assertInvalido(() -> service.actualizarCatalogo("INICIAL", new BigDecimal("-1"), null));
        assertInvalido(() -> service.actualizarCatalogo("INICIAL", new BigDecimal("10.123"), null));
        assertInvalido(() -> service.actualizarCatalogo("OTRO", null, null));
    }

    @Test
    void laSolicitudAvisaALosAdministradoresYLaRespuestaAlSolicitante() {
        usuarios.agregar(new Usuario(1L, 1L, "Admin", "admin@plataforma.test", "admin", "h", EstadoRegistro.ACTIVO,
                Set.of("ADMINISTRADOR_SISTEMA")));
        usuarios.agregar(new Usuario(3L, 1L, "Otro admin", "otro@plataforma.test", "otro", "h",
                EstadoRegistro.DESHABILITADO, Set.of("ADMINISTRADOR_SISTEMA")));
        usuarios.agregar(new Usuario(20L, 2L, "Jefa", "jefa@acme.test", "jefa", "h", EstadoRegistro.ACTIVO,
                Set.of("ADMINISTRADOR_ORGANIZACION")));

        SolicitudCambioPlan pedida = service.solicitar(2L, "PROFESIONAL", "ANUAL", "Sumamos sedes", "jefa");
        service.rechazar(pedida.id(), "Falta el contrato firmado", "admin");

        assertEquals(2, avisos.items.size());
        EnMemoriaGestion.Avisos.Aviso aviso = avisos.items.get(0);
        assertEquals(TipoCorreo.SOLICITUD_PLAN, aviso.tipo());
        assertEquals("admin@plataforma.test", aviso.destinatario(), "Solo a los administradores activos");
        assertEquals("Profesional", aviso.datos().get("planSolicitado"));
        EnMemoriaGestion.Avisos.Aviso respuesta = avisos.items.get(1);
        assertEquals(TipoCorreo.SOLICITUD_PLAN_RESUELTA, respuesta.tipo());
        assertEquals("jefa@acme.test", respuesta.destinatario());
        assertEquals("RECHAZADA", respuesta.datos().get("estado"));
        assertEquals("Falta el contrato firmado", respuesta.datos().get("respuesta"));
    }

    private static void assertInvalido(Executable accion) {
        assertEquals(ErrorAplicacion.SOLICITUD_INVALIDA, assertThrows(ExcepcionAplicacion.class, accion).tipo());
    }

    private static ExcepcionAplicacion assertConflicto(Executable accion) {
        ExcepcionAplicacion error = assertThrows(ExcepcionAplicacion.class, accion);
        assertEquals(ErrorAplicacion.CONFLICTO, error.tipo());
        return error;
    }

    private static Usuario usuario(Long id, String nombreUsuario, EstadoRegistro estado, String rol) {
        return new Usuario(id, 2L, nombreUsuario, nombreUsuario + "@acme.test", nombreUsuario, "hash", estado,
                Set.of(rol));
    }
}
