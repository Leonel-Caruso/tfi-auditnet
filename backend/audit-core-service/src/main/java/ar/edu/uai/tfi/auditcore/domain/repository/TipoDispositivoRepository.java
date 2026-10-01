package ar.edu.uai.tfi.auditcore.domain.repository;

import ar.edu.uai.tfi.auditcore.domain.model.TipoDispositivo;

import java.util.List;

public interface TipoDispositivoRepository {

    TipoDispositivo guardar(TipoDispositivo tipoDispositivo);

    List<TipoDispositivo> listar();

    /** Indica si ya existe un tipo con el mismo nombre y fabricante (sin distinguir mayúsculas). */
    default boolean existeNombreYFabricante(String nombre, String fabricante) {
        return listar().stream().anyMatch(tipo ->
                tipo.nombre().equalsIgnoreCase(nombre) && tipo.fabricante().equalsIgnoreCase(fabricante));
    }
}
