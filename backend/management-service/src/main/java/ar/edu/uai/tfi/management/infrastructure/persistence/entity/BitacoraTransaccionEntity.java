package ar.edu.uai.tfi.management.infrastructure.persistence.entity;

import ar.edu.uai.tfi.management.domain.model.OperacionTransaccion;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Bitácora de transacciones (migración V3). Tabla de solo inserción, compartida:
 * la crea management-service y también la escribe audit-core-service.
 */
@Entity
@Table(name = "bitacora_transacciones")
public class BitacoraTransaccionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_transaccion")
    public Long id;

    @Column(name = "fecha", nullable = false)
    public Instant fecha;

    @Column(name = "servicio", nullable = false, length = 40)
    public String servicio;

    @Column(name = "entidad", nullable = false, length = 40)
    public String entidad;

    @Column(name = "id_entidad")
    public Long entidadId;

    @Enumerated(EnumType.STRING)
    @Column(name = "operacion", nullable = false, length = 20)
    public OperacionTransaccion operacion;

    @Column(name = "actor", nullable = false, length = 160)
    public String actor;

    @Column(name = "id_organizacion")
    public Long organizacionId;

    @Column(name = "valor_anterior", columnDefinition = "text")
    public String valorAnterior;

    @Column(name = "valor_nuevo", columnDefinition = "text")
    public String valorNuevo;

    @Column(name = "correlacion", length = 64)
    public String correlacion;

    @Column(name = "detalle", length = 1000)
    public String detalle;
}
