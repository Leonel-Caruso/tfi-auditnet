package ar.edu.uai.tfi.management.domain.repository;

import ar.edu.uai.tfi.management.domain.model.Usuario;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface UsuarioRepository {
    Usuario guardar(Usuario usuario, Set<String> nombresRoles);
    List<Usuario> listar();
    Optional<Usuario> buscarPorEmail(String email);
    Optional<Usuario> buscarPorNombreUsuario(String nombreUsuario);
    Optional<Usuario> buscarPorIdentidad(String emailONombreUsuario);
}
