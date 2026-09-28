package ar.edu.uai.tfi.management.domain.repository;

import ar.edu.uai.tfi.management.domain.model.FiltroBitacoraExcepciones;
import ar.edu.uai.tfi.management.domain.model.RegistroExcepcion;

import java.util.List;

public interface BitacoraExcepcionesRepository {

    void guardar(RegistroExcepcion registro);

    /** Devuelve los registros más recientes primero, de ambos servicios. */
    List<RegistroExcepcion> buscar(FiltroBitacoraExcepciones filtro);
}
