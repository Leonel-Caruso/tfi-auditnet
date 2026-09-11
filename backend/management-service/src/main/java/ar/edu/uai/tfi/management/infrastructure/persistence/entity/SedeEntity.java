package ar.edu.uai.tfi.management.infrastructure.persistence.entity;

import ar.edu.uai.tfi.management.domain.model.EstadoRegistro;
import jakarta.persistence.*;

@Entity
@Table(name = "sedes",
        uniqueConstraints = @UniqueConstraint(name = "uk_sede_organizacion_nombre", columnNames = {"id_organizacion", "nombre"}))
public class SedeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_sede")
    public Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "id_organizacion", nullable = false)
    public OrganizacionClienteEntity organizacion;

    @Column(name = "nombre", nullable = false, length = 120)
    public String nombre;

    @Column(name = "ubicacion", nullable = false, length = 200)
    public String ubicacion;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 30)
    public EstadoRegistro estado;
}
