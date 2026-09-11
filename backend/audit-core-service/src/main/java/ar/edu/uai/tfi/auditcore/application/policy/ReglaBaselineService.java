package ar.edu.uai.tfi.auditcore.application.policy;

import ar.edu.uai.tfi.auditcore.application.port.TrazabilidadPort;
import ar.edu.uai.tfi.auditcore.domain.model.EstadoBaseline;
import ar.edu.uai.tfi.auditcore.domain.model.EstadoRegla;
import ar.edu.uai.tfi.auditcore.domain.model.ReglaBaseline;
import ar.edu.uai.tfi.auditcore.domain.model.SeveridadRegla;
import ar.edu.uai.tfi.auditcore.domain.model.TipoReglaConfiguracion;
import ar.edu.uai.tfi.auditcore.domain.repository.BaselineRepository;
import ar.edu.uai.tfi.auditcore.domain.repository.ReglaBaselineRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@ApplicationScoped
public class ReglaBaselineService {

    private final ReglaBaselineRepository repository;
    private final BaselineRepository baselineRepository;
    private final TrazabilidadPort trazabilidad;

    public ReglaBaselineService(
            ReglaBaselineRepository repository,
            BaselineRepository baselineRepository,
            TrazabilidadPort trazabilidad
    ) {
        this.repository = repository;
        this.baselineRepository = baselineRepository;
        this.trazabilidad = trazabilidad;
    }

    @Transactional
    public ReglaBaseline crear(
            Long baselineId,
            String codigo,
            String nombre,
            String descripcion,
            String tipo,
            String patron,
            String severidad,
            String recomendacion,
            String actor
    ) {
        validarId(baselineId, "baselineId");
        validarTexto(codigo, "codigo");
        validarTexto(nombre, "nombre");
        validarTexto(descripcion, "descripcion");
        validarTexto(patron, "patron");
        validarTexto(recomendacion, "recomendacion");

        var baseline = baselineRepository.buscarPorId(baselineId)
                .orElseThrow(() -> new NoSuchElementException("No existe la baseline seleccionada."));
        if (baseline.estado() != EstadoBaseline.ACTIVO) {
            throw new IllegalStateException("No se pueden agregar reglas a una baseline inactiva.");
        }

        String codigoNormalizado = codigo.trim().toUpperCase();
        if (repository.existeCodigoEnBaseline(codigoNormalizado, baselineId)) {
            throw new IllegalStateException("Ya existe una regla con el código " + codigoNormalizado + " en esa baseline.");
        }

        ReglaBaseline nueva = new ReglaBaseline(
                null,
                baselineId,
                codigoNormalizado,
                nombre.trim(),
                descripcion.trim(),
                parsearTipo(tipo),
                patron.trim(),
                parsearSeveridad(severidad),
                recomendacion.trim(),
                EstadoRegla.ACTIVA
        );

        ReglaBaseline creada = repository.guardar(nueva);
        trazabilidad.registrar(
                actorSeguro(actor),
                "REGLA_CREADA",
                "reglaId=" + creada.id() + ", baselineId=" + creada.baselineId() + ", codigo=" + creada.codigo()
        );
        return creada;
    }

    public List<ReglaBaseline> listar() {
        return repository.listar();
    }

    public List<ReglaBaseline> listarPorOrganizacion(Long organizacionId) {
        validarId(organizacionId, "organizacionId");
        return repository.listarPorOrganizacion(organizacionId);
    }

    public List<ReglaBaseline> listarPorBaseline(Long baselineId) {
        validarId(baselineId, "baselineId");
        return repository.listarPorBaseline(baselineId);
    }

    public List<ReglaBaseline> listarPorBaselineYOrganizacion(Long baselineId, Long organizacionId) {
        validarId(baselineId, "baselineId");
        validarId(organizacionId, "organizacionId");
        return repository.listarPorBaselineYOrganizacion(baselineId, organizacionId);
    }

    public ReglaBaseline buscarPorId(Long id) {
        validarId(id, "id");
        return repository.buscarPorId(id)
                .orElseThrow(() -> new NoSuchElementException("No existe la regla solicitada."));
    }

    public ReglaBaseline buscarPorIdYOrganizacion(Long id, Long organizacionId) {
        validarId(id, "id");
        validarId(organizacionId, "organizacionId");
        return repository.buscarPorIdYOrganizacion(id, organizacionId)
                .orElseThrow(() -> new NoSuchElementException("No existe la regla solicitada."));
    }

    public long contarPorBaseline(Long baselineId) {
        validarId(baselineId, "baselineId");
        return repository.contarPorBaseline(baselineId);
    }

    private TipoReglaConfiguracion parsearTipo(String valor) {
        validarTexto(valor, "tipo");
        try {
            return TipoReglaConfiguracion.valueOf(valor.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("El tipo de regla debe ser DEBE_CONTENER o NO_DEBE_CONTENER.");
        }
    }

    private SeveridadRegla parsearSeveridad(String valor) {
        validarTexto(valor, "severidad");
        try {
            return SeveridadRegla.valueOf(valor.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("La severidad debe ser BAJA, MEDIA, ALTA o CRITICA.");
        }
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
