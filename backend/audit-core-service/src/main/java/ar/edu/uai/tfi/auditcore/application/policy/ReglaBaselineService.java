package ar.edu.uai.tfi.auditcore.application.policy;

import ar.edu.uai.tfi.auditcore.application.port.TrazabilidadPort;
import ar.edu.uai.tfi.auditcore.domain.model.BaselineConfiguracion;
import ar.edu.uai.tfi.auditcore.domain.model.EstadoBaseline;
import ar.edu.uai.tfi.auditcore.domain.model.EstadoRegla;
import ar.edu.uai.tfi.auditcore.domain.model.OperacionTransaccion;
import ar.edu.uai.tfi.auditcore.domain.model.ReglaBaseline;
import ar.edu.uai.tfi.auditcore.domain.model.SeveridadRegla;
import ar.edu.uai.tfi.auditcore.domain.model.TipoReglaConfiguracion;
import ar.edu.uai.tfi.auditcore.domain.model.Transaccion;
import ar.edu.uai.tfi.auditcore.domain.repository.AuditoriaRepository;
import ar.edu.uai.tfi.auditcore.domain.repository.BaselineRepository;
import ar.edu.uai.tfi.auditcore.domain.repository.ReglaBaselineRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;

/**
 * Gestión de reglas de auditoría (RF-006, CU-003-002).
 *
 * Reglas de negocio:
 * - La condición debe poder interpretarla el motor: texto de una sola línea y, para VALOR_ESPERADO,
 *   parámetro y valor esperado.
 * - No puede haber dos reglas activas con la misma condición en una baseline.
 * - Las reglas de una baseline ya usada en auditorías no se crean ni se modifican (se conserva la
 *   evidencia): el cambio se hace en una versión nueva de la baseline. La baja lógica sí se permite.
 */
@ApplicationScoped
public class ReglaBaselineService {

    static final String ENTIDAD = "REGLA";
    private static final Pattern CODIGO_VALIDO = Pattern.compile("^[A-Z0-9][A-Z0-9_.-]*$");

    private final ReglaBaselineRepository repository;
    private final BaselineRepository baselineRepository;
    private final AuditoriaRepository auditoriaRepository;
    private final TrazabilidadPort trazabilidad;

    public ReglaBaselineService(
            ReglaBaselineRepository repository,
            BaselineRepository baselineRepository,
            AuditoriaRepository auditoriaRepository,
            TrazabilidadPort trazabilidad
    ) {
        this.repository = repository;
        this.baselineRepository = baselineRepository;
        this.auditoriaRepository = auditoriaRepository;
        this.trazabilidad = trazabilidad;
    }

    /** Datos editables de una regla, ya sin normalizar. */
    public record DatosRegla(
            String nombre,
            String descripcion,
            String tipo,
            String patron,
            String valorEsperado,
            String severidad,
            String impacto,
            String recomendacion
    ) {
    }

    @Transactional
    public ReglaBaseline crear(Long baselineId, String codigo, DatosRegla datos, String actor) {
        validarId(baselineId, "baselineId");
        validarTexto(codigo, "codigo", 60);
        BaselineConfiguracion baseline = baselineEditable(baselineId);

        String codigoNormalizado = codigo.trim().toUpperCase(Locale.ROOT);
        if (!CODIGO_VALIDO.matcher(codigoNormalizado).matches()) {
            throw new IllegalArgumentException(
                    "El código solo admite letras, números, guiones, puntos y guiones bajos (ej. SEC-SSH-01).");
        }
        if (repository.existeCodigoEnBaseline(codigoNormalizado, baselineId)) {
            throw new IllegalStateException("Ya existe una regla con el código " + codigoNormalizado + " en esa baseline.");
        }

        ReglaBaseline nueva = construir(null, baselineId, codigoNormalizado, datos, EstadoRegla.ACTIVA);
        validarSinDuplicadoActivo(nueva);

        ReglaBaseline creada = repository.guardar(nueva);
        trazabilidad.registrar(Transaccion.alta(ENTIDAD, creada.id(), baseline.organizacionId(), creada,
                actorSeguro(actor)));
        return creada;
    }

