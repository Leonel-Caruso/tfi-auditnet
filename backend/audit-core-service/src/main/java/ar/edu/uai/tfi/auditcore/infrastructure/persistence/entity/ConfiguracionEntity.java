package ar.edu.uai.tfi.auditcore.infrastructure.persistence.entity;

import ar.edu.uai.tfi.auditcore.domain.model.FormatoConfiguracion;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(
        name = "configuraciones_dispositivo",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_config_dispositivo_version",
                columnNames = {"id_dispositivo", "version"}
        )
)
public class ConfiguracionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_configuracion")
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_dispositivo", nullable = false)
    public DispositivoEntity dispositivo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_organizacion", nullable = false)
    public OrganizacionReferenciaEntity organizacion;

    @Column(nullable = false)
    public Integer version;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    public FormatoConfiguracion formato;

    @Column(name = "nombre_fuente", nullable = false, length = 180)
    public String nombreFuente;

    @Column(name = "contenido_original", nullable = false, columnDefinition = "text")
    public String contenidoOriginal;

    @Column(name = "contenido_normalizado", nullable = false, columnDefinition = "text")
    public String contenidoNormalizado;

    @Column(name = "fecha_importacion", nullable = false)
    public Instant fechaImportacion;

    @Column(name = "usuario_responsable", nullable = false, length = 120)
    public String usuarioResponsable;
}
