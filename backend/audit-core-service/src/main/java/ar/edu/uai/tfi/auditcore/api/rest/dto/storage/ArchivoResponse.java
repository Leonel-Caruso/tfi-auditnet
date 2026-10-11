package ar.edu.uai.tfi.auditcore.api.rest.dto.storage;

import ar.edu.uai.tfi.auditcore.domain.model.ArchivoRepositorio;

import java.time.Instant;

/** Archivo del repositorio (sin la ruta interna del contenedor). */
public record ArchivoResponse(Long id, String tipo, Long entidadId, Long organizacionId, String nombre,
                              String tipoContenido, long tamanioBytes, String sha256, Instant fecha, String usuario,
                              String detalle) {

    public static ArchivoResponse de(ArchivoRepositorio archivo) {
        return new ArchivoResponse(archivo.id(), archivo.tipo().name(), archivo.entidadId(), archivo.organizacionId(),
                archivo.nombre(), archivo.tipoContenido(), archivo.tamanioBytes(), archivo.sha256(), archivo.fecha(),
                archivo.usuario(), archivo.detalle());
    }
}
