package ar.edu.uai.tfi.auditcore.application.policy;

import ar.edu.uai.tfi.auditcore.application.port.TrazabilidadPort;
import ar.edu.uai.tfi.auditcore.domain.model.BaselineConfiguracion;
import ar.edu.uai.tfi.auditcore.domain.model.EstadoBaseline;
import ar.edu.uai.tfi.auditcore.domain.model.EstadoRegla;
import ar.edu.uai.tfi.auditcore.domain.model.OperacionTransaccion;
import ar.edu.uai.tfi.auditcore.domain.model.ReglaBaseline;
import ar.edu.uai.tfi.auditcore.domain.model.Transaccion;
import ar.edu.uai.tfi.auditcore.domain.repository.AuditoriaRepository;
import ar.edu.uai.tfi.auditcore.domain.repository.BaselineRepository;
import ar.edu.uai.tfi.auditcore.domain.repository.ReglaBaselineRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Gestión de baselines (RF-005, CU-003-001).
 *
 * Reglas de negocio:
 * - El alcance de una baseline es la organización y el tipo de dispositivo. Solo una baseline
 *   puede estar ACTIVA por alcance: es la que usa el motor de auditoría.
 * - El nombre y el alcance identifican a la baseline; cambiar sus criterios (reglas) genera una
 *   versión nueva, que copia las reglas activas y reemplaza a la versión vigente.
 * - Una baseline usada en auditorías no se sobrescribe: sus reglas quedan congeladas.
 */
@ApplicationScoped
public class BaselineService {

    static final String ENTIDAD = "BASELINE";

    private final BaselineRepository repository;
    private final ReglaBaselineRepository reglaRepository;
    private final AuditoriaRepository auditoriaRepository;
    private final TrazabilidadPort trazabilidad;

    public BaselineService(BaselineRepository repository,
                           ReglaBaselineRepository reglaRepository,
                           AuditoriaRepository auditoriaRepository,
                           TrazabilidadPort trazabilidad) {
        this.repository = repository;
        this.reglaRepository = reglaRepository;
        this.auditoriaRepository = auditoriaRepository;
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
        validarTexto(nombre, "nombre", 140);
        validarTexto(descripcion, "descripcion", 500);
        validarId(tipoDispositivoId, "tipoDispositivoId");
        validarId(organizacionId, "organizacionId");

        String nombreNormalizado = nombre.trim();
        if (repository.existeNombreEnOrganizacion(nombreNormalizado, organizacionId)) {
            throw new IllegalStateException("Ya existe una baseline con ese nombre para la organización seleccionada. "
                    + "Para cambiarla, generá una nueva versión.");
        }
        validarSinOtraActiva(organizacionId, tipoDispositivoId, null);

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
        trazabilidad.registrar(Transaccion.alta(ENTIDAD, creada.id(), creada.organizacionId(), creada,
                actorSeguro(actor)));
        return creada;
    }

    /**
     * Modifica la descripción. No cambia los criterios de auditoría, por lo que no genera versión.
     */
    @Transactional
    public BaselineConfiguracion modificarDescripcion(Long id, String descripcion, String actor) {
        validarId(id, "id");
        validarTexto(descripcion, "descripcion", 500);
        BaselineConfiguracion anterior = buscarPorId(id);
        if (anterior.descripcion().equals(descripcion.trim())) {
            return anterior;
        }

        BaselineConfiguracion actualizada = repository.actualizar(new BaselineConfiguracion(
                anterior.id(), anterior.nombre(), descripcion.trim(), anterior.version(),
                anterior.tipoDispositivoId(), anterior.organizacionId(), anterior.estado()
        ));
        trazabilidad.registrar(new Transaccion(ENTIDAD, actualizada.id(), OperacionTransaccion.MODIFICACION,
                actualizada.organizacionId(), anterior, actualizada, actorSeguro(actor), null));
        return actualizada;
    }

    /**
     * Activa o desactiva una baseline. Para activarla no debe haber otra activa en el mismo alcance.
     */
    @Transactional
    public BaselineConfiguracion cambiarEstado(Long id, String estado, String actor) {
        validarId(id, "id");
        EstadoBaseline nuevoEstado = parsearEstado(estado);
        BaselineConfiguracion anterior = buscarPorId(id);

        if (anterior.estado() == nuevoEstado) {
            throw new IllegalStateException("La baseline " + anterior.nombre() + " v" + anterior.version()
                    + " ya está " + nuevoEstado + ".");
        }
        if (nuevoEstado == EstadoBaseline.ACTIVO) {
            validarSinOtraActiva(anterior.organizacionId(), anterior.tipoDispositivoId(), anterior.id());
        }

        return aplicarEstado(anterior, nuevoEstado, actor, anterior.estado() + " -> " + nuevoEstado);
    }

