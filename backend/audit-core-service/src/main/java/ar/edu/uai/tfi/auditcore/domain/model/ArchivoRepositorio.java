package ar.edu.uai.tfi.auditcore.domain.model;

import java.time.Instant;

/**
 * Entrada del catálogo del repositorio de archivos (tabla {@code archivos_repositorio}): dónde está el archivo en el
 * almacenamiento, su tamaño y su hash SHA-256, que permite comprobar que no fue alterado.
 *
 * @param entidadId      configuración o reporte de origen (null para un respaldo)
 * @param organizacionId organización dueña del archivo (null para respaldos y reportes de todas las organizaciones)
 * @param ruta           nombre del archivo dentro del contenedor (ej. configuraciones/org-2/dispositivo-4/CFG-001-v1.cfg)
 * @param detalle        resumen legible (para respaldos: tablas, filas y versión del esquema)
 */
public record ArchivoRepositorio(
        Long id,
        TipoArchivo tipo,
        Long entidadId,
        Long organizacionId,
        String ruta,
        String nombre,
        String tipoContenido,
        long tamanioBytes,
        String sha256,
        Instant fecha,
        String usuario,
        String detalle
) {
}
