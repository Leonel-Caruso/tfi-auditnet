package ar.edu.uai.tfi.management.domain.repository;

import ar.edu.uai.tfi.management.domain.model.FiltroBitacoraSistema;
import ar.edu.uai.tfi.management.domain.model.RegistroBitacoraSistema;

import java.util.List;

public interface BitacoraSistemaRepository {

    RegistroBitacoraSistema guardar(RegistroBitacoraSistema registro);

    /** Devuelve los registros más recientes primero. */
    List<RegistroBitacoraSistema> buscar(FiltroBitacoraSistema filtro);
}
