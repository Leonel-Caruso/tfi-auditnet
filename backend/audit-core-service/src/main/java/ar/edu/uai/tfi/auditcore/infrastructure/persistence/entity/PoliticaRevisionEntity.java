package ar.edu.uai.tfi.auditcore.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "politicas_revision")
public class PoliticaRevisionEntity {

    @Id
    @Column(name = "id_organizacion")
    public Long organizacionId;

    @Column(name = "dias_critica", nullable = false)
    public int diasCritica;

    @Column(name = "dias_alta", nullable = false)
    public int diasAlta;

    @Column(name = "dias_media", nullable = false)
    public int diasMedia;

    @Column(name = "dias_baja", nullable = false)
    public int diasBaja;

    @Column(name = "dias_reauditoria", nullable = false)
    public int diasReauditoria;

    @Column(name = "dias_configuracion", nullable = false)
    public int diasConfiguracion;

    @Column(name = "reauditoria_automatica", nullable = false)
    public boolean reauditoriaAutomatica;

    @Column(name = "modificada_en", nullable = false)
    public Instant modificadaEn;

    @Column(name = "modificada_por", nullable = false, length = 120)
    public String modificadaPor;
}
