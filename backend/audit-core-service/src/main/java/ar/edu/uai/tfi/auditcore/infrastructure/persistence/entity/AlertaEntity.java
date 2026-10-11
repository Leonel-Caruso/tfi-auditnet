package ar.edu.uai.tfi.auditcore.infrastructure.persistence.entity;

import ar.edu.uai.tfi.auditcore.domain.model.EstadoAlerta;
import ar.edu.uai.tfi.auditcore.domain.model.PerfilTecnico;
import ar.edu.uai.tfi.auditcore.domain.model.SeveridadRegla;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** Alerta crítica interna (V4). Las claves se guardan como ids: las claves foráneas las controla la base. */
@Entity
@Table(name = "alertas_criticas")
public class AlertaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_alerta")
    public Long id;

    @Column(name = "id_hallazgo", nullable = false, unique = true)
    public Long hallazgoId;

    @Column(name = "id_auditoria", nullable = false)
    public Long auditoriaId;

    @Column(name = "id_dispositivo", nullable = false)
    public Long dispositivoId;

    @Column(name = "id_organizacion", nullable = false)
    public Long organizacionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    public SeveridadRegla severidad;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    public EstadoAlerta estado;

    @Column(name = "fecha_generacion", nullable = false)
    public Instant fechaGeneracion;

    @Enumerated(EnumType.STRING)
    @Column(name = "perfil_asignado", length = 40)
    public PerfilTecnico perfilAsignado;

    @Column(length = 1000)
    public String comentario;

    @Column(name = "fecha_estado")
    public Instant fechaEstado;

    @Column(name = "usuario_estado", length = 120)
    public String usuarioEstado;
}
