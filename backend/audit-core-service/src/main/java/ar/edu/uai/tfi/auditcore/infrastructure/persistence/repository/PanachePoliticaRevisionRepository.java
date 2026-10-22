package ar.edu.uai.tfi.auditcore.infrastructure.persistence.repository;

import ar.edu.uai.tfi.auditcore.domain.model.PoliticaRevision;
import ar.edu.uai.tfi.auditcore.domain.repository.PoliticaRevisionRepository;
import ar.edu.uai.tfi.auditcore.infrastructure.persistence.entity.PoliticaRevisionEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;

@ApplicationScoped
public class PanachePoliticaRevisionRepository
        implements PanacheRepository<PoliticaRevisionEntity>, PoliticaRevisionRepository {

    @Override
    public Optional<PoliticaRevision> buscar(Long organizacionId) {
        return findByIdOptional(organizacionId).map(PanachePoliticaRevisionRepository::convertir);
    }

    @Override
    public PoliticaRevision guardar(PoliticaRevision politica) {
        PoliticaRevisionEntity existente = findById(politica.organizacionId());
        PoliticaRevisionEntity entity = existente == null ? new PoliticaRevisionEntity() : existente;
        entity.organizacionId = politica.organizacionId();
        entity.diasCritica = politica.diasCritica();
        entity.diasAlta = politica.diasAlta();
        entity.diasMedia = politica.diasMedia();
        entity.diasBaja = politica.diasBaja();
        entity.diasReauditoria = politica.diasReauditoria();
        entity.diasConfiguracion = politica.diasConfiguracion();
        entity.reauditoriaAutomatica = politica.reauditoriaAutomatica();
        entity.modificadaEn = politica.modificadaEn();
        entity.modificadaPor = politica.modificadaPor();
        if (existente == null) {
            persist(entity);
        }
        return convertir(entity);
    }

    private static PoliticaRevision convertir(PoliticaRevisionEntity entity) {
        return new PoliticaRevision(entity.organizacionId, entity.diasCritica, entity.diasAlta, entity.diasMedia,
                entity.diasBaja, entity.diasReauditoria, entity.diasConfiguracion, entity.reauditoriaAutomatica,
                entity.modificadaEn, entity.modificadaPor);
    }
}
