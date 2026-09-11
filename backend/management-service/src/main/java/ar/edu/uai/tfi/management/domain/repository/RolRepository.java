package ar.edu.uai.tfi.management.domain.repository;

import ar.edu.uai.tfi.management.domain.model.Rol;

import java.util.List;
import java.util.Optional;

public interface RolRepository {
    Rol guardar(Rol rol);
    List<Rol> listar();
    Optional<Rol> buscarPorNombre(String nombre);
}
