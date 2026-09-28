package ar.edu.uai.tfi.auditcore.domain.repository;

import ar.edu.uai.tfi.auditcore.domain.model.RegistroExcepcion;

public interface BitacoraExcepcionesRepository {

    void guardar(RegistroExcepcion registro);
}
