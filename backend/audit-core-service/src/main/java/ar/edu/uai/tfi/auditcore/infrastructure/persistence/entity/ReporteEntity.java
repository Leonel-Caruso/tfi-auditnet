package ar.edu.uai.tfi.auditcore.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Reporte de auditoría (V5). Criterios y contenido se guardan como JSON en columnas text: es una foto fija que
 * solo se lee completa, no se consulta por dentro. La tabla es de solo inserción.
 */
@Entity
@Table(name = "reportes_auditoria")
public class ReporteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_reporte")
    public Long id;

    /** null = reporte del administrador sobre todas las organizaciones. */
    @Column(name = "id_organizacion")
    public Long organizacionId;

    @Column(nullable = false, length = 160)
    public String titulo;

    @Column(nullable = false, columnDefinition = "text")
    public String criterios;

    @Column(nullable = false, columnDefinition = "text")
    public String contenido;

    @Column(name = "total_auditorias", nullable = false)
    public Integer totalAuditorias;

    @Column(name = "total_hallazgos", nullable = false)
    public Integer totalHallazgos;

    @Column(name = "generado_por", nullable = false, length = 120)
    public String generadoPor;

    @Column(name = "fecha_generacion", nullable = false)
    public Instant fechaGeneracion;
}
