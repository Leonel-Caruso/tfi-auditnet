package ar.edu.uai.tfi.management.infrastructure.persistence.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "planes", uniqueConstraints = @UniqueConstraint(name = "uk_plan_codigo", columnNames = "codigo"))
public class PlanEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_plan")
    public Long id;

    @Column(name = "codigo", nullable = false, length = 30)
    public String codigo;

    @Column(name = "nombre", nullable = false, length = 80)
    public String nombre;

    @Column(name = "max_dispositivos")
    public Integer maxDispositivos;

    @Column(name = "max_usuarios")
    public Integer maxUsuarios;

    @Column(name = "dias_vigencia")
    public Integer diasVigencia;

    @Column(name = "precio_mensual_usd", precision = 10, scale = 2)
    public BigDecimal precioMensualUsd;

    @Column(name = "condiciones", length = 400)
    public String condiciones;

    @Column(name = "orden", nullable = false)
    public Integer orden;
}
