package ar.edu.uai.tfi.auditcore.domain.repository;

import ar.edu.uai.tfi.auditcore.domain.model.ReglaBaseline;

import java.util.List;
import java.util.Optional;

public interface ReglaBaselineRepository {
    ReglaBaseline guardar(ReglaBaseline regla);
    List<ReglaBaseline> listar();
    List<ReglaBaseline> listarPorOrganizacion(Long organizacionId);
    List<ReglaBaseline> listarPorBaseline(Long baselineId);
    List<ReglaBaseline> listarPorBaselineYOrganizacion(Long baselineId, Long organizacionId);
    Optional<ReglaBaseline> buscarPorId(Long id);
    Optional<ReglaBaseline> buscarPorIdYOrganizacion(Long id, Long organizacionId);
    boolean existeCodigoEnBaseline(String codigo, Long baselineId);
    long contarPorBaseline(Long baselineId);
}
