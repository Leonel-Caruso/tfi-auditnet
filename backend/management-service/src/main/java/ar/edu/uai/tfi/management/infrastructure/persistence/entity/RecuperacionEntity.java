package ar.edu.uai.tfi.management.infrastructure.persistence.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "recuperaciones_contrasena",
        uniqueConstraints = @UniqueConstraint(name = "uk_recuperacion_token", columnNames = "token_hash"))
public class RecuperacionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_recuperacion")
    public Long id;

    @Column(name = "id_usuario", nullable = false)
    public Long usuarioId;

    /** SHA-256 del token en hexadecimal; el token solo viaja en el correo. */
    @Column(name = "token_hash", nullable = false, length = 64)
    public String tokenHash;

    @Column(name = "creada_en", nullable = false)
    public Instant creadaEn;

    @Column(name = "vence_en", nullable = false)
    public Instant venceEn;

    @Column(name = "usada_en")
    public Instant usadaEn;
}
