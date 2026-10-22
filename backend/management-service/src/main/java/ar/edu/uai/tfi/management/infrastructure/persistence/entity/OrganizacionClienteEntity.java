package ar.edu.uai.tfi.management.infrastructure.persistence.entity;

import ar.edu.uai.tfi.management.domain.model.EstadoRegistro;
import jakarta.persistence.*;
import org.hibernate.annotations.DynamicUpdate;

import java.time.Instant;

/** Solo actualiza las columnas modificadas: editar la organización no pisa un cambio de plan simultáneo. */
@Entity
@DynamicUpdate
@Table(name = "organizaciones_cliente",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_organizacion_identificador", columnNames = "identificador"),
                @UniqueConstraint(name = "uk_organizacion_razon_social", columnNames = "razon_social")
        })
public class OrganizacionClienteEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_organizacion")
    public Long id;

    @Column(name = "identificador", nullable = false, length = 80)
    public String identificador;

    @Column(name = "razon_social", nullable = false, length = 160)
    public String razonSocial;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 30)
    public EstadoRegistro estado;

    /** Carga inmediata: el catálogo es chico y se lee el plan completo, no solo su id. */
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "id_plan", nullable = false)
    public PlanEntity plan;

    @Column(name = "plan_desde", nullable = false)
    public Instant planDesde;

    /** Fin del período de prueba; null si el plan no vence. */
    @Column(name = "plan_vence")
    public Instant planVence;

    /** Límites acordados para un plan a medida; null para usar los del plan. */
    @Column(name = "limite_dispositivos")
    public Integer limiteDispositivos;

    @Column(name = "limite_usuarios")
    public Integer limiteUsuarios;
}
