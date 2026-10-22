package ar.edu.uai.tfi.management.application.user;

import ar.edu.uai.tfi.management.application.ErrorAplicacion;
import ar.edu.uai.tfi.management.application.ExcepcionAplicacion;
import ar.edu.uai.tfi.management.application.b2b.SedeService;
import ar.edu.uai.tfi.management.application.plan.PlanService;
import ar.edu.uai.tfi.management.domain.model.EstadoRegistro;
import ar.edu.uai.tfi.management.domain.model.PlanOrganizacion;
import ar.edu.uai.tfi.management.domain.model.Sede;
import ar.edu.uai.tfi.management.domain.model.Usuario;
import ar.edu.uai.tfi.management.support.EnMemoriaGestion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Administrador de organización: gestiona usuarios con perfiles de operación y sedes de su organización, nada más.
 * También el límite de usuarios del plan.
 */
class AdministradorOrganizacionTest {

    private static final Instant AHORA = Instant.now();
    private static final String ADMIN_ORG = MatrizPermisos.ADMIN_ORGANIZACION;
    private static final Actor ADMIN_ACME = new Actor(20L, 2L, Set.of(ADMIN_ORG));
    private static final Actor ADMIN_SISTEMA = new Actor(1L, 1L, Set.of(MatrizPermisos.ADMIN));

    private final EnMemoriaGestion.Usuarios usuarios = new EnMemoriaGestion.Usuarios();
    private final EnMemoriaGestion.Organizaciones organizaciones = new EnMemoriaGestion.Organizaciones();
    private final EnMemoriaGestion.Sedes sedes = new EnMemoriaGestion.Sedes();
    private final PlanService planes = new PlanService(new EnMemoriaGestion.Planes(), organizaciones,
            new EnMemoriaGestion.Solicitudes(), usuarios, new EnMemoriaGestion.Dispositivos(), transaccion -> { }, new EnMemoriaGestion.Avisos());
    private final UsuarioService service = new UsuarioService(usuarios, organizaciones, new EnMemoriaGestion.Roles(),
            new EnMemoriaGestion.PasswordSimple(), evento -> { }, planes, new EnMemoriaGestion.Avisos());
    private final SedeService sedeService = new SedeService(sedes, organizaciones, transaccion -> { });

    AdministradorOrganizacionTest() {
        organizaciones.agregar(1L, "PLATAFORMA", "Plataforma", EstadoRegistro.ACTIVO);
        organizaciones.agregar(2L, "ACME", "ACME S.A.", EstadoRegistro.ACTIVO);
        organizaciones.agregar(3L, "BETA", "Beta SRL", EstadoRegistro.ACTIVO);
        usuarios.agregar(usuario(1L, 1L, "admin", Set.of(MatrizPermisos.ADMIN)));
        usuarios.agregar(usuario(20L, 2L, "jefa", Set.of(ADMIN_ORG)));
        usuarios.agregar(usuario(21L, 2L, "ana", Set.of(MatrizPermisos.ANALISTA)));
        usuarios.agregar(usuario(30L, 3L, "beto", Set.of(MatrizPermisos.ANALISTA)));
        usuarios.agregar(usuario(31L, 3L, "otra.jefa", Set.of(ADMIN_ORG)));
    }

    @Test
    void veSoloLosUsuariosDeSuOrganizacion() {
        var lista = service.listarConAcceso(ADMIN_ACME);

        assertEquals(Set.of("jefa", "ana"), Set.copyOf(lista.stream().map(f -> f.usuario().nombreUsuario()).toList()));
        assertProhibido(() -> service.buscar(30L, ADMIN_ACME));
        assertEquals(5, service.listarConAcceso(ADMIN_SISTEMA).size());
    }

    @Test
    void creaUsuariosDeOperacionEnSuOrganizacion() {
        Usuario creado = service.crear(2L, "Carla", "carla@acme.test", "carla", "Secreta2026",
                Set.of(MatrizPermisos.AUDITOR, MatrizPermisos.RESPONSABLE), ADMIN_ACME);

        assertEquals(2L, creado.organizacionId());
        assertEquals(Set.of(MatrizPermisos.AUDITOR, MatrizPermisos.RESPONSABLE), creado.roles());
    }

