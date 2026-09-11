package ar.edu.uai.tfi.auditcore.domain.repository;

import ar.edu.uai.tfi.auditcore.domain.model.BaselineConfiguracion;
import ar.edu.uai.tfi.auditcore.domain.model.EstadoBaseline;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public interface BaselineRepository {
    BaselineConfiguracion guardar(BaselineConfiguracion baseline);
    List<BaselineConfiguracion> listar();
    List<BaselineConfiguracion> listarPorOrganizacion(Long organizacionId);
    Optional<BaselineConfiguracion> buscarPorId(Long id);
    Optional<BaselineConfiguracion> buscarPorIdYOrganizacion(Long id, Long organizacionId);
    boolean existeNombreEnOrganizacion(String nombre, Long organizacionId);

    default Optional<BaselineConfiguracion> buscarActivaAplicable(Long organizacionId, Long tipoDispositivoId) {
        return listarPorOrganizacion(organizacionId).stream()
                .filter(b -> b.estado() == EstadoBaseline.ACTIVO)
                .filter(b -> b.tipoDispositivoId().equals(tipoDispositivoId))
                .max(Comparator
                        .comparing(BaselineConfiguracion::version)
                        .thenComparing(BaselineConfiguracion::id, Comparator.nullsFirst(Long::compareTo)));
    }
}
