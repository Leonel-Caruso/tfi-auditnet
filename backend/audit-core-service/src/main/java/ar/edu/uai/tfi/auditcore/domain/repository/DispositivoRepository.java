package ar.edu.uai.tfi.auditcore.domain.repository;

import ar.edu.uai.tfi.auditcore.domain.model.DispositivoRed;

import java.util.List;
import java.util.Optional;

public interface DispositivoRepository {

    DispositivoRed guardar(DispositivoRed dispositivo);

    List<DispositivoRed> listar();

    List<DispositivoRed> listarPorOrganizacion(Long organizacionId);

    Optional<DispositivoRed> buscarPorId(Long id);

    Optional<DispositivoRed> buscarPorIdYOrganizacion(Long id, Long organizacionId);

    boolean existePorIdentificador(String identificador);
}