    @Test
    void noPuedeCrearEnOtraOrganizacionNiAsignarPerfilesDeAdministrador() {
        assertProhibido(() -> service.crear(3L, "Carla", "carla@acme.test", "carla", "Secreta2026",
                Set.of(MatrizPermisos.ANALISTA), ADMIN_ACME));
        assertProhibido(() -> service.crear(2L, "Carla", "carla@acme.test", "carla", "Secreta2026",
                Set.of(ADMIN_ORG), ADMIN_ACME));
        assertProhibido(() -> service.crear(2L, "Carla", "carla@acme.test", "carla", "Secreta2026",
                Set.of(MatrizPermisos.ADMIN), ADMIN_ACME));
        assertProhibido(() -> service.actualizar(21L, 2L, "Ana", "ana@cliente.com",
                Set.of(MatrizPermisos.ANALISTA, ADMIN_ORG), ADMIN_ACME));
        assertProhibido(() -> service.actualizar(21L, 3L, "Ana", "ana@cliente.com",
                Set.of(MatrizPermisos.ANALISTA), ADMIN_ACME));
    }

    @Test
    void noTocaAUsuariosDeOtraOrganizacionNiAAdministradores() {
        assertProhibido(() -> service.cambiarEstado(30L, "DESHABILITADO", ADMIN_ACME));
        assertProhibido(() -> service.restablecerPassword(30L, "Temporal2026", ADMIN_ACME));
        assertProhibido(() -> service.cambiarEstado(31L, "DESHABILITADO", ADMIN_ACME));
        assertProhibido(() -> service.restablecerPassword(20L, "Temporal2026", ADMIN_ACME));
        assertProhibido(() -> service.desbloquear(1L, ADMIN_ACME));
    }

    @Test
    void gestionaAUnUsuarioDeOperacionDeSuOrganizacion() {
        service.actualizar(21L, 2L, "Ana María", "ana@cliente.com", Set.of(MatrizPermisos.AUDITOR), ADMIN_ACME);
        service.cambiarEstado(21L, "DESHABILITADO", ADMIN_ACME);
        service.restablecerPassword(21L, "Temporal2026", ADMIN_ACME);

        Usuario ana = usuarios.buscarPorId(21L).orElseThrow();
        assertEquals(Set.of(MatrizPermisos.AUDITOR), ana.roles());
        assertEquals(EstadoRegistro.DESHABILITADO, ana.estado());
        assertEquals("hash:Temporal2026", ana.passwordHash());
    }

    @Test
    void unPerfilDeOperacionNoGestionaUsuarios() {
        Actor analista = new Actor(21L, 2L, Set.of(MatrizPermisos.ANALISTA));

        assertProhibido(() -> service.listarConAcceso(analista));
        assertProhibido(() -> service.cambiarEstado(21L, "DESHABILITADO", analista));
    }

    @Test
    void sinOrganizacionEnLaSesionNoTieneAlcance() {
        Actor sinOrganizacion = new Actor(20L, null, Set.of(ADMIN_ORG));

        assertProhibido(() -> service.listarConAcceso(sinOrganizacion));
        assertProhibido(() -> service.cambiarEstado(21L, "DESHABILITADO", sinOrganizacion));
    }

    @Test
    void gestionaLasSedesDeSuOrganizacionYNoLasDeOtra() {
        Sede propia = sedeService.crear(2L, "Casa central", "CABA", 2L);
        Sede ajena = sedeService.crear(3L, "Planta", "Pilar", null);

        sedeService.actualizar(propia.id(), "Sede central", "CABA", 2L);
        sedeService.cambiarEstado(propia.id(), "DESHABILITADO", 2L);

        assertEquals(EstadoRegistro.DESHABILITADO, sedes.buscarPorId(propia.id()).orElseThrow().estado());
        assertProhibido(() -> sedeService.crear(3L, "Depósito", "Lanús", 2L));
        assertProhibido(() -> sedeService.actualizar(ajena.id(), "Otra", "Pilar", 2L));
        assertProhibido(() -> sedeService.cambiarEstado(ajena.id(), "DESHABILITADO", 2L));
        assertProhibido(() -> sedeService.buscarPorId(ajena.id(), 2L));
    }

