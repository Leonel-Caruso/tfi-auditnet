package ar.edu.uai.tfi.management.infrastructure.persistence.repository;

import ar.edu.uai.tfi.management.domain.model.FiltroBitacoraTransacciones;
import ar.edu.uai.tfi.management.domain.model.RegistroTransaccion;
import ar.edu.uai.tfi.management.domain.repository.BitacoraTransaccionesRepository;
import ar.edu.uai.tfi.management.infrastructure.persistence.entity.BitacoraTransaccionEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@ApplicationScoped
public class PanacheBitacoraTransaccionesRepository
        implements PanacheRepository<BitacoraTransaccionEntity>, BitacoraTransaccionesRepository {

    @Override
    public RegistroTransaccion guardar(RegistroTransaccion registro) {
        BitacoraTransaccionEntity entity = new BitacoraTransaccionEntity();
        entity.fecha = registro.fecha();
        entity.servicio = registro.servicio();
        entity.entidad = registro.entidad();
        entity.entidadId = registro.entidadId();
        entity.operacion = registro.operacion();
        entity.actor = registro.actor();
        entity.organizacionId = registro.organizacionId();
        entity.valorAnterior = registro.valorAnterior();
        entity.valorNuevo = registro.valorNuevo();
        entity.correlacion = registro.correlacion();
        entity.detalle = registro.detalle();
        persist(entity);
        return aDominio(entity);
    }

    @Override
    public List<RegistroTransaccion> buscar(FiltroBitacoraTransacciones filtro) {
        List<String> condiciones = new ArrayList<>();
        Map<String, Object> parametros = new HashMap<>();

        if (filtro.entidad() != null) {
            condiciones.add("entidad = :entidad");
            parametros.put("entidad", filtro.entidad());
        }
        if (filtro.entidadId() != null) {
            condiciones.add("entidadId = :entidadId");
            parametros.put("entidadId", filtro.entidadId());
        }
        if (filtro.operacion() != null) {
            condiciones.add("operacion = :operacion");
            parametros.put("operacion", filtro.operacion());
        }
        if (filtro.actor() != null) {
            condiciones.add("lower(actor) like :actor");
            parametros.put("actor", "%" + filtro.actor().toLowerCase(Locale.ROOT) + "%");
        }
        if (filtro.organizacionId() != null) {
            condiciones.add("organizacionId = :organizacionId");
            parametros.put("organizacionId", filtro.organizacionId());
        }
        if (filtro.desde() != null) {
            condiciones.add("fecha >= :desde");
            parametros.put("desde", filtro.desde());
        }
        if (filtro.hasta() != null) {
            condiciones.add("fecha <= :hasta");
            parametros.put("hasta", filtro.hasta());
        }

        String consulta = condiciones.isEmpty() ? "1 = 1" : String.join(" and ", condiciones);
        return find(consulta, Sort.descending("fecha", "id"), parametros)
                .page(0, filtro.limite())
                .list()
                .stream()
                .map(this::aDominio)
                .toList();
    }

    private RegistroTransaccion aDominio(BitacoraTransaccionEntity entity) {
        return new RegistroTransaccion(
                entity.id,
                entity.fecha,
                entity.servicio,
                entity.entidad,
                entity.entidadId,
                entity.operacion,
                entity.actor,
                entity.organizacionId,
                entity.valorAnterior,
                entity.valorNuevo,
                entity.correlacion,
                entity.detalle
        );
    }
}
