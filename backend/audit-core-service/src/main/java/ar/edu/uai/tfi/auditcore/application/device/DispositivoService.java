package ar.edu.uai.tfi.auditcore.application.device;

import ar.edu.uai.tfi.auditcore.application.port.TrazabilidadPort;
import ar.edu.uai.tfi.auditcore.domain.model.CriticidadDispositivo;
import ar.edu.uai.tfi.auditcore.domain.model.DispositivoRed;
import ar.edu.uai.tfi.auditcore.domain.model.EstadoDispositivo;
import ar.edu.uai.tfi.auditcore.domain.model.OperacionTransaccion;
import ar.edu.uai.tfi.auditcore.domain.model.Transaccion;
import ar.edu.uai.tfi.auditcore.domain.repository.DispositivoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;

/**
 * Gestión de dispositivos de red (RF-003, CU-002-001).
 * El identificador es único dentro de cada organización y la baja es siempre lógica.
 */
@ApplicationScoped
public class DispositivoService {

    static final String ENTIDAD = "DISPOSITIVO";

    private final DispositivoRepository repository;
    private final TrazabilidadPort trazabilidad;

    public DispositivoService(
            DispositivoRepository repository,
            TrazabilidadPort trazabilidad
    ) {
        this.repository = repository;
        this.trazabilidad = trazabilidad;
    }

    @Transactional
    public DispositivoRed crear(
            String nombre,
            String identificador,
            Long tipoDispositivoId,
            String fabricante,
            Long organizacionId,
            Long sedeId,
            String criticidad,
            String actor
    ) {
        validarTexto(nombre, "nombre");
        validarTexto(identificador, "identificador");
        validarTexto(fabricante, "fabricante");
        validarId(tipoDispositivoId, "tipoDispositivoId");
        validarId(organizacionId, "organizacionId");
        validarSede(sedeId);

        String identificadorNormalizado = normalizarIdentificador(identificador);
        validarIdentificadorDisponible(identificadorNormalizado, organizacionId, null);

        DispositivoRed nuevo = new DispositivoRed(
                null,
                nombre.trim(),
                identificadorNormalizado,
                tipoDispositivoId,
                fabricante.trim(),
                organizacionId,
                sedeId,
                parsearCriticidad(criticidad),
                EstadoDispositivo.ACTIVO
        );

        DispositivoRed creado = repository.guardar(nuevo);

        trazabilidad.registrar(Transaccion.alta(ENTIDAD, creado.id(), creado.organizacionId(), creado,
                actorOpcional(actor)));

        return creado;
    }

    /**
     * Modifica los datos de un dispositivo. La organización no se puede cambiar: un activo no pasa
     * de un cliente a otro. El estado se cambia con {@link #cambiarEstado}.
     */
    @Transactional
    public DispositivoRed modificar(
            Long id,
            String nombre,
            String identificador,
            Long tipoDispositivoId,
            String fabricante,
            Long sedeId,
            String criticidad,
            String actor
    ) {
        validarId(id, "id");
        validarTexto(nombre, "nombre");
        validarTexto(identificador, "identificador");
        validarTexto(fabricante, "fabricante");
        validarId(tipoDispositivoId, "tipoDispositivoId");
        validarSede(sedeId);

        DispositivoRed anterior = buscarPorId(id);
        String identificadorNormalizado = normalizarIdentificador(identificador);
        validarIdentificadorDisponible(identificadorNormalizado, anterior.organizacionId(), anterior.id());

        DispositivoRed cambios = new DispositivoRed(
                anterior.id(),
                nombre.trim(),
                identificadorNormalizado,
                tipoDispositivoId,
                fabricante.trim(),
                anterior.organizacionId(),
                sedeId,
                parsearCriticidad(criticidad),
                anterior.estado()
        );

        if (cambios.equals(anterior)) {
            return anterior;
        }

        DispositivoRed actualizado = repository.actualizar(cambios);
        trazabilidad.registrar(new Transaccion(ENTIDAD, actualizado.id(), OperacionTransaccion.MODIFICACION,
                actualizado.organizacionId(), anterior, actualizado, actorOpcional(actor), null));
        return actualizado;
    }

