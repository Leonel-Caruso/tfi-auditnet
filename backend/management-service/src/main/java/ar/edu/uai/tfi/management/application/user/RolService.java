package ar.edu.uai.tfi.management.application.user;

import ar.edu.uai.tfi.management.application.ErrorAplicacion;
import ar.edu.uai.tfi.management.application.ExcepcionAplicacion;
import ar.edu.uai.tfi.management.application.port.TrazabilidadPort;
import ar.edu.uai.tfi.management.domain.model.EstadoRegistro;
import ar.edu.uai.tfi.management.domain.model.Rol;
import ar.edu.uai.tfi.management.domain.repository.RolRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Locale;

@ApplicationScoped
public class RolService {
    private final RolRepository repository;
    private final TrazabilidadPort trazabilidad;

    public RolService(RolRepository repository, TrazabilidadPort trazabilidad) {
        this.repository = repository;
        this.trazabilidad = trazabilidad;
    }

    public List<Rol> listar() {
        return repository.listar();
    }

    @Transactional
    public Rol crear(String nombre, String descripcion) {
        validarObligatorio(nombre, "nombre");
        validarObligatorio(descripcion, "descripcion");
        String nombreNormalizado = nombre.trim().toUpperCase(Locale.ROOT);

        if (repository.buscarPorNombre(nombreNormalizado).isPresent()) {
            throw new ExcepcionAplicacion(ErrorAplicacion.CONFLICTO, "Ya existe un rol con ese nombre.");
        }

        Rol creado = repository.guardar(new Rol(null, nombreNormalizado, descripcion.trim(), EstadoRegistro.ACTIVO));
        trazabilidad.registrar("ROL_CREADO", "rolId=" + creado.id() + ", nombre=" + creado.nombre());
        return creado;
    }

    private void validarObligatorio(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new ExcepcionAplicacion(ErrorAplicacion.SOLICITUD_INVALIDA,
                    "El campo " + campo + " es obligatorio.");
        }
    }
}
