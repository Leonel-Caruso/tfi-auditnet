package ar.edu.uai.tfi.management.infrastructure.persistence.entity;

import ar.edu.uai.tfi.management.domain.model.EstadoRegistro;
import jakarta.persistence.*;

@Entity
@Table(name = "roles", uniqueConstraints = @UniqueConstraint(name = "uk_rol_nombre", columnNames = "nombre"))
public class RolEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_rol")
    public Long id;

    @Column(name = "nombre", nullable = false, length = 80)
    public String nombre;

    @Column(name = "descripcion", nullable = false, length = 240)
    public String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 30)
    public EstadoRegistro estado;
}
