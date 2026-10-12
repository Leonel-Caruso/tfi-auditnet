package ar.edu.uai.tfi.auditcore.infrastructure.persistence.repository;

import ar.edu.uai.tfi.auditcore.domain.model.ContenidoReporte;
import ar.edu.uai.tfi.auditcore.domain.model.CriteriosReporte;
import ar.edu.uai.tfi.auditcore.domain.model.ReporteAuditoria;
import ar.edu.uai.tfi.auditcore.domain.repository.ReporteRepository;
import ar.edu.uai.tfi.auditcore.infrastructure.persistence.entity.ReporteEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class PanacheReporteRepository implements PanacheRepository<ReporteEntity>, ReporteRepository {

    private final ObjectMapper objectMapper;

    public PanacheReporteRepository(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public ReporteAuditoria guardar(ReporteAuditoria reporte, List<Long> auditoriaIds) {
        ReporteEntity entity = new ReporteEntity();
        entity.organizacionId = reporte.organizacionId();
        entity.titulo = reporte.titulo();
        entity.criterios = aJson(reporte.criterios());
        entity.contenido = aJson(reporte.contenido());
        entity.totalAuditorias = reporte.totalAuditorias();
        entity.totalHallazgos = reporte.totalHallazgos();
        entity.generadoPor = reporte.generadoPor();
        entity.fechaGeneracion = reporte.fechaGeneracion();
        persist(entity);
        flush();
        // Referencia a cada auditoría usada como fuente.
        for (Long auditoriaId : new LinkedHashSet<>(auditoriaIds)) {
            getEntityManager()
                    .createNativeQuery("INSERT INTO reportes_auditoria_auditorias (id_reporte, id_auditoria) VALUES (?1, ?2)")
                    .setParameter(1, entity.id)
                    .setParameter(2, auditoriaId)
                    .executeUpdate();
        }
        return convertir(entity, true);
    }

    @Override
    public Optional<ReporteAuditoria> buscarPorId(Long id) {
        return findByIdOptional(id).map(entity -> convertir(entity, true));
    }

    @Override
    public List<ReporteAuditoria> listar(Long organizacionId) {
        String orden = "order by fechaGeneracion desc, id desc";
        List<ReporteEntity> entidades = organizacionId == null
                ? find(orden).list()
                : find("organizacionId = ?1 " + orden, organizacionId).list();
        return entidades.stream().map(entity -> convertir(entity, false)).toList();
    }

    private ReporteAuditoria convertir(ReporteEntity entity, boolean conContenido) {
        return new ReporteAuditoria(
                entity.id,
                entity.organizacionId,
                entity.titulo,
                desdeJson(entity.criterios, CriteriosReporte.class),
                conContenido ? desdeJson(entity.contenido, ContenidoReporte.class) : null,
                entity.totalAuditorias,
                entity.totalHallazgos,
                entity.generadoPor,
                entity.fechaGeneracion
        );
    }

    private String aJson(Object valor) {
        try {
            return objectMapper.writeValueAsString(valor);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("No se pudo guardar el contenido del reporte.", exception);
        }
    }

    private <T> T desdeJson(String json, Class<T> tipo) {
        try {
            return objectMapper.readValue(json, tipo);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("No se pudo leer el contenido del reporte.", exception);
        }
    }
}
