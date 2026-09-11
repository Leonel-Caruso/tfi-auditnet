package ar.edu.uai.tfi.management.domain.repository;

import ar.edu.uai.tfi.management.domain.model.Sede;

import java.util.List;
import java.util.Optional;

public interface SedeRepository {
    Sede guardar(Sede sede);
    List<Sede> listarPorOrganizacion(Long organizacionId);
    Optional<Sede> buscarPorId(Long id);
    Optional<Sede> buscarPorOrganizacionYNombre(Long organizacionId, String nombre);
}
