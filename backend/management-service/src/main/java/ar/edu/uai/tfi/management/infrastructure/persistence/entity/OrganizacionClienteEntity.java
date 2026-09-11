package ar.edu.uai.tfi.management.infrastructure.persistence.entity;

import ar.edu.uai.tfi.management.domain.model.EstadoRegistro;
import jakarta.persistence.*;

@Entity
@Table(name = "organizaciones_cliente",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_organizacion_identificador", columnNames = "identificador"),
                @UniqueConstraint(name = "uk_organizacion_razon_social", columnNames = "razon_social")
        })
public class OrganizacionClienteEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_organizacion")
    public Long id;

    @Column(name = "identificador", nullable = false, length = 80)
    public String identificador;

    @Column(name = "razon_social", nullable = false, length = 160)
    public String razonSocial;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 30)
    public EstadoRegistro estado;
}
