package ar.edu.uai.tfi.auditcore.domain.model;

/**
 * Gravedad de un error registrado en la bitácora de excepciones.
 * ERROR: fallas inesperadas (respuesta 5xx). WARN: errores de uso rechazados por el sistema (4xx).
 */
public enum NivelExcepcion {
    ERROR,
    WARN
}