    @Transactional
    public ReglaBaseline modificar(Long id, DatosRegla datos, String actor) {
        validarId(id, "id");
        ReglaBaseline anterior = buscarPorId(id);
        BaselineConfiguracion baseline = baselineEditable(anterior.baselineId());

        ReglaBaseline cambios = construir(anterior.id(), anterior.baselineId(), anterior.codigo(), datos,
                anterior.estado());
        if (cambios.equals(anterior)) {
            return anterior;
        }
        if (cambios.estado() == EstadoRegla.ACTIVA) {
            validarSinDuplicadoActivo(cambios);
        }

        ReglaBaseline actualizada = repository.actualizar(cambios);
        trazabilidad.registrar(new Transaccion(ENTIDAD, actualizada.id(), OperacionTransaccion.MODIFICACION,
                baseline.organizacionId(), anterior, actualizada, actorSeguro(actor), null));
        return actualizada;
    }

    /**
     * Baja lógica (INACTIVA) o reactivación (ACTIVA). La baja lógica se permite aunque la baseline tenga
     * auditorías (CU-003-002, flujo alternativo 6): la regla no se sobrescribe y las auditorías anteriores
     * conservan su copia. La reactivación agrega un criterio, por eso exige una baseline editable.
     */
    @Transactional
    public ReglaBaseline cambiarEstado(Long id, String estado, String actor) {
        validarId(id, "id");
        EstadoRegla nuevoEstado = parsearEstadoRegla(estado);
        ReglaBaseline anterior = buscarPorId(id);
        BaselineConfiguracion baseline = baselineRepository.buscarPorId(anterior.baselineId())
                .orElseThrow(() -> new NoSuchElementException("No existe la baseline de la regla."));

        if (anterior.estado() == nuevoEstado) {
            throw new IllegalStateException("La regla " + anterior.codigo() + " ya está " + nuevoEstado + ".");
        }
        if (nuevoEstado == EstadoRegla.ACTIVA) {
            // Reactivar agrega un criterio: solo en baselines activas y todavía no auditadas.
            baselineEditable(anterior.baselineId());
        }

        ReglaBaseline cambios = new ReglaBaseline(anterior.id(), anterior.baselineId(), anterior.codigo(),
                anterior.nombre(), anterior.descripcion(), anterior.tipo(), anterior.patron(),
                anterior.valorEsperado(), anterior.severidad(), anterior.impacto(), anterior.recomendacion(),
                nuevoEstado);
        if (nuevoEstado == EstadoRegla.ACTIVA) {
            validarSinDuplicadoActivo(cambios);
        }

        ReglaBaseline actualizada = repository.actualizar(cambios);
        OperacionTransaccion operacion = nuevoEstado == EstadoRegla.INACTIVA
                ? OperacionTransaccion.BAJA_LOGICA
                : OperacionTransaccion.CAMBIO_ESTADO;
        trazabilidad.registrar(new Transaccion(ENTIDAD, actualizada.id(), operacion, baseline.organizacionId(),
                anterior, actualizada, actorSeguro(actor), anterior.estado() + " -> " + nuevoEstado));
        return actualizada;
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

    /** La baseline debe existir, estar activa y no haber sido usada en auditorías. */
    private BaselineConfiguracion baselineEditable(Long baselineId) {
        BaselineConfiguracion baseline = baselineRepository.buscarPorId(baselineId)
                .orElseThrow(() -> new NoSuchElementException("No existe la baseline seleccionada."));
        if (baseline.estado() != EstadoBaseline.ACTIVO) {
            throw new IllegalStateException("La baseline " + baseline.nombre() + " v" + baseline.version()
                    + " está inactiva: sus reglas no se pueden crear ni modificar.");
        }
        long auditorias = auditoriaRepository.contarPorBaseline(baselineId);
        if (auditorias > 0) {
            throw new IllegalStateException("La baseline " + baseline.nombre() + " v" + baseline.version()
                    + " ya se usó en " + auditorias + (auditorias == 1 ? " auditoría" : " auditorías")
                    + ": sus reglas no se modifican para conservar la evidencia. "
                    + "Generá una nueva versión de la baseline y modificá las reglas allí.");
        }
        return baseline;
    }

    private ReglaBaseline construir(Long id, Long baselineId, String codigo, DatosRegla datos, EstadoRegla estado) {
        if (datos == null) {
            throw new IllegalArgumentException("Los datos de la regla son obligatorios.");
        }
        validarTexto(datos.nombre(), "nombre", 140);
        validarTexto(datos.descripcion(), "descripcion", 500);
        validarTexto(datos.impacto(), "impacto", 500);
        validarTexto(datos.recomendacion(), "recomendacion", 500);
        validarTexto(datos.patron(), "patron", 500);

        TipoReglaConfiguracion tipo = parsearTipo(datos.tipo());
        String patron = datos.patron().trim();
        validarUnaLinea(patron, tipo == TipoReglaConfiguracion.VALOR_ESPERADO ? "parámetro" : "patrón");

        String valorEsperado = null;
        boolean informaValor = datos.valorEsperado() != null && !datos.valorEsperado().isBlank();
        if (tipo == TipoReglaConfiguracion.VALOR_ESPERADO) {
            if (!informaValor) {
                throw new IllegalArgumentException("Una regla VALOR_ESPERADO necesita el valor esperado del parámetro.");
            }
            valorEsperado = datos.valorEsperado().trim();
            validarTexto(valorEsperado, "valorEsperado", 500);
            validarUnaLinea(valorEsperado, "valor esperado");
        } else if (informaValor) {
            throw new IllegalArgumentException("El valor esperado solo se usa en reglas de tipo VALOR_ESPERADO.");
        }

        return new ReglaBaseline(
                id,
                baselineId,
                codigo,
                datos.nombre().trim(),
                datos.descripcion().trim(),
                tipo,
                patron,
                valorEsperado,
                parsearSeveridad(datos.severidad()),
                datos.impacto().trim(),
                datos.recomendacion().trim(),
                estado
        );
    }

    /**
     * Evita dos reglas activas con la misma condición, o contradictorias (DEBE_CONTENER y NO_DEBE_CONTENER
     * del mismo patrón), dentro de una baseline.
     */
    private void validarSinDuplicadoActivo(ReglaBaseline regla) {
        repository.listarPorBaseline(regla.baselineId()).stream()
                .filter(existente -> existente.estado() == EstadoRegla.ACTIVA)
                .filter(existente -> !existente.id().equals(regla.id()))
                .filter(existente -> existente.patron().equalsIgnoreCase(regla.patron()))
                .filter(existente -> existente.tipo() == regla.tipo() || sonOpuestas(existente.tipo(), regla.tipo()))
                .findFirst()
                .ifPresent(existente -> {
                    throw new IllegalStateException(existente.tipo() == regla.tipo()
                            ? "La regla activa " + existente.codigo() + " ya evalúa la misma condición en esta baseline."
                            : "La regla activa " + existente.codigo() + " exige lo contrario para el mismo patrón ("
                                    + existente.tipo() + "): las dos no se pueden cumplir a la vez.");
                });
    }

    private static boolean sonOpuestas(TipoReglaConfiguracion a, TipoReglaConfiguracion b) {
        return (a == TipoReglaConfiguracion.DEBE_CONTENER && b == TipoReglaConfiguracion.NO_DEBE_CONTENER)
                || (a == TipoReglaConfiguracion.NO_DEBE_CONTENER && b == TipoReglaConfiguracion.DEBE_CONTENER);
    }

    private void validarUnaLinea(String valor, String campo) {
        if (valor.contains("\n") || valor.contains("\r")) {
            throw new IllegalArgumentException("El " + campo + " debe ser una sola línea de configuración.");
        }
    }

    private TipoReglaConfiguracion parsearTipo(String valor) {
        validarTexto(valor, "tipo", 30);
        try {
            return TipoReglaConfiguracion.valueOf(valor.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "El tipo de regla debe ser DEBE_CONTENER, NO_DEBE_CONTENER o VALOR_ESPERADO.");
        }
    }

    private SeveridadRegla parsearSeveridad(String valor) {
        validarTexto(valor, "severidad", 20);
        try {
            return SeveridadRegla.valueOf(valor.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("La severidad debe ser BAJA, MEDIA, ALTA o CRITICA.");
        }
    }

    private EstadoRegla parsearEstadoRegla(String valor) {
        validarTexto(valor, "estado", 20);
        try {
            return EstadoRegla.valueOf(valor.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("El estado debe ser ACTIVA o INACTIVA.");
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
