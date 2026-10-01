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
        OrganizacionReferenciaEntity organizacion = getEntityManager().find(
                OrganizacionReferenciaEntity.class,
                dispositivo.organizacionId()
        );
        if (organizacion == null) {
            throw new NoSuchElementException("La organización seleccionada no existe.");
        }

        DispositivoEntity entity = new DispositivoEntity();
        entity.organizacion = organizacion;
        copiarDatos(dispositivo, entity);

        persist(entity);
        flush();

        return convertirADominio(entity);
    }

    @Override
    public DispositivoRed actualizar(DispositivoRed dispositivo) {
        DispositivoEntity entity = findByIdOptional(dispositivo.id())
                .orElseThrow(() -> new NoSuchElementException("No existe el dispositivo solicitado."));
        copiarDatos(dispositivo, entity);
        flush();
        return convertirADominio(entity);
    }

    /** Copia los datos editables validando tipo y sede (la organización no cambia). */
    private void copiarDatos(DispositivoRed dispositivo, DispositivoEntity entity) {
        TipoDispositivoEntity tipo = getEntityManager().find(
                TipoDispositivoEntity.class,
                dispositivo.tipoDispositivoId()
        );
        boolean mismoTipo = entity.tipoDispositivo != null && entity.tipoDispositivo.id.equals(dispositivo.tipoDispositivoId());
        // Un tipo dado de baja no se puede asignar, pero el dispositivo que ya lo tiene se puede editar o dar de baja.
        if (tipo == null || (!tipo.activo && !mismoTipo)) {
            throw new NoSuchElementException("El tipo de dispositivo seleccionado no existe o está inactivo.");
        }

        SedeReferenciaEntity sede = null;
        if (dispositivo.sedeId() != null) {
            sede = getEntityManager().find(SedeReferenciaEntity.class, dispositivo.sedeId());
            if (sede == null) {
                throw new NoSuchElementException("La sede seleccionada no existe.");
            }
            if (!entity.organizacion.id.equals(sede.organizacionId)) {
                throw new IllegalArgumentException("La sede seleccionada no pertenece a la organización del dispositivo.");
            }
        }

        entity.nombre = dispositivo.nombre();
        entity.identificador = dispositivo.identificador();
        entity.tipoDispositivo = tipo;
        entity.fabricante = dispositivo.fabricante();
        entity.sede = sede;
        entity.criticidad = dispositivo.criticidad();
        entity.estado = dispositivo.estado();
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
    public boolean existeIdentificadorEnOrganizacion(String identificador, Long organizacionId, Long excluirId) {
        if (excluirId == null) {
            return count("lower(identificador) = ?1 and organizacion.id = ?2",
                    identificador.toLowerCase(), organizacionId) > 0;
        }
        return count("lower(identificador) = ?1 and organizacion.id = ?2 and id <> ?3",
                identificador.toLowerCase(), organizacionId, excluirId) > 0;
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