    /**
     * Genera una versión nueva a partir de una existente: copia sus reglas activas, queda ACTIVA y
     * desactiva la baseline que estaba vigente en el mismo alcance. La versión anterior y sus
     * auditorías no se modifican.
     *
     * @param descripcion descripción de la versión nueva; si es null se conserva la de origen
     */
    @Transactional
    public BaselineConfiguracion nuevaVersion(Long id, String descripcion, String actor) {
        validarId(id, "id");
        BaselineConfiguracion origen = buscarPorId(id);
        String descripcionNueva = descripcion == null || descripcion.isBlank()
                ? origen.descripcion()
                : descripcion.trim();
        validarTexto(descripcionNueva, "descripcion", 500);

        List<BaselineConfiguracion> versiones = repository.listarVersiones(origen.organizacionId(), origen.nombre());
        int siguiente = versiones.stream().mapToInt(BaselineConfiguracion::version).max().orElse(origen.version()) + 1;

        for (BaselineConfiguracion vigente : repository.listarActivasDelAlcance(
                origen.organizacionId(), origen.tipoDispositivoId())) {
            aplicarEstado(vigente, EstadoBaseline.INACTIVO, actor,
                    "Reemplazada por " + origen.nombre() + " v" + siguiente);
        }

        BaselineConfiguracion creada = repository.guardar(new BaselineConfiguracion(
                null, origen.nombre(), descripcionNueva, siguiente,
                origen.tipoDispositivoId(), origen.organizacionId(), EstadoBaseline.ACTIVO
        ));

        List<ReglaBaseline> reglasOrigen = reglaRepository.listarPorBaseline(origen.id()).stream()
                .filter(regla -> regla.estado() == EstadoRegla.ACTIVA)
                .toList();
        for (ReglaBaseline regla : reglasOrigen) {
            ReglaBaseline copia = reglaRepository.guardar(new ReglaBaseline(
                    null, creada.id(), regla.codigo(), regla.nombre(), regla.descripcion(), regla.tipo(),
                    regla.patron(), regla.valorEsperado(), regla.severidad(), regla.impacto(),
                    regla.recomendacion(), EstadoRegla.ACTIVA
            ));
            trazabilidad.registrar(new Transaccion(ReglaBaselineService.ENTIDAD, copia.id(),
                    OperacionTransaccion.ALTA, creada.organizacionId(), null, copia, actorSeguro(actor),
                    "Copiada de la regla #" + regla.id() + " (" + origen.nombre() + " v" + origen.version() + ")"));
        }

        trazabilidad.registrar(new Transaccion(ENTIDAD, creada.id(), OperacionTransaccion.ALTA,
                creada.organizacionId(), null, creada, actorSeguro(actor),
                "Versión " + siguiente + " generada a partir de la baseline #" + origen.id()
                        + " (v" + origen.version() + "); reglas copiadas: " + reglasOrigen.size()));
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

    /** Cantidad de auditorías que usaron una baseline. */
    public long auditoriasDe(Long baselineId) {
        return auditoriaRepository.contarPorBaseline(baselineId);
    }

    /** Cantidad de auditorías por baseline (las que no figuran no fueron usadas). */
    public Map<Long, Long> auditoriasPorBaseline() {
        return auditoriaRepository.contarPorBaselines();
    }

    private BaselineConfiguracion aplicarEstado(BaselineConfiguracion anterior, EstadoBaseline nuevoEstado,
                                                String actor, String detalle) {
        BaselineConfiguracion actualizada = repository.actualizar(new BaselineConfiguracion(
                anterior.id(), anterior.nombre(), anterior.descripcion(), anterior.version(),
                anterior.tipoDispositivoId(), anterior.organizacionId(), nuevoEstado
        ));
        trazabilidad.registrar(new Transaccion(ENTIDAD, actualizada.id(), OperacionTransaccion.CAMBIO_ESTADO,
                actualizada.organizacionId(), anterior, actualizada, actorSeguro(actor), detalle));
        return actualizada;
    }

    private void validarSinOtraActiva(Long organizacionId, Long tipoDispositivoId, Long excluirId) {
        repository.listarActivasDelAlcance(organizacionId, tipoDispositivoId).stream()
                .filter(activa -> !activa.id().equals(excluirId))
                .findFirst()
                .ifPresent(activa -> {
                    throw new IllegalStateException("Ya existe la baseline activa " + activa.nombre() + " v"
                            + activa.version() + " para ese tipo de dispositivo y organización. "
                            + "Generá una nueva versión de esa baseline o desactivala primero.");
                });
    }

    private EstadoBaseline parsearEstado(String valor) {
        validarTexto(valor, "estado", 20);
        try {
            return EstadoBaseline.valueOf(valor.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("El estado debe ser ACTIVO o INACTIVO.");
        }
    }

    private String actorSeguro(String actor) {
        return actor == null || actor.isBlank() ? "SISTEMA_O_ANONIMO" : actor;
    }

    private void validarTexto(String valor, String campo, int maximo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("El campo " + campo + " es obligatorio.");
        }
        if (valor.trim().length() > maximo) {
            throw new IllegalArgumentException("El campo " + campo + " no puede superar los " + maximo + " caracteres.");
        }
    }

    private void validarId(Long valor, String campo) {
        if (valor == null || valor <= 0) {
            throw new IllegalArgumentException("El campo " + campo + " debe ser un identificador válido.");
        }
    }
}
