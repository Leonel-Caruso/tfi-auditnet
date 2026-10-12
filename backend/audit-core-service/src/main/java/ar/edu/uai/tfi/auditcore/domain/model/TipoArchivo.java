package ar.edu.uai.tfi.auditcore.domain.model;

/** Qué guarda cada archivo del repositorio de archivos. */
public enum TipoArchivo {
    /** Archivo original de una configuración importada. */
    CONFIGURACION,
    /** CSV de un reporte de auditoría, tal como se generó. */
    REPORTE,
    /** Respaldo lógico de la base de datos. */
    RESPALDO
}
