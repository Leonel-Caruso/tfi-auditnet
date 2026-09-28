package ar.edu.uai.tfi.management.application.port;

import ar.edu.uai.tfi.management.domain.model.Transaccion;

/**
 * Puerto de la bitácora de transacciones (operaciones de negocio sobre entidades críticas).
 * El registro se guarda en la misma transacción que la operación: si la operación no se
 * confirma, tampoco queda el registro.
 */
public interface TrazabilidadPort {

    void registrar(Transaccion transaccion);
}
