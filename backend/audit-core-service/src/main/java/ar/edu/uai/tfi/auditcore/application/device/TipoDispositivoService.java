package ar.edu.uai.tfi.auditcore.application.device;

import ar.edu.uai.tfi.auditcore.application.port.TrazabilidadPort;
import ar.edu.uai.tfi.auditcore.domain.model.TipoDispositivo;
import ar.edu.uai.tfi.auditcore.domain.model.Transaccion;
import ar.edu.uai.tfi.auditcore.domain.repository.TipoDispositivoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;

/** Catálogo de tipos de dispositivo. Es común a todas las organizaciones. */
@ApplicationScoped
public class TipoDispositivoService {

    private final TipoDispositivoRepository repository;
    private final TrazabilidadPort trazabilidad;

    public TipoDispositivoService(TipoDispositivoRepository repository, TrazabilidadPort trazabilidad) {
        this.repository = repository;
        this.trazabilidad = trazabilidad;
    }

    @Transactional
    public TipoDispositivo crear(String nombre, String fabricante, String familia, String actor) {
        validarCampo(nombre, "nombre");
        validarCampo(fabricante, "fabricante");
        validarCampo(familia, "familia");

        if (repository.existeNombreYFabricante(nombre.trim(), fabricante.trim())) {
            throw new IllegalStateException(
                    "Ya existe el tipo de dispositivo " + nombre.trim() + " de " + fabricante.trim() + "."
            );
        }

        TipoDispositivo nuevoTipo = new TipoDispositivo(
                null,
                nombre.trim(),
                fabricante.trim(),
                familia.trim(),
                true
        );

        TipoDispositivo creado = repository.guardar(nuevoTipo);
        // Catálogo global: no pertenece a una organización cliente.
        trazabilidad.registrar(Transaccion.alta("TIPO_DISPOSITIVO", creado.id(), null, creado,
                actor == null || actor.isBlank() ? null : actor));
        return creado;
    }

    public List<TipoDispositivo> listar() {
        return repository.listar();
    }

    private void validarCampo(String valor, String nombreCampo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("El campo " + nombreCampo + " es obligatorio.");
        }
        if (valor.trim().length() > 100) {
            throw new IllegalArgumentException("El campo " + nombreCampo + " no puede superar los 100 caracteres.");
        }
    }
}
