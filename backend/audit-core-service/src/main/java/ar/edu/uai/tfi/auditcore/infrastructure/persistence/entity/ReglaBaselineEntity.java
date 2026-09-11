package ar.edu.uai.tfi.auditcore.infrastructure.persistence.entity;

import ar.edu.uai.tfi.auditcore.domain.model.EstadoRegla;
import ar.edu.uai.tfi.auditcore.domain.model.SeveridadRegla;
import ar.edu.uai.tfi.auditcore.domain.model.TipoReglaConfiguracion;
import jakarta.persistence.*;

@Entity
@Table(
        name = "reglas_baseline",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_regla_baseline_codigo",
                columnNames = {"id_baseline", "codigo"}
        )
)
public class ReglaBaselineEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_regla")
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_baseline", nullable = false)
    public BaselineEntity baseline;

    @Column(nullable = false, length = 60)
    public String codigo;

    @Column(nullable = false, length = 140)
    public String nombre;

    @Column(nullable = false, length = 500)
    public String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    public TipoReglaConfiguracion tipo;

    @Column(nullable = false, length = 500)
    public String patron;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    public SeveridadRegla severidad;

    @Column(nullable = false, length = 500)
    public String recomendacion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    public EstadoRegla estado;
}
