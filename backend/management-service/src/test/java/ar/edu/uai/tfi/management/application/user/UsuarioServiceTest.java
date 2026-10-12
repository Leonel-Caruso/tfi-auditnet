package ar.edu.uai.tfi.management.application.user;

import ar.edu.uai.tfi.management.application.ErrorAplicacion;
import ar.edu.uai.tfi.management.application.ExcepcionAplicacion;
import ar.edu.uai.tfi.management.domain.model.EstadoAcceso;
import ar.edu.uai.tfi.management.domain.model.EstadoRegistro;
import ar.edu.uai.tfi.management.domain.model.EventoSistema;
import ar.edu.uai.tfi.management.domain.model.ResultadoEvento;
import ar.edu.uai.tfi.management.domain.model.TipoEventoSistema;
import ar.edu.uai.tfi.management.domain.model.Usuario;
import ar.edu.uai.tfi.management.support.EnMemoriaGestion;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Alta con registro de creación y privilegios en la bitácora; modificación, habilitación, restablecimiento de
 * contraseña, desbloqueo y reglas de protección.
 */
class UsuarioServiceTest {

    private static final Instant AHORA = Instant.parse("2026-10-11T12:00:00Z");
    private static final String ADMIN = "ADMINISTRADOR_SISTEMA";

    private final List<EventoSistema> eventos = new ArrayList<>();
    private final EnMemoriaGestion.Usuarios usuarios = new EnMemoriaGestion.Usuarios();
    private final EnMemoriaGestion.Organizaciones organizaciones = new EnMemoriaGestion.Organizaciones();
    private final UsuarioService service;

    UsuarioServiceTest() {
        organizaciones.agregar(1L, "PLATAFORMA", "Plataforma", EstadoRegistro.ACTIVO);
        organizaciones.agregar(3L, "CLIENTE-1", "Cliente Uno", EstadoRegistro.ACTIVO);
        service = new UsuarioService(usuarios, organizaciones, new EnMemoriaGestion.Roles(),
                new EnMemoriaGestion.PasswordSimple(), eventos::add);
        service.reloj = new EnMemoriaGestion.RelojAjustable(AHORA);
    }

    @Test
    void crearUsuarioRegistraCreacionYAsignacionDeRoles() {
        Usuario creado = service.crear(3L, "Ana Pérez", "Ana@Cliente.com", "Ana", "Secreta123",
                Set.of("analista_red"));

        assertEquals("ana", creado.nombreUsuario());
        assertEquals("ana@cliente.com", creado.email());
        assertEquals(2, eventos.size());

        EventoSistema creacion = eventos.get(0);
        assertEquals(TipoEventoSistema.USUARIO_CREADO, creacion.tipo());
        assertEquals(ResultadoEvento.EXITO, creacion.resultado());
        assertNull(creacion.actor(), "El actor lo completa el adaptador con el usuario autenticado");
        assertEquals(creado.id(), creacion.usuarioAfectadoId());
        assertEquals(3L, creacion.organizacionId());
        assertFalse(creacion.detalle().contains("Secreta123"), "La bitácora nunca debe contener contraseñas");

        EventoSistema roles = eventos.get(1);
        assertEquals(TipoEventoSistema.ROLES_ASIGNADOS, roles.tipo());
        assertTrue(roles.detalle().contains("ANALISTA_RED"));
    }

    @Test
    void crearRechazaContrasenaDebilYUsuarioDuplicado() {
        ExcepcionAplicacion debil = assertThrows(ExcepcionAplicacion.class,
                () -> service.crear(3L, "Ana", "ana@cliente.com", "ana", "corta1", Set.of("ANALISTA_RED")));
        assertEquals(ErrorAplicacion.SOLICITUD_INVALIDA, debil.tipo());

        service.crear(3L, "Ana", "ana@cliente.com", "ana", "Secreta1234", Set.of("ANALISTA_RED"));
        ExcepcionAplicacion duplicado = assertThrows(ExcepcionAplicacion.class,
                () -> service.crear(3L, "Ana 2", "otra@cliente.com", "ANA", "Secreta1234", Set.of("ANALISTA_RED")));
        assertEquals(ErrorAplicacion.CONFLICTO, duplicado.tipo());
    }

