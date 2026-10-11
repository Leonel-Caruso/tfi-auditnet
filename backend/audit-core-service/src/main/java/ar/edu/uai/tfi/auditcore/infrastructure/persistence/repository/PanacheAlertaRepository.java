package ar.edu.uai.tfi.auditcore.infrastructure.persistence.repository;

import ar.edu.uai.tfi.auditcore.domain.model.AlertaCritica;
import ar.edu.uai.tfi.auditcore.domain.model.EstadoAlerta;
import ar.edu.uai.tfi.auditcore.domain.model.FiltroAlertas;
import ar.edu.uai.tfi.auditcore.domain.model.PerfilTecnico;
import ar.edu.uai.tfi.auditcore.domain.repository.AlertaRepository;
import ar.edu.uai.tfi.auditcore.infrastructure.persistence.entity.AlertaEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.LockModeType;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

@ApplicationScoped
public class PanacheAlertaRepository implements PanacheRepository<AlertaEntity>, AlertaRepository {

    @Override
    public AlertaCritica guardar(AlertaCritica alerta) {
        AlertaEntity entity = new AlertaEntity();
        entity.hallazgoId = alerta.hallazgoId();
        entity.auditoriaId = alerta.auditoriaId();
        entity.dispositivoId = alerta.dispositivoId();
        entity.organizacionId = alerta.organizacionId();
        entity.severidad = alerta.severidad();
        entity.estado = alerta.estado();
        entity.fechaGeneracion = alerta.fechaGeneracion();
        entity.perfilAsignado = alerta.perfilAsignado();
        entity.comentario = alerta.comentario();
        entity.fechaEstado = alerta.fechaEstado();
        entity.usuarioEstado = alerta.usuarioEstado();
        persist(entity);
        flush();
        return convertir(entity);
    }

    @Override
    public Optional<AlertaCritica> buscarPorId(Long id) {
        return findByIdOptional(id).map(PanacheAlertaRepository::convertir);
    }

    @Override
    public Optional<AlertaCritica> buscarPorIdYOrganizacion(Long id, Long organizacionId) {
        return find("id = ?1 and organizacionId = ?2", id, organizacionId)
                .firstResultOptional().map(PanacheAlertaRepository::convertir);
    }

    @Override
    public Optional<AlertaCritica> buscarPorHallazgo(Long hallazgoId) {
        return find("hallazgoId = ?1", hallazgoId).firstResultOptional().map(PanacheAlertaRepository::convertir);
    }

    @Override
    public List<AlertaCritica> buscar(FiltroAlertas filtro) {
        List<String> condiciones = new ArrayList<>();
        Map<String, Object> parametros = new HashMap<>();
        if (filtro.organizacionId() != null) {
            condiciones.add("organizacionId = :organizacion");
            parametros.put("organizacion", filtro.organizacionId());
        }
        if (filtro.estado() != null) {
            condiciones.add("estado = :estado");
            parametros.put("estado", filtro.estado());
        } else if (filtro.soloVigentes()) {
            condiciones.add("estado <> :cerrada");
            parametros.put("cerrada", EstadoAlerta.CERRADA);
        }
        if (filtro.severidad() != null) {
            condiciones.add("severidad = :severidad");
            parametros.put("severidad", filtro.severidad());
        }
        if (filtro.dispositivoId() != null) {
            condiciones.add("dispositivoId = :dispositivo");
            parametros.put("dispositivo", filtro.dispositivoId());
        }
        String orden = "order by fechaGeneracion desc, id desc";
        String consulta = condiciones.isEmpty() ? orden : String.join(" and ", condiciones) + " " + orden;
        return find(consulta, parametros).list().stream().map(PanacheAlertaRepository::convertir).toList();
    }

    @Override
    public AlertaCritica actualizarEstado(Long id, EstadoAlerta estadoEsperado, EstadoAlerta estado,
                                          PerfilTecnico perfilAsignado, String comentario, String usuario,
                                          Instant fecha) {
        AlertaEntity entity = findByIdOptional(id)
                .orElseThrow(() -> new NoSuchElementException("No existe la alerta solicitada."));
        // SELECT ... FOR UPDATE, igual que en hallazgos: no se pisa un cambio hecho por otro usuario.
        getEntityManager().refresh(entity, LockModeType.PESSIMISTIC_WRITE);
        if (estadoEsperado != null && entity.estado != estadoEsperado) {
            throw new IllegalStateException("Otro usuario cambió la alerta a " + entity.estado
                    + " mientras tanto. Actualizá la pantalla y volvé a intentar.");
        }
        entity.estado = estado;
        entity.perfilAsignado = perfilAsignado;
        entity.comentario = comentario;
        entity.usuarioEstado = usuario;
        entity.fechaEstado = fecha;
        flush();
        return convertir(entity);
    }

    private static AlertaCritica convertir(AlertaEntity entity) {
        return new AlertaCritica(
                entity.id,
                entity.hallazgoId,
                entity.auditoriaId,
                entity.dispositivoId,
                entity.organizacionId,
                entity.severidad,
                entity.estado,
                entity.fechaGeneracion,
                entity.perfilAsignado,
                entity.comentario,
                entity.fechaEstado,
                entity.usuarioEstado
        );
    }
}
