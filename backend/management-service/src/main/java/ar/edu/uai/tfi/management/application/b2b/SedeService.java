package ar.edu.uai.tfi.management.application.b2b;

import ar.edu.uai.tfi.management.application.ErrorAplicacion;
import ar.edu.uai.tfi.management.application.ExcepcionAplicacion;
import ar.edu.uai.tfi.management.application.port.TrazabilidadPort;
import ar.edu.uai.tfi.management.domain.model.EstadoRegistro;
import ar.edu.uai.tfi.management.domain.model.OperacionTransaccion;
import ar.edu.uai.tfi.management.domain.model.Transaccion;
import ar.edu.uai.tfi.management.domain.model.Sede;
import ar.edu.uai.tfi.management.domain.repository.OrganizacionClienteRepository;
import ar.edu.uai.tfi.management.domain.repository.SedeRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;

/**
 * Sedes de las organizaciones cliente. Las operaciones reciben el alcance del usuario: null para el administrador del
 * sistema (cualquier organización) o la organización del administrador de organización (solo la propia).
 */
@ApplicationScoped
public class SedeService {
    private final SedeRepository sedeRepository;
    private final OrganizacionClienteRepository organizacionRepository;
    private final TrazabilidadPort trazabilidad;

    public SedeService(SedeRepository sedeRepository,
                       OrganizacionClienteRepository organizacionRepository,
                       TrazabilidadPort trazabilidad) {
        this.sedeRepository = sedeRepository;
        this.organizacionRepository = organizacionRepository;
        this.trazabilidad = trazabilidad;
    }

    public List<Sede> listarPorOrganizacion(Long organizacionId) {
        validarOrganizacion(organizacionId);
        return sedeRepository.listarPorOrganizacion(organizacionId);
    }

    public Sede buscarPorId(Long id, Long alcance) {
        if (id == null) {
            throw new ExcepcionAplicacion(ErrorAplicacion.SOLICITUD_INVALIDA, "El id de sede es obligatorio.");
        }
        Sede sede = sedeRepository.buscarPorId(id)
                .orElseThrow(() -> new ExcepcionAplicacion(ErrorAplicacion.NO_ENCONTRADO, "La sede no fue encontrada."));
        verificarAlcance(sede.organizacionId(), alcance);
        return sede;
    }

    @Transactional
    public Sede crear(Long organizacionId, String nombre, String ubicacion, Long alcance) {
        verificarAlcance(organizacionId, alcance);
        validarOrganizacion(organizacionId);
        if (organizacionRepository.buscarPorId(organizacionId).map(o -> o.estado() != EstadoRegistro.ACTIVO).orElse(false)) {
            throw new ExcepcionAplicacion(ErrorAplicacion.CONFLICTO,
                    "La organización está deshabilitada: no se le pueden agregar sedes.");
        }
        validarObligatorio(nombre, "nombre");
        validarObligatorio(ubicacion, "ubicacion");

        String nombreNormalizado = nombre.trim();
        if (sedeRepository.buscarPorOrganizacionYNombre(organizacionId, nombreNormalizado).isPresent()) {
            throw new ExcepcionAplicacion(ErrorAplicacion.CONFLICTO,
                    "Ya existe una sede con ese nombre para la organización seleccionada.");
        }

        Sede creada = sedeRepository.guardar(new Sede(
                null,
                organizacionId,
                nombreNormalizado,
                ubicacion.trim(),
                EstadoRegistro.ACTIVO));

        trazabilidad.registrar(Transaccion.alta("SEDE", creada.id(), organizacionId, creada));
        return creada;
    }

    /** El nombre sigue siendo único dentro de la organización. */
    @Transactional
    public Sede actualizar(Long id, String nombre, String ubicacion, Long alcance) {
        Sede anterior = buscarPorId(id, alcance);
        validarObligatorio(nombre, "nombre");
        validarObligatorio(ubicacion, "ubicacion");
        String nombreNormalizado = nombre.trim();
        if (sedeRepository.buscarPorOrganizacionYNombre(anterior.organizacionId(), nombreNormalizado)
                .filter(otra -> !otra.id().equals(id)).isPresent()) {
            throw new ExcepcionAplicacion(ErrorAplicacion.CONFLICTO,
                    "Ya existe otra sede con ese nombre en la organización.");
        }
        Sede actualizada = sedeRepository.actualizar(new Sede(id, anterior.organizacionId(), nombreNormalizado,
                ubicacion.trim(), anterior.estado()));
        trazabilidad.registrar(new Transaccion("SEDE", id, OperacionTransaccion.MODIFICACION,
                anterior.organizacionId(), anterior, actualizada, null, null));
        return actualizada;
    }

    /** Baja lógica: una sede deshabilitada no admite dispositivos nuevos; los que tenía conservan su historial. */
    @Transactional
    public Sede cambiarEstado(Long id, String estado, Long alcance) {
        Sede anterior = buscarPorId(id, alcance);
        EstadoRegistro nuevo = OrganizacionClienteService.parsearEstado(estado);
        if (anterior.estado() == nuevo) {
            throw new ExcepcionAplicacion(ErrorAplicacion.CONFLICTO, "La sede ya está " + nuevo + ".");
        }
        Sede actualizada = sedeRepository.actualizar(new Sede(id, anterior.organizacionId(), anterior.nombre(),
                anterior.ubicacion(), nuevo));
        trazabilidad.registrar(new Transaccion("SEDE", id, OperacionTransaccion.CAMBIO_ESTADO,
                anterior.organizacionId(), anterior, actualizada, null, anterior.estado() + " -> " + nuevo));
        return actualizada;
    }

    private static void verificarAlcance(Long organizacionId, Long alcance) {
        if (alcance != null && !alcance.equals(organizacionId)) {
            throw new ExcepcionAplicacion(ErrorAplicacion.PROHIBIDO, "Solo podés gestionar las sedes de tu organización.");
        }
    }

    private void validarOrganizacion(Long organizacionId) {
        if (organizacionId == null || organizacionRepository.buscarPorId(organizacionId).isEmpty()) {
            throw new ExcepcionAplicacion(ErrorAplicacion.NO_ENCONTRADO,
                    "La organización cliente no fue encontrada.");
        }
    }

    private void validarObligatorio(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new ExcepcionAplicacion(ErrorAplicacion.SOLICITUD_INVALIDA,
                    "El campo " + campo + " es obligatorio.");
        }
    }
}