    @Test
    void actualizarRolesCierraLasSesionesYRegistraElCambio() {
        Usuario ana = usuarios.agregar(usuario(10L, 3L, "ana", Set.of("ANALISTA_RED")));
        usuarios.agregar(usuario(1L, 1L, "admin", Set.of(ADMIN)));

        Usuario actualizado = service.actualizar(ana.id(), 3L, "Ana María", "ana@cliente.com",
                Set.of("AUDITOR_TECNICO"), 1L);

        assertEquals(Set.of("AUDITOR_TECNICO"), actualizado.roles());
        assertEquals("ana", actualizado.nombreUsuario(), "El nombre de usuario no cambia");
        assertEquals(AHORA, usuarios.estadoAcceso(10L).sesionesValidasDesde());
        assertEquals(TipoEventoSistema.USUARIO_MODIFICADO, eventos.get(0).tipo());
        assertEquals(TipoEventoSistema.ROLES_ASIGNADOS, eventos.get(1).tipo());
        assertTrue(eventos.get(1).detalle().contains("anteriores=ANALISTA_RED"));
    }

    @Test
    void actualizarSoloElNombreNoCierraLasSesiones() {
        usuarios.agregar(usuario(10L, 3L, "ana", Set.of("ANALISTA_RED")));

        service.actualizar(10L, 3L, "Ana María", "ana@cliente.com", Set.of("ANALISTA_RED"), 1L);

        assertNull(usuarios.estadoAcceso(10L).sesionesValidasDesde());
        assertEquals(1, eventos.size());
        assertEquals(TipoEventoSistema.USUARIO_MODIFICADO, eventos.get(0).tipo());
    }

    @Test
    void actualizarRechazaEmailDeOtroUsuario() {
        usuarios.agregar(usuario(10L, 3L, "ana", Set.of("ANALISTA_RED")));
        usuarios.agregar(usuario(11L, 3L, "beto", Set.of("ANALISTA_RED")));

        ExcepcionAplicacion error = assertThrows(ExcepcionAplicacion.class,
                () -> service.actualizar(10L, 3L, "Ana", "beto@cliente.com", Set.of("ANALISTA_RED"), 1L));

        assertEquals(ErrorAplicacion.CONFLICTO, error.tipo());
    }

    @Test
    void elAdministradorNoPuedeQuitarseSuPropioPerfilNiDeshabilitarse() {
        usuarios.agregar(usuario(1L, 1L, "admin", Set.of(ADMIN)));
        usuarios.agregar(usuario(2L, 1L, "admin2", Set.of(ADMIN)));

        ExcepcionAplicacion perfil = assertThrows(ExcepcionAplicacion.class,
                () -> service.actualizar(1L, 1L, "Admin", "admin@cliente.com", Set.of("AUDITOR_TECNICO"), 1L));
        ExcepcionAplicacion estado = assertThrows(ExcepcionAplicacion.class,
                () -> service.cambiarEstado(1L, "DESHABILITADO", 1L));

        assertEquals(ErrorAplicacion.CONFLICTO, perfil.tipo());
        assertEquals(ErrorAplicacion.CONFLICTO, estado.tipo());
        assertTrue(eventos.isEmpty());
    }

    @Test
    void siempreQuedaUnAdministradorActivo() {
        usuarios.agregar(usuario(1L, 1L, "admin", Set.of(ADMIN)));
        usuarios.agregar(usuario(2L, 1L, "admin2", Set.of(ADMIN)));

        service.cambiarEstado(2L, "DESHABILITADO", 1L);
        // Otro actor (por ejemplo, un administrador recién deshabilitado con un token viejo) no puede dejar 0.
        ExcepcionAplicacion ultimo = assertThrows(ExcepcionAplicacion.class,
                () -> service.cambiarEstado(1L, "DESHABILITADO", 2L));
        ExcepcionAplicacion sinPerfil = assertThrows(ExcepcionAplicacion.class,
                () -> service.actualizar(1L, 1L, "Admin", "admin@cliente.com", Set.of("ANALISTA_RED"), 2L));

        assertEquals(ErrorAplicacion.CONFLICTO, ultimo.tipo());
        assertEquals(ErrorAplicacion.CONFLICTO, sinPerfil.tipo());
        assertEquals(EstadoRegistro.DESHABILITADO, usuarios.buscarPorId(2L).orElseThrow().estado());
        assertEquals(TipoEventoSistema.USUARIO_DESHABILITADO, eventos.get(0).tipo());
    }

