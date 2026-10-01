package ar.edu.uai.tfi.auditcore.infrastructure.persistence.entity;

import ar.edu.uai.tfi.auditcore.domain.model.EstadoBaseline;
import jakarta.persistence.*;

@Entity
@Table(
        name = "baselines_configuracion",
        // V2 (Bloque 3): una baseline se versiona; el nombre se repite en cada versión.
        uniqueConstraints = @UniqueConstraint(
                name = "uk_baseline_org_nombre_version",
                columnNames = {"id_organizacion", "nombre", "version"}
        )
)
public class BaselineEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_baseline")
    public Long id;

    @Column(nullable = false, length = 140)
    public String nombre;

    @Column(nullable = false, length = 500)
    public String descripcion;

    @Column(nullable = false)
    public Integer version;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_tipo_dispositivo", nullable = false)
    public TipoDispositivoEntity tipoDispositivo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_organizacion", nullable = false)
    public OrganizacionReferenciaEntity organizacion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    public EstadoBaseline estado;
}
