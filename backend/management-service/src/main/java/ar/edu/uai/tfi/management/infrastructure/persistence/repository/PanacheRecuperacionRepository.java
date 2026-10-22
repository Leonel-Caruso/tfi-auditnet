package ar.edu.uai.tfi.management.infrastructure.persistence.repository;

import ar.edu.uai.tfi.management.domain.model.RecuperacionContrasena;
import ar.edu.uai.tfi.management.domain.repository.RecuperacionRepository;
import ar.edu.uai.tfi.management.infrastructure.persistence.entity.RecuperacionEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.LockModeType;

import java.time.Instant;
import java.util.Optional;

@ApplicationScoped
public class PanacheRecuperacionRepository implements PanacheRepository<RecuperacionEntity>, RecuperacionRepository {

    @Override
    public RecuperacionContrasena guardar(RecuperacionContrasena recuperacion) {
        RecuperacionEntity entity = new RecuperacionEntity();
        entity.usuarioId = recuperacion.usuarioId();
        entity.tokenHash = recuperacion.tokenHash();
        entity.creadaEn = recuperacion.creadaEn();
        entity.venceEn = recuperacion.venceEn();
        entity.usadaEn = recuperacion.usadaEn();
        persist(entity);
        return aDominio(entity);
    }

    @Override
    public Optional<RecuperacionContrasena> buscarParaUsar(String tokenHash) {
        return find("tokenHash", tokenHash).withLock(LockModeType.PESSIMISTIC_WRITE).firstResultOptional()
                .map(PanacheRecuperacionRepository::aDominio);
    }

    @Override
    public Optional<Instant> ultimoPedido(Long usuarioId) {
        return find("usuarioId = ?1 order by creadaEn desc", usuarioId).firstResultOptional()
                .map(entity -> entity.creadaEn);
    }

    @Override
    public void anularPendientes(Long usuarioId, Instant ahora) {
        update("usadaEn = ?1 where usuarioId = ?2 and usadaEn is null", ahora, usuarioId);
    }

    private static RecuperacionContrasena aDominio(RecuperacionEntity entity) {
        return new RecuperacionContrasena(entity.id, entity.usuarioId, entity.tokenHash, entity.creadaEn,
                entity.venceEn, entity.usadaEn);
    }
}
