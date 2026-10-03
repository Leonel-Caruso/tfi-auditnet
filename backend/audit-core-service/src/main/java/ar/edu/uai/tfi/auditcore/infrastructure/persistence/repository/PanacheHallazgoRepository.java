package ar.edu.uai.tfi.auditcore.infrastructure.persistence.repository;

import ar.edu.uai.tfi.auditcore.domain.model.EstadoHallazgo;
import ar.edu.uai.tfi.auditcore.domain.model.FiltroHallazgos;
import ar.edu.uai.tfi.auditcore.domain.model.HallazgoAuditoria;
import ar.edu.uai.tfi.auditcore.domain.repository.HallazgoRepository;
import ar.edu.uai.tfi.auditcore.infrastructure.persistence.entity.*;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.persistence.LockModeType;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

@ApplicationScoped
public class PanacheHallazgoRepository implements PanacheRepository<HallazgoEntity>, HallazgoRepository {

    @Override
    public HallazgoAuditoria guardar(HallazgoAuditoria hallazgo) {
        AuditoriaEntity auditoria = getEntityManager().find(AuditoriaEntity.class, hallazgo.auditoriaId());
        ReglaBaselineEntity regla = getEntityManager().find(ReglaBaselineEntity.class, hallazgo.reglaId());
        DispositivoEntity dispositivo = getEntityManager().find(DispositivoEntity.class, hallazgo.dispositivoId());
        BaselineEntity baseline = getEntityManager().find(BaselineEntity.class, hallazgo.baselineId());
        OrganizacionReferenciaEntity organizacion = getEntityManager().find(
                OrganizacionReferenciaEntity.class,
                hallazgo.organizacionId()
        );

        if (auditoria == null || regla == null || dispositivo == null || baseline == null || organizacion == null) {
            throw new NoSuchElementException("No se pudo resolver el contexto persistente del hallazgo.");
        }

        HallazgoEntity entity = new HallazgoEntity();
        entity.auditoria = auditoria;
        entity.regla = regla;
        entity.dispositivo = dispositivo;
        entity.baseline = baseline;
        entity.organizacion = organizacion;
        entity.codigoRegla = hallazgo.codigoRegla();
        entity.nombreRegla = hallazgo.nombreRegla();
        entity.tipoRegla = hallazgo.tipoRegla();
        entity.patron = hallazgo.patron();
        entity.severidad = hallazgo.severidad();
        entity.evidencia = hallazgo.evidencia();
        entity.recomendacion = hallazgo.recomendacion();
        entity.impacto = hallazgo.impacto();
        entity.estado = hallazgo.estado();
        entity.fechaDeteccion = hallazgo.fechaDeteccion();
        entity.fechaEstado = hallazgo.fechaEstado();
        entity.usuarioEstado = hallazgo.usuarioEstado();

        persist(entity);
        flush();
        return convertir(entity);
    }

    @Override
    public List<HallazgoAuditoria> listar() {
        return find("order by fechaDeteccion desc, id desc").list().stream().map(this::convertir).toList();
    }

    @Override
    public List<HallazgoAuditoria> listarPorOrganizacion(Long organizacionId) {
        return find("organizacion.id = ?1 order by fechaDeteccion desc, id desc", organizacionId)
                .list().stream().map(this::convertir).toList();
    }

    @Override
    public List<HallazgoAuditoria> listarPorAuditoria(Long auditoriaId) {
        return find("auditoria.id = ?1 order by id", auditoriaId).list().stream().map(this::convertir).toList();
    }

    @Override
    public Optional<HallazgoAuditoria> buscarPorId(Long id) {
        return findByIdOptional(id).map(this::convertir);
    }

    @Override
    public Optional<HallazgoAuditoria> buscarPorIdYOrganizacion(Long id, Long organizacionId) {
        return find("id = ?1 and organizacion.id = ?2", id, organizacionId)
                .firstResultOptional().map(this::convertir);
    }

    @Override
    public List<HallazgoAuditoria> buscar(FiltroHallazgos filtro) {
        List<String> condiciones = new ArrayList<>();
        Map<String, Object> parametros = new HashMap<>();
        agregar(condiciones, parametros, "organizacion.id = :organizacion", "organizacion", filtro.organizacionId());
        agregar(condiciones, parametros, "estado = :estado", "estado", filtro.estado());
        agregar(condiciones, parametros, "severidad = :severidad", "severidad", filtro.severidad());
        agregar(condiciones, parametros, "dispositivo.id = :dispositivo", "dispositivo", filtro.dispositivoId());
        agregar(condiciones, parametros, "baseline.id = :baseline", "baseline", filtro.baselineId());
        agregar(condiciones, parametros, "auditoria.id = :auditoria", "auditoria", filtro.auditoriaId());
        agregar(condiciones, parametros, "fechaDeteccion >= :desde", "desde", filtro.desde());
        agregar(condiciones, parametros, "fechaDeteccion <= :hasta", "hasta", filtro.hasta());

        String orden = "order by fechaDeteccion desc, id desc";
        String consulta = condiciones.isEmpty() ? orden : String.join(" and ", condiciones) + " " + orden;
        return find(consulta, parametros).list().stream().map(this::convertir).toList();
    }

    @Override
    public HallazgoAuditoria actualizarEstado(Long id, EstadoHallazgo estadoEsperado, EstadoHallazgo estado,
                                              String usuario, Instant fecha) {
        HallazgoEntity entity = findByIdOptional(id)
                .orElseThrow(() -> new NoSuchElementException("No existe el hallazgo solicitado."));
        // SELECT ... FOR UPDATE: relee el estado actual y espera si otra transacción lo está cambiando.
        getEntityManager().refresh(entity, LockModeType.PESSIMISTIC_WRITE);
        if (entity.estado != estadoEsperado) {
            throw new IllegalStateException("Otro usuario cambió el hallazgo a " + entity.estado
                    + " mientras tanto. Actualizá la pantalla y volvé a intentar.");
        }
        entity.estado = estado;
        entity.usuarioEstado = usuario;
        entity.fechaEstado = fecha;
        flush();
        return convertir(entity);
    }

    private static void agregar(List<String> condiciones, Map<String, Object> parametros,
                                String condicion, String nombre, Object valor) {
        if (valor != null) {
            condiciones.add(condicion);
            parametros.put(nombre, valor);
        }
    }

    private HallazgoAuditoria convertir(HallazgoEntity entity) {
        return new HallazgoAuditoria(
                entity.id,
                entity.auditoria.id,
                entity.regla.id,
                entity.dispositivo.id,
                entity.baseline.id,
                entity.organizacion.id,
                entity.codigoRegla,
                entity.nombreRegla,
                entity.tipoRegla,
                entity.patron,
                entity.severidad,
                entity.evidencia,
                entity.recomendacion,
                entity.impacto,
                entity.estado,
                entity.fechaDeteccion,
                entity.fechaEstado,
                entity.usuarioEstado
        );
    }
}
