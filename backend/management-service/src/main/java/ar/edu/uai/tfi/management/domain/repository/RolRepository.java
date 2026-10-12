package ar.edu.uai.tfi.management.domain.repository;

import ar.edu.uai.tfi.management.domain.model.Rol;

import java.util.List;
import java.util.Optional;

public interface RolRepository {
    Rol guardar(Rol rol);
    List<Rol> listar();
    Optional<Rol> buscarPorNombre(String nombre);

    Optional<Rol> buscarPorId(Long id);

    /** Actualiza la descripción (los nombres y permisos de los perfiles son fijos). */
    Rol actualizar(Rol rol);
}
