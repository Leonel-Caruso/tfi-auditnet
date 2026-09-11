package ar.edu.uai.tfi.auditcore.infrastructure.persistence.entity;

import ar.edu.uai.tfi.auditcore.domain.model.CriticidadDispositivo;
import ar.edu.uai.tfi.auditcore.domain.model.EstadoDispositivo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "dispositivos_red",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_dispositivos_red_identificador",
                columnNames = "identificador"
        )
)
public class DispositivoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_dispositivo")
    public Long id;

    @Column(nullable = false, length = 120)
    public String nombre;

    @Column(nullable = false, length = 100)
    public String identificador;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_tipo_dispositivo", nullable = false)
    public TipoDispositivoEntity tipoDispositivo;

    @Column(nullable = false, length = 100)
    public String fabricante;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_organizacion", nullable = false)
    public OrganizacionReferenciaEntity organizacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_sede")
    public SedeReferenciaEntity sede;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    public CriticidadDispositivo criticidad;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    public EstadoDispositivo estado;
}
