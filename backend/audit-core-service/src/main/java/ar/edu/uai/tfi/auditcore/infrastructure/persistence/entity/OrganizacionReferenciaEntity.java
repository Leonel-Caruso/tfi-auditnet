package ar.edu.uai.tfi.auditcore.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "organizaciones_cliente")
public class OrganizacionReferenciaEntity {

    @Id
    @Column(name = "id_organizacion")
    public Long id;
}
