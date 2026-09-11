package ar.edu.uai.tfi.auditcore.application.policy;

import ar.edu.uai.tfi.auditcore.application.port.TrazabilidadPort;
import ar.edu.uai.tfi.auditcore.domain.model.BaselineConfiguracion;
import ar.edu.uai.tfi.auditcore.domain.model.EstadoBaseline;
import ar.edu.uai.tfi.auditcore.domain.repository.BaselineRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@ApplicationScoped
public class BaselineService {

    private final BaselineRepository repository;
    private final TrazabilidadPort trazabilidad;

    public BaselineService(BaselineRepository repository, TrazabilidadPort trazabilidad) {
        this.repository = repository;
        this.trazabilidad = trazabilidad;
    }

    @Transactional
    public BaselineConfiguracion crear(
            String nombre,
            String descripcion,
            Long tipoDispositivoId,
            Long organizacionId,
            String actor
    ) {
        validarTexto(nombre, "nombre");
        validarTexto(descripcion, "descripcion");
        validarId(tipoDispositivoId, "tipoDispositivoId");
        validarId(organizacionId, "organizacionId");

        String nombreNormalizado = nombre.trim();
        if (repository.existeNombreEnOrganizacion(nombreNormalizado, organizacionId)) {
            throw new IllegalStateException("Ya existe una baseline con ese nombre para la organización seleccionada.");
        }

        BaselineConfiguracion nueva = new BaselineConfiguracion(
                null,
                nombreNormalizado,
                descripcion.trim(),
                1,
                tipoDispositivoId,
                organizacionId,
                EstadoBaseline.ACTIVO
        );

        BaselineConfiguracion creada = repository.guardar(nueva);
        trazabilidad.registrar(
                actorSeguro(actor),
                "BASELINE_CREADA",
                "baselineId=" + creada.id() + ", nombre=" + creada.nombre() + ", version=" + creada.version()
        );
        return creada;
    }

    public List<BaselineConfiguracion> listar() {
        return repository.listar();
    }

    public List<BaselineConfiguracion> listarPorOrganizacion(Long organizacionId) {
        validarId(organizacionId, "organizacionId");
        return repository.listarPorOrganizacion(organizacionId);
    }

    public BaselineConfiguracion buscarPorId(Long id) {
        validarId(id, "id");
        return repository.buscarPorId(id)
                .orElseThrow(() -> new NoSuchElementException("No existe la baseline solicitada."));
    }

    public BaselineConfiguracion buscarPorIdYOrganizacion(Long id, Long organizacionId) {
        validarId(id, "id");
        validarId(organizacionId, "organizacionId");
        return repository.buscarPorIdYOrganizacion(id, organizacionId)
                .orElseThrow(() -> new NoSuchElementException("No existe la baseline solicitada."));
    }

    private String actorSeguro(String actor) {
        return actor == null || actor.isBlank() ? "SISTEMA_O_ANONIMO" : actor;
    }

    private void validarTexto(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("El campo " + campo + " es obligatorio.");
        }
    }

    private void validarId(Long valor, String campo) {
        if (valor == null || valor <= 0) {
            throw new IllegalArgumentException("El campo " + campo + " debe ser un identificador válido.");
        }
    }
}
