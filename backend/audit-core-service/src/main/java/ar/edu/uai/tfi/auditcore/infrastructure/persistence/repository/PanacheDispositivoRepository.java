package ar.edu.uai.tfi.auditcore.infrastructure.persistence.repository;

import ar.edu.uai.tfi.auditcore.domain.model.DispositivoRed;
import ar.edu.uai.tfi.auditcore.domain.repository.DispositivoRepository;
import ar.edu.uai.tfi.auditcore.infrastructure.persistence.entity.DispositivoEntity;
import ar.edu.uai.tfi.auditcore.infrastructure.persistence.entity.OrganizacionReferenciaEntity;
import ar.edu.uai.tfi.auditcore.infrastructure.persistence.entity.SedeReferenciaEntity;
import ar.edu.uai.tfi.auditcore.infrastructure.persistence.entity.TipoDispositivoEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@ApplicationScoped
public class PanacheDispositivoRepository
        implements PanacheRepository<DispositivoEntity>, DispositivoRepository {

    @Override
    public DispositivoRed guardar(DispositivoRed dispositivo) {
        TipoDispositivoEntity tipo = getEntityManager().find(
                TipoDispositivoEntity.class,
                dispositivo.tipoDispositivoId()
        );
        if (tipo == null || !tipo.activo) {
            throw new NoSuchElementException("El tipo de dispositivo seleccionado no existe o está inactivo.");
        }

        OrganizacionReferenciaEntity organizacion = getEntityManager().find(
                OrganizacionReferenciaEntity.class,
                dispositivo.organizacionId()
        );
        if (organizacion == null) {
            throw new NoSuchElementException("La organización seleccionada no existe.");
        }

        SedeReferenciaEntity sede = null;
        if (dispositivo.sedeId() != null) {
            sede = getEntityManager().find(SedeReferenciaEntity.class, dispositivo.sedeId());
            if (sede == null) {
                throw new NoSuchElementException("La sede seleccionada no existe.");
            }
            if (!dispositivo.organizacionId().equals(sede.organizacionId)) {
                throw new IllegalArgumentException("La sede seleccionada no pertenece a la organización indicada.");
            }
        }

        DispositivoEntity entity = new DispositivoEntity();
        entity.nombre = dispositivo.nombre();
        entity.identificador = dispositivo.identificador();
        entity.tipoDispositivo = tipo;
        entity.fabricante = dispositivo.fabricante();
        entity.organizacion = organizacion;
        entity.sede = sede;
        entity.criticidad = dispositivo.criticidad();
        entity.estado = dispositivo.estado();

        persist(entity);
        flush();

        return convertirADominio(entity);
    }

    @Override
    public List<DispositivoRed> listar() {
        return find("order by identificador").list()
                .stream()
                .map(this::convertirADominio)
                .toList();
    }

    @Override
    public List<DispositivoRed> listarPorOrganizacion(Long organizacionId) {
        return find("organizacion.id = ?1 order by identificador", organizacionId).list()
                .stream()
                .map(this::convertirADominio)
                .toList();
    }

    @Override
    public Optional<DispositivoRed> buscarPorId(Long id) {
        return findByIdOptional(id).map(this::convertirADominio);
    }

    @Override
    public Optional<DispositivoRed> buscarPorIdYOrganizacion(Long id, Long organizacionId) {
        return find("id = ?1 and organizacion.id = ?2", id, organizacionId)
                .firstResultOptional()
                .map(this::convertirADominio);
    }

    @Override
    public boolean existePorIdentificador(String identificador) {
        return count("lower(identificador) = ?1", identificador.toLowerCase()) > 0;
    }

    private DispositivoRed convertirADominio(DispositivoEntity entity) {
        return new DispositivoRed(
                entity.id,
                entity.nombre,
                entity.identificador,
                entity.tipoDispositivo.id,
                entity.fabricante,
                entity.organizacion.id,
                entity.sede == null ? null : entity.sede.id,
                entity.criticidad,
                entity.estado
        );
    }
}
