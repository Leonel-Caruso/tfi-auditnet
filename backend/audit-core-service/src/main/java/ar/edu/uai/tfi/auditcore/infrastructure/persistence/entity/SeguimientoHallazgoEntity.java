package ar.edu.uai.tfi.auditcore.infrastructure.persistence.entity;

import ar.edu.uai.tfi.auditcore.domain.model.EstadoHallazgo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** Historial de cambios de estado de un hallazgo (V3). La tabla es de solo inserción. */
@Entity
@Table(name = "seguimientos_hallazgo")
public class SeguimientoHallazgoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_seguimiento")
    public Long id;

    @Column(name = "id_hallazgo", nullable = false)
    public Long hallazgoId;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_anterior", nullable = false, length = 20)
    public EstadoHallazgo estadoAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_nuevo", nullable = false, length = 20)
    public EstadoHallazgo estadoNuevo;

    @Column(length = 1000)
    public String comentario;

    @Column(nullable = false, length = 120)
    public String usuario;

    @Column(nullable = false)
    public Instant fecha;
}
