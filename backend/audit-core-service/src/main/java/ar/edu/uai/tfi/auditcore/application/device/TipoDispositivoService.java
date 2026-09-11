package ar.edu.uai.tfi.auditcore.application.device;

import ar.edu.uai.tfi.auditcore.domain.model.TipoDispositivo;
import ar.edu.uai.tfi.auditcore.domain.repository.TipoDispositivoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;

@ApplicationScoped
public class TipoDispositivoService {

    private final TipoDispositivoRepository repository;

    public TipoDispositivoService(TipoDispositivoRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public TipoDispositivo crear(String nombre, String fabricante, String familia) {
        validarCampo(nombre, "nombre");
        validarCampo(fabricante, "fabricante");
        validarCampo(familia, "familia");

        TipoDispositivo nuevoTipo = new TipoDispositivo(
                null,
                nombre.trim(),
                fabricante.trim(),
                familia.trim(),
                true
        );

        return repository.guardar(nuevoTipo);
    }

    public List<TipoDispositivo> listar() {
        return repository.listar();
    }

    private void validarCampo(String valor, String nombreCampo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("El campo " + nombreCampo + " es obligatorio.");
        }
    }
}
