package ar.edu.uai.tfi.auditcore.domain.repository;

import ar.edu.uai.tfi.auditcore.domain.model.AlertaCritica;
import ar.edu.uai.tfi.auditcore.domain.model.EstadoAlerta;
import ar.edu.uai.tfi.auditcore.domain.model.FiltroAlertas;
import ar.edu.uai.tfi.auditcore.domain.model.PerfilTecnico;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface AlertaRepository {

    AlertaCritica guardar(AlertaCritica alerta);

    Optional<AlertaCritica> buscarPorId(Long id);

    Optional<AlertaCritica> buscarPorIdYOrganizacion(Long id, Long organizacionId);

    Optional<AlertaCritica> buscarPorHallazgo(Long hallazgoId);

    /** Alertas que cumplen los criterios, de la más nueva a la más vieja. */
    List<AlertaCritica> buscar(FiltroAlertas filtro);

    /**
     * Bloquea la fila y verifica que el estado siga siendo {@code estadoEsperado}; si otro usuario lo cambió, lanza
     * IllegalStateException (409). Con null no verifica: lo usa la sincronización con el hallazgo.
     */
    AlertaCritica actualizarEstado(Long id, EstadoAlerta estadoEsperado, EstadoAlerta estado,
                                   PerfilTecnico perfilAsignado, String comentario, String usuario, Instant fecha);
}
