package ar.edu.uai.tfi.management.infrastructure.persistence.repository;

import ar.edu.uai.tfi.management.domain.model.FiltroBitacoraSistema;
import ar.edu.uai.tfi.management.domain.model.RegistroBitacoraSistema;
import ar.edu.uai.tfi.management.domain.repository.BitacoraSistemaRepository;
import ar.edu.uai.tfi.management.infrastructure.persistence.entity.BitacoraSistemaEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@ApplicationScoped
public class PanacheBitacoraSistemaRepository
        implements PanacheRepository<BitacoraSistemaEntity>, BitacoraSistemaRepository {

    @Override
    public RegistroBitacoraSistema guardar(RegistroBitacoraSistema registro) {
        BitacoraSistemaEntity entity = new BitacoraSistemaEntity();
        entity.fecha = registro.fecha();
        entity.tipo = registro.tipo();
        entity.resultado = registro.resultado();
        entity.actor = registro.actor();
        entity.usuarioAfectadoId = registro.usuarioAfectadoId();
        entity.organizacionId = registro.organizacionId();
        entity.origenIp = registro.origenIp();
        entity.userAgent = registro.userAgent();
        entity.correlacion = registro.correlacion();
        entity.detalle = registro.detalle();
        persist(entity);
        return aDominio(entity);
    }

    @Override
    public List<RegistroBitacoraSistema> buscar(FiltroBitacoraSistema filtro) {
        List<String> condiciones = new ArrayList<>();
        Map<String, Object> parametros = new HashMap<>();

        if (filtro.tipo() != null) {
            condiciones.add("tipo = :tipo");
            parametros.put("tipo", filtro.tipo());
        }
        if (filtro.resultado() != null) {
            condiciones.add("resultado = :resultado");
            parametros.put("resultado", filtro.resultado());
        }
        if (filtro.actor() != null) {
            condiciones.add("lower(actor) like :actor");
            parametros.put("actor", "%" + filtro.actor().toLowerCase(Locale.ROOT) + "%");
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

    private RegistroBitacoraSistema aDominio(BitacoraSistemaEntity entity) {
        return new RegistroBitacoraSistema(
                entity.id,
                entity.fecha,
                entity.tipo,
                entity.resultado,
                entity.actor,
                entity.usuarioAfectadoId,
                entity.organizacionId,
                entity.origenIp,
                entity.userAgent,
                entity.correlacion,
                entity.detalle
        );
    }
}
