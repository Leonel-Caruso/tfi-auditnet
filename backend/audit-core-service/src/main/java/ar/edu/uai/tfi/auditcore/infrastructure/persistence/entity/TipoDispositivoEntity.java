package ar.edu.uai.tfi.auditcore.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "tipos_dispositivo")
public class TipoDispositivoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false, length = 100)
    public String nombre;

    @Column(nullable = false, length = 100)
    public String fabricante;

    @Column(nullable = false, length = 100)
    public String familia;

    @Column(nullable = false)
    public boolean activo;
}
