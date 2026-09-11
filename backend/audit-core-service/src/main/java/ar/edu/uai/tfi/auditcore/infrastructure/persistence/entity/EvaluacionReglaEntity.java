package ar.edu.uai.tfi.auditcore.infrastructure.persistence.entity;

import ar.edu.uai.tfi.auditcore.domain.model.SeveridadRegla;
import ar.edu.uai.tfi.auditcore.domain.model.TipoReglaConfiguracion;
import jakarta.persistence.*;

@Entity
@Table(
        name = "evaluaciones_regla_auditoria",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_evaluacion_auditoria_regla",
                columnNames = {"id_auditoria", "id_regla"}
        )
)
public class EvaluacionReglaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_evaluacion")
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_auditoria", nullable = false)
    public AuditoriaEntity auditoria;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_regla", nullable = false)
    public ReglaBaselineEntity regla;

    @Column(name = "codigo_regla", nullable = false, length = 60)
    public String codigoRegla;

    @Column(name = "nombre_regla", nullable = false, length = 140)
    public String nombreRegla;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    public TipoReglaConfiguracion tipo;

    @Column(nullable = false, length = 500)
    public String patron;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    public SeveridadRegla severidad;

    @Column(nullable = false)
    public Boolean cumple;

    @Column(nullable = false, length = 800)
    public String evidencia;
}