    @Test
    void elLimiteDeUsuariosPermiteHastaElMaximoExactoDelPlan() {
        // Piloto: 5 usuarios. ACME ya tiene 2 (jefa y ana); los administradores del sistema no cuentan.
        organizaciones.asignarPlan(2L, new PlanOrganizacion(2L, EnMemoriaGestion.Planes.PILOTO, AHORA,
                AHORA.plusSeconds(30L * 86_400), null, null));
        for (int i = 1; i <= 3; i++) {
            service.crear(2L, "Usuario " + i, "u" + i + "@acme.test", "usuario" + i, "Secreta2026",
                    Set.of(MatrizPermisos.ANALISTA), ADMIN_ACME);
        }

        ExcepcionAplicacion sexto = assertThrows(ExcepcionAplicacion.class,
                () -> service.crear(2L, "Usuario 6", "u6@acme.test", "usuario6", "Secreta2026",
                        Set.of(MatrizPermisos.ANALISTA), ADMIN_ACME));
        service.crear(2L, "Soporte", "soporte@plataforma.test", "soporte", "Secreta2026",
                Set.of(MatrizPermisos.ADMIN), ADMIN_SISTEMA);

        assertEquals(ErrorAplicacion.CONFLICTO, sexto.tipo());
        assertTrue(sexto.getMessage().contains("hasta 5 usuarios activos"), sexto.getMessage());
    }

    @Test
    void rehabilitarOMoverUnUsuarioTambienRespetaElLimite() {
        organizaciones.asignarPlan(2L, new PlanOrganizacion(2L, EnMemoriaGestion.Planes.EMPRESA, AHORA, null, 10, 2));
        service.cambiarEstado(21L, "DESHABILITADO", ADMIN_ACME);
        service.crear(2L, "Carla", "carla@acme.test", "carla", "Secreta2026", Set.of(MatrizPermisos.ANALISTA),
                ADMIN_ACME);

        assertConflicto(() -> service.cambiarEstado(21L, "ACTIVO", ADMIN_ACME));
        assertConflicto(() -> service.actualizar(30L, 2L, "Beto", "beto@cliente.com",
                Set.of(MatrizPermisos.ANALISTA), ADMIN_SISTEMA));
    }

    @Test
    void conElPilotoVencidoNoSeSumanUsuarios() {
        organizaciones.asignarPlan(2L, new PlanOrganizacion(2L, EnMemoriaGestion.Planes.PILOTO,
                AHORA.minusSeconds(90L * 86_400), AHORA.minusSeconds(60), null, null));

        ExcepcionAplicacion error = assertConflicto(() -> service.crear(2L, "Carla", "carla@acme.test", "carla",
                "Secreta2026", Set.of(MatrizPermisos.ANALISTA), ADMIN_ACME));
        service.cambiarEstado(21L, "DESHABILITADO", ADMIN_ACME);

        assertTrue(error.getMessage().contains("venció el"), error.getMessage());
        assertConflicto(() -> service.cambiarEstado(21L, "ACTIVO", ADMIN_ACME));
    }

    private static void assertProhibido(Executable accion) {
        assertEquals(ErrorAplicacion.PROHIBIDO, assertThrows(ExcepcionAplicacion.class, accion).tipo());
    }

    private static ExcepcionAplicacion assertConflicto(Executable accion) {
        ExcepcionAplicacion error = assertThrows(ExcepcionAplicacion.class, accion);
        assertEquals(ErrorAplicacion.CONFLICTO, error.tipo());
        return error;
    }

    private static Usuario usuario(Long id, Long organizacionId, String nombreUsuario, Set<String> roles) {
        return new Usuario(id, organizacionId, nombreUsuario, nombreUsuario + "@cliente.com", nombreUsuario,
                "hash:Original2026", EstadoRegistro.ACTIVO, roles);
    }
}
