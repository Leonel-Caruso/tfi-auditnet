package ar.edu.uai.tfi.auditcore.domain.repository;

import ar.edu.uai.tfi.auditcore.domain.model.BaselineConfiguracion;
import ar.edu.uai.tfi.auditcore.domain.model.EstadoBaseline;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public interface BaselineRepository {
    BaselineConfiguracion guardar(BaselineConfiguracion baseline);

    /** Actualiza la descripción y el estado de una baseline existente. */
    BaselineConfiguracion actualizar(BaselineConfiguracion baseline);

    List<BaselineConfiguracion> listar();
    List<BaselineConfiguracion> listarPorOrganizacion(Long organizacionId);
    Optional<BaselineConfiguracion> buscarPorId(Long id);
    Optional<BaselineConfiguracion> buscarPorIdYOrganizacion(Long id, Long organizacionId);
    boolean existeNombreEnOrganizacion(String nombre, Long organizacionId);

    /** Baselines activas de un alcance (organización + tipo de dispositivo). */
    default List<BaselineConfiguracion> listarActivasDelAlcance(Long organizacionId, Long tipoDispositivoId) {
        return listarPorOrganizacion(organizacionId).stream()
                .filter(b -> b.estado() == EstadoBaseline.ACTIVO)
                .filter(b -> b.tipoDispositivoId().equals(tipoDispositivoId))
                .toList();
    }

    /** Todas las versiones de una baseline (mismo nombre en la organización), de la más nueva a la más vieja. */
    default List<BaselineConfiguracion> listarVersiones(Long organizacionId, String nombre) {
        return listarPorOrganizacion(organizacionId).stream()
                .filter(b -> b.nombre().equalsIgnoreCase(nombre))
                .sorted(Comparator.comparing(BaselineConfiguracion::version).reversed())
                .toList();
    }

    default Optional<BaselineConfiguracion> buscarActivaAplicable(Long organizacionId, Long tipoDispositivoId) {
        return listarActivasDelAlcance(organizacionId, tipoDispositivoId).stream()
                .max(Comparator
                        .comparing(BaselineConfiguracion::version)
                        .thenComparing(BaselineConfiguracion::id, Comparator.nullsFirst(Long::compareTo)));
    }
}
