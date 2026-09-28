package ar.edu.uai.tfi.management.infrastructure.persistence.entity;

import ar.edu.uai.tfi.management.domain.model.ResultadoEvento;
import ar.edu.uai.tfi.management.domain.model.TipoEventoSistema;
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
 * Bitácora de auditoría de sistema. Tabla de solo inserción: la migración V2 agrega
 * un trigger que rechaza UPDATE, DELETE y TRUNCATE.
 * Los ids de usuario y organización no tienen clave foránea a propósito: el registro
 * debe conservarse aunque la entidad referenciada cambie.
 */
@Entity
@Table(name = "bitacora_sistema")
public class BitacoraSistemaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_evento")
    public Long id;

    @Column(name = "fecha", nullable = false)
    public Instant fecha;

    @Enumerated(EnumType.STRING)
    @Column(name = "evento", nullable = false, length = 40)
    public TipoEventoSistema tipo;

    @Enumerated(EnumType.STRING)
    @Column(name = "resultado", nullable = false, length = 10)
    public ResultadoEvento resultado;

    @Column(name = "actor", nullable = false, length = 160)
    public String actor;

    @Column(name = "id_usuario_afectado")
    public Long usuarioAfectadoId;

    @Column(name = "id_organizacion")
    public Long organizacionId;

    @Column(name = "origen_ip", length = 200)
    public String origenIp;

    @Column(name = "user_agent", length = 300)
    public String userAgent;

    @Column(name = "correlacion", length = 64)
    public String correlacion;

    @Column(name = "detalle", length = 1000)
    public String detalle;
}
