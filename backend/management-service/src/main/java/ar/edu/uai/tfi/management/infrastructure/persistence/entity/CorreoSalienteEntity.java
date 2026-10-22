package ar.edu.uai.tfi.management.infrastructure.persistence.entity;

import ar.edu.uai.tfi.management.domain.model.EstadoCorreo;
import ar.edu.uai.tfi.management.domain.model.TipoCorreo;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "correos_salientes")
public class CorreoSalienteEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_correo")
    public Long id;

    @Column(name = "origen", nullable = false, length = 30)
    public String origen;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 40)
    public TipoCorreo tipo;

    @Column(name = "id_organizacion")
    public Long organizacionId;

    @Column(name = "destinatario", nullable = false, length = 160)
    public String destinatario;

    @Column(name = "nombre_destinatario", length = 140)
    public String nombreDestinatario;

    /** Datos del aviso en JSON. */
    @Column(name = "datos", nullable = false, columnDefinition = "text")
    public String datos;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    public EstadoCorreo estado;

    @Column(name = "intentos", nullable = false)
    public int intentos;

    @Column(name = "proximo_intento", nullable = false)
    public Instant proximoIntento;

    @Column(name = "creado_en", nullable = false)
    public Instant creadoEn;

    @Column(name = "enviado_en")
    public Instant enviadoEn;

    @Column(name = "ultimo_error", length = 500)
    public String ultimoError;

    @Column(name = "id_envio", length = 100)
    public String idEnvio;
}
