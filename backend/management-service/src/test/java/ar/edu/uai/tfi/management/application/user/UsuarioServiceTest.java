package ar.edu.uai.tfi.management.application.user;

import ar.edu.uai.tfi.management.application.port.PasswordPort;
import ar.edu.uai.tfi.management.domain.model.EstadoRegistro;
import ar.edu.uai.tfi.management.domain.model.EventoSistema;
import ar.edu.uai.tfi.management.domain.model.OrganizacionCliente;
import ar.edu.uai.tfi.management.domain.model.ResultadoEvento;
import ar.edu.uai.tfi.management.domain.model.Rol;
import ar.edu.uai.tfi.management.domain.model.TipoEventoSistema;
import ar.edu.uai.tfi.management.domain.model.Usuario;
import ar.edu.uai.tfi.management.domain.repository.OrganizacionClienteRepository;
import ar.edu.uai.tfi.management.domain.repository.RolRepository;
import ar.edu.uai.tfi.management.domain.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Bloque 2.3a · Crear un usuario deja en la bitácora la creación y la asignación de privilegios.
 */
class UsuarioServiceTest {

    @Test
    void crearUsuarioRegistraCreacionYAsignacionDeRoles() {
        List<EventoSistema> eventos = new ArrayList<>();
        UsuarioService service = new UsuarioService(
                new UsuariosEnMemoria(), new OrganizacionUnica(), new RolesActivos(), new PasswordSimple(), eventos::add);

        Usuario creado = service.crear(3L, "Ana Pérez", "Ana@Cliente.com", "Ana", "Secreta123",
                Set.of("analista_red"));

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

    private static class UsuariosEnMemoria implements UsuarioRepository {
        private final List<Usuario> usuarios = new ArrayList<>();

        @Override
        public Usuario guardar(Usuario usuario, Set<String> nombresRoles) {
            Usuario guardado = new Usuario((long) usuarios.size() + 10, usuario.organizacionId(), usuario.nombre(),
                    usuario.email(), usuario.nombreUsuario(), usuario.passwordHash(), usuario.estado(), nombresRoles);
            usuarios.add(guardado);
            return guardado;
        }

        @Override
        public List<Usuario> listar() {
            return usuarios;
        }

        @Override
        public Optional<Usuario> buscarPorEmail(String email) {
            return usuarios.stream().filter(u -> u.email().equals(email)).findFirst();
        }

        @Override
        public Optional<Usuario> buscarPorNombreUsuario(String nombreUsuario) {
            return usuarios.stream().filter(u -> u.nombreUsuario().equals(nombreUsuario)).findFirst();
        }

        @Override
        public Optional<Usuario> buscarPorIdentidad(String identidad) {
            return Optional.empty();
        }
    }

    private static class OrganizacionUnica implements OrganizacionClienteRepository {
        private final OrganizacionCliente organizacion =
                new OrganizacionCliente(3L, "CLIENTE-1", "Cliente Uno", EstadoRegistro.ACTIVO);

        @Override
        public OrganizacionCliente guardar(OrganizacionCliente o) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<OrganizacionCliente> listar() {
            return List.of(organizacion);
        }

        @Override
        public Optional<OrganizacionCliente> buscarPorId(Long id) {
            return organizacion.id().equals(id) ? Optional.of(organizacion) : Optional.empty();
        }

        @Override
        public Optional<OrganizacionCliente> buscarPorIdentificador(String identificador) {
            return Optional.empty();
        }

        @Override
        public Optional<OrganizacionCliente> buscarPorRazonSocial(String razonSocial) {
            return Optional.empty();
        }
    }

    private static class RolesActivos implements RolRepository {
        @Override
        public Rol guardar(Rol rol) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Rol> listar() {
            return List.of();
        }

        @Override
        public Optional<Rol> buscarPorNombre(String nombre) {
            return Optional.of(new Rol(1L, nombre, "Rol de prueba", EstadoRegistro.ACTIVO));
        }
    }

    private static class PasswordSimple implements PasswordPort {
        @Override
        public String hashear(String passwordPlano) {
            return "hash:" + passwordPlano.length();
        }

        @Override
        public boolean coincide(String passwordPlano, String passwordHash) {
            return false;
        }
    }
}
