package ar.edu.uai.tfi.management.infrastructure.persistence.entity;

import ar.edu.uai.tfi.management.domain.model.EstadoRegistro;
import jakarta.persistence.*;

import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "usuarios",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_usuario_email", columnNames = "email"),
                @UniqueConstraint(name = "uk_usuario_username", columnNames = "nombre_usuario")
        })
public class UsuarioEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    public Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "id_organizacion", nullable = false)
    public OrganizacionClienteEntity organizacion;

    @Column(name = "nombre", nullable = false, length = 140)
    public String nombre;

    @Column(name = "email", nullable = false, length = 160)
    public String email;

    @Column(name = "nombre_usuario", nullable = false, length = 100)
    public String nombreUsuario;

    @Column(name = "password_hash", nullable = false, length = 120)
    public String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 30)
    public EstadoRegistro estado;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "usuarios_roles",
            joinColumns = @JoinColumn(name = "id_usuario"),
            inverseJoinColumns = @JoinColumn(name = "id_rol"))
    public Set<RolEntity> roles = new LinkedHashSet<>();
}
