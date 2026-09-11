package ar.edu.uai.tfi.management.domain.repository;

import ar.edu.uai.tfi.management.domain.model.OrganizacionCliente;

import java.util.List;
import java.util.Optional;

public interface OrganizacionClienteRepository {
    OrganizacionCliente guardar(OrganizacionCliente organizacion);
    List<OrganizacionCliente> listar();
    Optional<OrganizacionCliente> buscarPorId(Long id);
    Optional<OrganizacionCliente> buscarPorIdentificador(String identificador);
    Optional<OrganizacionCliente> buscarPorRazonSocial(String razonSocial);
}
