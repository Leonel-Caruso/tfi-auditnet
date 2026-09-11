package ar.edu.uai.tfi.auditcore.domain.repository;

import ar.edu.uai.tfi.auditcore.domain.model.HallazgoAuditoria;

import java.util.List;
import java.util.Optional;

public interface HallazgoRepository {

    HallazgoAuditoria guardar(HallazgoAuditoria hallazgo);

    List<HallazgoAuditoria> listar();

    List<HallazgoAuditoria> listarPorOrganizacion(Long organizacionId);

    List<HallazgoAuditoria> listarPorAuditoria(Long auditoriaId);

    Optional<HallazgoAuditoria> buscarPorId(Long id);

    Optional<HallazgoAuditoria> buscarPorIdYOrganizacion(Long id, Long organizacionId);
}
