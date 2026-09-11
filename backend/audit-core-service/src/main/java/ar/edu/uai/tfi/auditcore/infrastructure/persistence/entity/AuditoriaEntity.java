package ar.edu.uai.tfi.auditcore.infrastructure.persistence.entity;

import ar.edu.uai.tfi.auditcore.domain.model.EstadoAuditoria;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "auditorias_configuracion")
public class AuditoriaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_auditoria")
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_configuracion", nullable = false)
    public ConfiguracionEntity configuracion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_dispositivo", nullable = false)
    public DispositivoEntity dispositivo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_baseline", nullable = false)
    public BaselineEntity baseline;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_organizacion", nullable = false)
    public OrganizacionReferenciaEntity organizacion;

    @Column(name = "fecha_ejecucion", nullable = false)
    public Instant fechaEjecucion;

    @Column(name = "ejecutado_por", nullable = false, length = 120)
    public String ejecutadoPor;

    @Column(name = "total_reglas", nullable = false)
    public Integer totalReglas;

    @Column(name = "reglas_cumplidas", nullable = false)
    public Integer reglasCumplidas;

    @Column(name = "total_hallazgos", nullable = false)
    public Integer totalHallazgos;

    @Column(name = "severidad_maxima", nullable = false, length = 20)
    public String severidadMaxima;

    @Column(nullable = false, length = 30)
    public String resultado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    public EstadoAuditoria estado;
}
