package ar.edu.uai.tfi.management.infrastructure.persistence.entity;

import ar.edu.uai.tfi.management.domain.model.EstadoSolicitudPlan;
import ar.edu.uai.tfi.management.domain.model.ModalidadContratacion;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "solicitudes_cambio_plan")
public class SolicitudPlanEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_solicitud")
    public Long id;

    @Column(name = "id_organizacion", nullable = false)
    public Long organizacionId;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "id_plan_actual", nullable = false)
    public PlanEntity planActual;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "id_plan_solicitado", nullable = false)
    public PlanEntity planSolicitado;

    @Enumerated(EnumType.STRING)
    @Column(name = "modalidad", nullable = false, length = 10)
    public ModalidadContratacion modalidad;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    public EstadoSolicitudPlan estado;

    @Column(name = "comentario", length = 500)
    public String comentario;

    @Column(name = "solicitado_por", nullable = false, length = 100)
    public String solicitadoPor;

    @Column(name = "fecha_solicitud", nullable = false)
    public Instant fechaSolicitud;

    @Column(name = "resuelto_por", length = 100)
    public String resueltoPor;

    @Column(name = "fecha_resolucion")
    public Instant fechaResolucion;

    @Column(name = "respuesta", length = 500)
    public String respuesta;
}
