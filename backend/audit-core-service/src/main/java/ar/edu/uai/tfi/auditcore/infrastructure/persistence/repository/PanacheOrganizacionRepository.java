package ar.edu.uai.tfi.auditcore.infrastructure.persistence.repository;

import ar.edu.uai.tfi.auditcore.domain.repository.OrganizacionRepository;
import ar.edu.uai.tfi.auditcore.infrastructure.persistence.entity.OrganizacionReferenciaEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

/** Solo lectura: las organizaciones las administra management-service. */
@ApplicationScoped
public class PanacheOrganizacionRepository
        implements PanacheRepository<OrganizacionReferenciaEntity>, OrganizacionRepository {

    @Override
    public boolean existe(Long organizacionId) {
        return organizacionId != null && findByIdOptional(organizacionId).isPresent();
    }
}
