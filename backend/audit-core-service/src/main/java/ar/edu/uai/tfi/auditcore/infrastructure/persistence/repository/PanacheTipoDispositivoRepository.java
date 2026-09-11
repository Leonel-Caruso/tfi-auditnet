package ar.edu.uai.tfi.auditcore.infrastructure.persistence.repository;

import ar.edu.uai.tfi.auditcore.domain.model.TipoDispositivo;
import ar.edu.uai.tfi.auditcore.domain.repository.TipoDispositivoRepository;
import ar.edu.uai.tfi.auditcore.infrastructure.persistence.entity.TipoDispositivoEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class PanacheTipoDispositivoRepository
        implements PanacheRepository<TipoDispositivoEntity>, TipoDispositivoRepository {

    @Override
    public TipoDispositivo guardar(TipoDispositivo tipoDispositivo) {
        TipoDispositivoEntity entity = new TipoDispositivoEntity();
        entity.nombre = tipoDispositivo.nombre();
        entity.fabricante = tipoDispositivo.fabricante();
        entity.familia = tipoDispositivo.familia();
        entity.activo = tipoDispositivo.activo();

        persist(entity);

        return convertirADominio(entity);
    }

    @Override
    public List<TipoDispositivo> listar() {
        return find("order by id").list()
                .stream()
                .map(this::convertirADominio)
                .toList();
    }

    private TipoDispositivo convertirADominio(TipoDispositivoEntity entity) {
        return new TipoDispositivo(
                entity.id,
                entity.nombre,
                entity.fabricante,
                entity.familia,
                entity.activo
        );
    }
}
