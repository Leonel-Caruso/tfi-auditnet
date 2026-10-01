package ar.edu.uai.tfi.auditcore.domain.repository;

import ar.edu.uai.tfi.auditcore.domain.model.DispositivoRed;

import java.util.List;
import java.util.Optional;

public interface DispositivoRepository {

    DispositivoRed guardar(DispositivoRed dispositivo);

    /** Actualiza un dispositivo existente (todos sus campos salvo id y organización). */
    DispositivoRed actualizar(DispositivoRed dispositivo);

    List<DispositivoRed> listar();

    List<DispositivoRed> listarPorOrganizacion(Long organizacionId);

    Optional<DispositivoRed> buscarPorId(Long id);

    Optional<DispositivoRed> buscarPorIdYOrganizacion(Long id, Long organizacionId);

    /**
     * Indica si en la organización ya existe otro dispositivo con ese identificador (sin distinguir mayúsculas).
     *
     * @param excluirId id a ignorar (el propio dispositivo al modificarlo); null en un alta
     */
    boolean existeIdentificadorEnOrganizacion(String identificador, Long organizacionId, Long excluirId);
}