    @Test
    void habilitarYDeshabilitarRegistraEventosYRechazaElMismoEstado() {
        usuarios.agregar(usuario(10L, 3L, "ana", Set.of("ANALISTA_RED")));

        service.cambiarEstado(10L, "deshabilitado", 1L);
        ExcepcionAplicacion repetido = assertThrows(ExcepcionAplicacion.class,
                () -> service.cambiarEstado(10L, "DESHABILITADO", 1L));
        ExcepcionAplicacion invalido = assertThrows(ExcepcionAplicacion.class,
                () -> service.cambiarEstado(10L, "BORRADO", 1L));
        service.cambiarEstado(10L, "ACTIVO", 1L);

        assertEquals(ErrorAplicacion.CONFLICTO, repetido.tipo());
        assertEquals(ErrorAplicacion.SOLICITUD_INVALIDA, invalido.tipo());
        assertEquals(AHORA, usuarios.estadoAcceso(10L).sesionesValidasDesde(),
                "Deshabilitar cierra las sesiones: al habilitarlo, los tokens viejos no vuelven a valer");
        assertEquals(List.of(TipoEventoSistema.USUARIO_DESHABILITADO, TipoEventoSistema.USUARIO_HABILITADO),
                eventos.stream().map(EventoSistema::tipo).toList());
    }

    @Test
    void restablecerPasswordQuitaElBloqueoYCierraLasSesiones() {
        usuarios.agregar(usuario(10L, 3L, "ana", Set.of("ANALISTA_RED")));
        usuarios.accesos.put(10L, new EstadoAcceso(10L, 0, AHORA.plusSeconds(600), null, null));

        ExcepcionAplicacion conUsuario = assertThrows(ExcepcionAplicacion.class,
                () -> service.restablecerPassword(10L, "ana-2026-clave"));
        service.restablecerPassword(10L, "Temporal2026");

        assertEquals(ErrorAplicacion.SOLICITUD_INVALIDA, conUsuario.tipo());
        assertEquals("hash:Temporal2026", usuarios.buscarPorId(10L).orElseThrow().passwordHash());
        EstadoAcceso acceso = usuarios.estadoAcceso(10L);
        assertNull(acceso.bloqueadoHasta());
        assertEquals(AHORA, acceso.sesionesValidasDesde());
        assertEquals(TipoEventoSistema.CONTRASENA_RESTABLECIDA, eventos.get(0).tipo());
        assertFalse(eventos.get(0).detalle().contains("Temporal2026"));
    }

    @Test
    void desbloquearSoloAplicaAUnaCuentaBloqueada() {
        usuarios.agregar(usuario(10L, 3L, "ana", Set.of("ANALISTA_RED")));

        ExcepcionAplicacion noBloqueada = assertThrows(ExcepcionAplicacion.class, () -> service.desbloquear(10L));
        usuarios.accesos.put(10L, new EstadoAcceso(10L, 0, AHORA.plusSeconds(600), null, null));
        service.desbloquear(10L);

        assertEquals(ErrorAplicacion.CONFLICTO, noBloqueada.tipo());
        assertFalse(usuarios.estadoAcceso(10L).bloqueado(AHORA));
        assertEquals(TipoEventoSistema.CUENTA_DESBLOQUEADA, eventos.get(0).tipo());
    }

    @Test
    void listarConAccesoIncluyeBloqueoYUltimoAcceso() {
        usuarios.agregar(usuario(10L, 3L, "ana", Set.of("ANALISTA_RED")));
        usuarios.accesos.put(10L, new EstadoAcceso(10L, 2, AHORA.plusSeconds(60), AHORA.minusSeconds(3600), null));

        UsuarioService.UsuarioConAcceso fila = service.listarConAcceso().get(0);

        assertEquals("ana", fila.usuario().nombreUsuario());
        assertEquals(AHORA.plusSeconds(60), fila.acceso().bloqueadoHasta());
        assertEquals(AHORA.minusSeconds(3600), fila.acceso().ultimoAcceso());
    }

    @Test
    void usuarioInexistenteDevuelveNoEncontrado() {
        ExcepcionAplicacion error = assertThrows(ExcepcionAplicacion.class, () -> service.buscar(99L));
        assertEquals(ErrorAplicacion.NO_ENCONTRADO, error.tipo());
    }

    private static Usuario usuario(Long id, Long organizacionId, String nombreUsuario, Set<String> roles) {
        return new Usuario(id, organizacionId, nombreUsuario, nombreUsuario + "@cliente.com", nombreUsuario,
                "hash:Original2026", EstadoRegistro.ACTIVO, roles);
    }
}
