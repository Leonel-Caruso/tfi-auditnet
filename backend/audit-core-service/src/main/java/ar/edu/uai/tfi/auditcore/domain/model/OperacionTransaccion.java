package ar.edu.uai.tfi.auditcore.domain.model;

/** Tipo de operación de negocio registrada en la bitácora de transacciones. */
public enum OperacionTransaccion {
    ALTA,
    MODIFICACION,
    CAMBIO_ESTADO,
    BAJA_LOGICA,
    IMPORTACION,
    EJECUCION
}
