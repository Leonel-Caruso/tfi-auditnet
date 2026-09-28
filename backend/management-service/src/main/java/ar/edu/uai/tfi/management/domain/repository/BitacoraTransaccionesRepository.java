package ar.edu.uai.tfi.management.domain.repository;

import ar.edu.uai.tfi.management.domain.model.FiltroBitacoraTransacciones;
import ar.edu.uai.tfi.management.domain.model.RegistroTransaccion;

import java.util.List;

public interface BitacoraTransaccionesRepository {

    RegistroTransaccion guardar(RegistroTransaccion registro);

    /** Devuelve los registros más recientes primero. */
    List<RegistroTransaccion> buscar(FiltroBitacoraTransacciones filtro);
}
