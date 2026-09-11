package ar.edu.uai.tfi.auditcore.domain.repository;

import ar.edu.uai.tfi.auditcore.domain.model.AuditoriaConfiguracion;

import java.util.List;
import java.util.Optional;

public interface AuditoriaRepository {

    AuditoriaConfiguracion guardar(AuditoriaConfiguracion auditoria);

    List<AuditoriaConfiguracion> listarHistorial();

    List<AuditoriaConfiguracion> listarHistorialPorOrganizacion(Long organizacionId);

    Optional<AuditoriaConfiguracion> buscarPorId(Long id);

    Optional<AuditoriaConfiguracion> buscarPorIdYOrganizacion(Long id, Long organizacionId);
}
