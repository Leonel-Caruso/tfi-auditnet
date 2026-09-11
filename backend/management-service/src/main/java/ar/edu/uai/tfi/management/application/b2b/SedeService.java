package ar.edu.uai.tfi.management.application.b2b;

import ar.edu.uai.tfi.management.application.ErrorAplicacion;
import ar.edu.uai.tfi.management.application.ExcepcionAplicacion;
import ar.edu.uai.tfi.management.application.port.TrazabilidadPort;
import ar.edu.uai.tfi.management.domain.model.EstadoRegistro;
import ar.edu.uai.tfi.management.domain.model.Sede;
import ar.edu.uai.tfi.management.domain.repository.OrganizacionClienteRepository;
import ar.edu.uai.tfi.management.domain.repository.SedeRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;

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

    public Sede buscarPorId(Long id) {
        if (id == null) {
            throw new ExcepcionAplicacion(ErrorAplicacion.SOLICITUD_INVALIDA, "El id de sede es obligatorio.");
        }
        return sedeRepository.buscarPorId(id)
                .orElseThrow(() -> new ExcepcionAplicacion(ErrorAplicacion.NO_ENCONTRADO, "La sede no fue encontrada."));
    }

    @Transactional
    public Sede crear(Long organizacionId, String nombre, String ubicacion) {
        validarOrganizacion(organizacionId);
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

        trazabilidad.registrar("SEDE_CREADA", "sedeId=" + creada.id() + ", organizacionId=" + organizacionId);
        return creada;
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
