package ar.edu.uai.tfi.management.infrastructure.persistence.repository;

import ar.edu.uai.tfi.management.domain.model.OrganizacionCliente;
import ar.edu.uai.tfi.management.domain.model.PlanOrganizacion;
import ar.edu.uai.tfi.management.domain.repository.OrganizacionClienteRepository;
import ar.edu.uai.tfi.management.infrastructure.persistence.entity.OrganizacionClienteEntity;
import ar.edu.uai.tfi.management.infrastructure.persistence.entity.PlanEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.LockModeType;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@ApplicationScoped
public class PanacheOrganizacionClienteRepository
        implements PanacheRepository<OrganizacionClienteEntity>, OrganizacionClienteRepository {

    @Override
    public OrganizacionCliente guardar(OrganizacionCliente organizacion, PlanOrganizacion plan) {
        OrganizacionClienteEntity entity = new OrganizacionClienteEntity();
        entity.identificador = organizacion.identificador();
        entity.razonSocial = organizacion.razonSocial();
        entity.estado = organizacion.estado();
        entity.plan = getEntityManager().find(PlanEntity.class, plan.plan().id());
        entity.planDesde = plan.desde();
        entity.planVence = plan.vence();
        entity.limiteDispositivos = plan.limiteDispositivos();
        entity.limiteUsuarios = plan.limiteUsuarios();
        persist(entity);
        return aDominio(entity);
    }

    @Override
    public List<OrganizacionCliente> listar() {
        return find("order by razonSocial").list().stream().map(this::aDominio).toList();
    }

    @Override
    public Optional<OrganizacionCliente> buscarPorId(Long id) {
        return findByIdOptional(id).map(this::aDominio);
    }

    @Override
    public Optional<OrganizacionCliente> buscarPorIdentificador(String identificador) {
        return find("upper(identificador) = ?1", identificador.trim().toUpperCase(Locale.ROOT))
                .firstResultOptional().map(this::aDominio);
    }

    @Override
    public Optional<OrganizacionCliente> buscarPorRazonSocial(String razonSocial) {
        return find("lower(razonSocial) = ?1", razonSocial.trim().toLowerCase(Locale.ROOT))
                .firstResultOptional().map(this::aDominio);
    }

    @Override
    public OrganizacionCliente actualizar(OrganizacionCliente organizacion) {
        OrganizacionClienteEntity entity = findByIdOptional(organizacion.id())
                .orElseThrow(() -> new java.util.NoSuchElementException("No existe la organización."));
        entity.identificador = organizacion.identificador();
        entity.razonSocial = organizacion.razonSocial();
        entity.estado = organizacion.estado();
        flush();
        return aDominio(entity);
    }

    @Override
    public Optional<PlanOrganizacion> buscarPlan(Long organizacionId) {
        List<Object[]> filas = getEntityManager().createQuery("""
                        select o.id, p.id, o.planDesde, o.planVence, o.limiteDispositivos, o.limiteUsuarios
                        from OrganizacionClienteEntity o join o.plan p
                        where o.id = :id""", Object[].class)
                .setParameter("id", organizacionId)
                .getResultList();
        if (filas.isEmpty()) {
            return Optional.empty();
        }
        Object[] fila = filas.get(0);
        PlanEntity plan = getEntityManager().find(PlanEntity.class, fila[1]);
        return Optional.of(new PlanOrganizacion((Long) fila[0], PanachePlanRepository.aDominio(plan),
                (Instant) fila[2], (Instant) fila[3], (Integer) fila[4], (Integer) fila[5]));
    }

    @Override
    public Optional<PlanOrganizacion> buscarPlanParaActualizar(Long organizacionId) {
        if (findByIdOptional(organizacionId, LockModeType.PESSIMISTIC_WRITE).isEmpty()) {
            return Optional.empty();
        }
        return buscarPlan(organizacionId);
    }

    /** Actualización directa en la base: el plan se lee siempre con una consulta, no desde la entidad en memoria. */
    @Override
    public void asignarPlan(Long organizacionId, PlanOrganizacion plan) {
        flush();
        update("plan = ?1, planDesde = ?2, planVence = ?3, limiteDispositivos = ?4, limiteUsuarios = ?5 where id = ?6",
                getEntityManager().find(PlanEntity.class, plan.plan().id()), plan.desde(), plan.vence(),
                plan.limiteDispositivos(), plan.limiteUsuarios(), organizacionId);
    }

    private OrganizacionCliente aDominio(OrganizacionClienteEntity entity) {
        return new OrganizacionCliente(entity.id, entity.identificador, entity.razonSocial, entity.estado);
    }
}
