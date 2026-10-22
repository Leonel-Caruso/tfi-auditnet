package ar.edu.uai.tfi.management.domain.repository;

import ar.edu.uai.tfi.management.domain.model.EstadoAcceso;
import ar.edu.uai.tfi.management.domain.model.EstadoRegistro;
import ar.edu.uai.tfi.management.domain.model.PreferenciasAviso;
import ar.edu.uai.tfi.management.domain.model.Usuario;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface UsuarioRepository {

    Usuario guardar(Usuario usuario, Set<String> nombresRoles);

    List<Usuario> listar();

    Optional<Usuario> buscarPorEmail(String email);

    Optional<Usuario> buscarPorNombreUsuario(String nombreUsuario);

    Optional<Usuario> buscarPorIdentidad(String emailONombreUsuario);

    Optional<Usuario> buscarPorId(Long id);

    /** Actualiza nombre, email, organización y roles (no la contraseña ni el estado). */
    Usuario actualizar(Usuario usuario, Set<String> nombresRoles);

    Usuario cambiarEstado(Long id, EstadoRegistro estado);

    /** Guarda la nueva contraseña (hash), quita el bloqueo y deja sin efecto las sesiones anteriores a {@code ahora}. */
    void cambiarPassword(Long id, String passwordHash, Instant ahora);

    /** Los tokens emitidos antes de {@code desde} dejan de valer (cambio de roles u organización). */
    void invalidarSesiones(Long id, Instant desde);

    /** Igual que {@link #invalidarSesiones} para todos los usuarios de una organización (al deshabilitarla). */
    void invalidarSesionesDeOrganizacion(Long organizacionId, Instant desde);

    EstadoAcceso estadoAcceso(Long id);

    /** Estado de acceso de todos los usuarios (id de usuario → estado), para el listado del administrador. */
    Map<Long, EstadoAcceso> estadosAcceso();

    /**
     * Suma un intento fallido. Al llegar a {@code maximo} bloquea la cuenta durante {@code bloqueo} y reinicia el
     * contador. Se guarda aunque el login termine con error (transacción propia).
     */
    EstadoAcceso registrarIntentoFallido(Long id, Instant ahora, int maximo, Duration bloqueo);

    /** Login exitoso: reinicia los intentos fallidos y guarda la fecha de acceso. */
    void registrarAccesoExitoso(Long id, Instant ahora);

    void desbloquear(Long id);

    PreferenciasAviso preferencias(Long id);

    void guardarPreferencias(Long id, PreferenciasAviso preferencias);
}
