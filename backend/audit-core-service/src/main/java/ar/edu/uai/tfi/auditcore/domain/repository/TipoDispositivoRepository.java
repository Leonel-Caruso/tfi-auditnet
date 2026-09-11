package ar.edu.uai.tfi.auditcore.domain.repository;

import ar.edu.uai.tfi.auditcore.domain.model.TipoDispositivo;

import java.util.List;

public interface TipoDispositivoRepository {

    TipoDispositivo guardar(TipoDispositivo tipoDispositivo);

    List<TipoDispositivo> listar();
}
