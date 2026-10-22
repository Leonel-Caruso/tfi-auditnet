package ar.edu.uai.tfi.management.domain.repository;

import ar.edu.uai.tfi.management.domain.model.Plan;

import java.util.List;
import java.util.Optional;

public interface PlanRepository {

    /** Catálogo en el orden en que se muestra. */
    List<Plan> listar();

    Optional<Plan> buscarPorCodigo(String codigo);

    /** Actualiza el precio de referencia y las condiciones (los límites del catálogo son fijos). */
    Plan actualizar(Plan plan);
}
