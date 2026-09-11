package ar.edu.uai.tfi.auditcore.infrastructure.persistence.entity;

import ar.edu.uai.tfi.auditcore.domain.model.EstadoHallazgo;
import ar.edu.uai.tfi.auditcore.domain.model.SeveridadRegla;
import ar.edu.uai.tfi.auditcore.domain.model.TipoReglaConfiguracion;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(
        name = "hallazgos_auditoria",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_hallazgo_auditoria_regla",
                columnNames = {"id_auditoria", "id_regla"}
        )
)
public class HallazgoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_hallazgo")
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_auditoria", nullable = false)
    public AuditoriaEntity auditoria;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_regla", nullable = false)
    public ReglaBaselineEntity regla;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_dispositivo", nullable = false)
    public DispositivoEntity dispositivo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_baseline", nullable = false)
    public BaselineEntity baseline;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_organizacion", nullable = false)
    public OrganizacionReferenciaEntity organizacion;

    @Column(name = "codigo_regla", nullable = false, length = 60)
    public String codigoRegla;

    @Column(name = "nombre_regla", nullable = false, length = 140)
    public String nombreRegla;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_regla", nullable = false, length = 30)
    public TipoReglaConfiguracion tipoRegla;

    @Column(nullable = false, length = 500)
    public String patron;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    public SeveridadRegla severidad;

    @Column(nullable = false, length = 800)
    public String evidencia;

    @Column(nullable = false, length = 500)
    public String recomendacion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    public EstadoHallazgo estado;

    @Column(name = "fecha_deteccion", nullable = false)
    public Instant fechaDeteccion;
}
