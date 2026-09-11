package ar.edu.uai.tfi.management.infrastructure.persistence.repository;

import ar.edu.uai.tfi.management.domain.model.OrganizacionCliente;
import ar.edu.uai.tfi.management.domain.repository.OrganizacionClienteRepository;
import ar.edu.uai.tfi.management.infrastructure.persistence.entity.OrganizacionClienteEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@ApplicationScoped
public class PanacheOrganizacionClienteRepository
        implements PanacheRepository<OrganizacionClienteEntity>, OrganizacionClienteRepository {

    @Override
    public OrganizacionCliente guardar(OrganizacionCliente organizacion) {
        OrganizacionClienteEntity entity = new OrganizacionClienteEntity();
        entity.identificador = organizacion.identificador();
        entity.razonSocial = organizacion.razonSocial();
        entity.estado = organizacion.estado();
        persist(entity);
        return aDominio(entity);
    }

    @Override
    public List<OrganizacionCliente> listar() {
        return find("order by razonSocial").list().stream().map(this::aDominio).toList();
    }

    @Override
    public Optional<OrganizacionCliente> buscarPorId(Long id) {
        return findByIdOptional(id).map(this::aDominio);
    }

    @Override
    public Optional<OrganizacionCliente> buscarPorIdentificador(String identificador) {
        return find("upper(identificador) = ?1", identificador.trim().toUpperCase(Locale.ROOT))
                .firstResultOptional().map(this::aDominio);
    }

    @Override
    public Optional<OrganizacionCliente> buscarPorRazonSocial(String razonSocial) {
        return find("lower(razonSocial) = ?1", razonSocial.trim().toLowerCase(Locale.ROOT))
                .firstResultOptional().map(this::aDominio);
    }

    private OrganizacionCliente aDominio(OrganizacionClienteEntity entity) {
        return new OrganizacionCliente(entity.id, entity.identificador, entity.razonSocial, entity.estado);
    }
}
