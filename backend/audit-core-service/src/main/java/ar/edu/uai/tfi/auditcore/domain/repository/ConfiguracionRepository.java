package ar.edu.uai.tfi.auditcore.domain.repository;

import ar.edu.uai.tfi.auditcore.domain.model.ConfiguracionDispositivo;

import java.util.List;
import java.util.Optional;

public interface ConfiguracionRepository {

    ConfiguracionDispositivo guardar(ConfiguracionDispositivo configuracion);

    List<ConfiguracionDispositivo> listarAuditables();

    List<ConfiguracionDispositivo> listarAuditablesPorOrganizacion(Long organizacionId);

    Optional<ConfiguracionDispositivo> buscarPorId(Long id);

    Optional<ConfiguracionDispositivo> buscarPorIdYOrganizacion(Long id, Long organizacionId);

    int siguienteVersionParaDispositivo(Long dispositivoId);
}
