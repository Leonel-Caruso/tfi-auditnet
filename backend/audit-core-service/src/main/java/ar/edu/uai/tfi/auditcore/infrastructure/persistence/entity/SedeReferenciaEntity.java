package ar.edu.uai.tfi.auditcore.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "sedes")
public class SedeReferenciaEntity {

    @Id
    @Column(name = "id_sede")
    public Long id;

    @Column(name = "id_organizacion", nullable = false)
    public Long organizacionId;
}
