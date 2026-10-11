package ar.edu.uai.tfi.auditcore.infrastructure.persistence.entity;

import ar.edu.uai.tfi.auditcore.domain.model.TipoArchivo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** Catálogo del repositorio de archivos (V7). Solo inserción. */
@Entity
@Table(name = "archivos_repositorio")
public class ArchivoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_archivo")
    public Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    public TipoArchivo tipo;

    @Column(name = "id_entidad")
    public Long entidadId;

    @Column(name = "id_organizacion")
    public Long organizacionId;

    @Column(nullable = false, length = 400)
    public String ruta;

    @Column(nullable = false, length = 200)
    public String nombre;

    @Column(name = "tipo_contenido", nullable = false, length = 100)
    public String tipoContenido;

    @Column(name = "tamanio_bytes", nullable = false)
    public Long tamanioBytes;

    @Column(nullable = false, length = 64)
    public String sha256;

    @Column(nullable = false)
    public Instant fecha;

    @Column(nullable = false, length = 120)
    public String usuario;

    @Column(length = 500)
    public String detalle;
}