    /**
     * Baja lógica (INACTIVO) o reactivación (ACTIVO). Un dispositivo inactivo conserva su historial
     * pero no admite nuevas configuraciones ni auditorías.
     */
    @Transactional
    public DispositivoRed cambiarEstado(Long id, String estado, String actor) {
        validarId(id, "id");
        EstadoDispositivo nuevoEstado = parsearEstado(estado);
        DispositivoRed anterior = buscarPorId(id);

        if (anterior.estado() == nuevoEstado) {
            throw new IllegalStateException("El dispositivo " + anterior.identificador() + " ya está " + nuevoEstado + ".");
        }

        DispositivoRed actualizado = repository.actualizar(new DispositivoRed(
                anterior.id(), anterior.nombre(), anterior.identificador(), anterior.tipoDispositivoId(),
                anterior.fabricante(), anterior.organizacionId(), anterior.sedeId(), anterior.criticidad(),
                nuevoEstado
        ));

        OperacionTransaccion operacion = nuevoEstado == EstadoDispositivo.INACTIVO
                ? OperacionTransaccion.BAJA_LOGICA
                : OperacionTransaccion.CAMBIO_ESTADO;
        trazabilidad.registrar(new Transaccion(ENTIDAD, actualizado.id(), operacion, actualizado.organizacionId(),
                anterior, actualizado, actorOpcional(actor), anterior.estado() + " -> " + nuevoEstado));
        return actualizado;
    }

    public List<DispositivoRed> listar() {
        return repository.listar();
    }

    public List<DispositivoRed> listarPorOrganizacion(Long organizacionId) {
        validarId(organizacionId, "organizacionId");
        return repository.listarPorOrganizacion(organizacionId);
    }

    public DispositivoRed buscarPorId(Long id) {
        validarId(id, "id");
        return repository.buscarPorId(id)
                .orElseThrow(() -> new NoSuchElementException("No existe el dispositivo solicitado."));
    }

    public DispositivoRed buscarPorIdYOrganizacion(Long id, Long organizacionId) {
        validarId(id, "id");
        validarId(organizacionId, "organizacionId");
        return repository.buscarPorIdYOrganizacion(id, organizacionId)
                .orElseThrow(() -> new NoSuchElementException("No existe el dispositivo solicitado."));
    }

    private void validarIdentificadorDisponible(String identificador, Long organizacionId, Long excluirId) {
        if (repository.existeIdentificadorEnOrganizacion(identificador, organizacionId, excluirId)) {
            throw new IllegalStateException(
                    "Ya existe un dispositivo con el identificador " + identificador + " en esta organización."
            );
        }
    }

    private String normalizarIdentificador(String identificador) {
        String normalizado = identificador.trim().toUpperCase(Locale.ROOT);
        if (normalizado.length() > 100) {
            throw new IllegalArgumentException("El identificador no puede superar los 100 caracteres.");
        }
        return normalizado;
    }

    private CriticidadDispositivo parsearCriticidad(String valor) {
        validarTexto(valor, "criticidad");
        try {
            return CriticidadDispositivo.valueOf(valor.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "La criticidad debe ser BAJA, MEDIA, ALTA o CRITICA."
            );
        }
    }

    private EstadoDispositivo parsearEstado(String valor) {
        validarTexto(valor, "estado");
        try {
            return EstadoDispositivo.valueOf(valor.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("El estado debe ser ACTIVO o INACTIVO.");
        }
    }

    private String actorOpcional(String actor) {
        return actor == null || actor.isBlank() ? null : actor;
    }

    private void validarSede(Long sedeId) {
        if (sedeId != null && sedeId <= 0) {
            throw new IllegalArgumentException("El campo sedeId debe ser un identificador válido.");
        }
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
