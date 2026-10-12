package ar.edu.uai.tfi.management.application.user;

import ar.edu.uai.tfi.management.application.ErrorAplicacion;
import ar.edu.uai.tfi.management.application.ExcepcionAplicacion;
import ar.edu.uai.tfi.management.application.port.BitacoraSistemaPort;
import ar.edu.uai.tfi.management.domain.model.EventoSistema;
import ar.edu.uai.tfi.management.domain.model.Rol;
import ar.edu.uai.tfi.management.domain.model.TipoEventoSistema;
import ar.edu.uai.tfi.management.domain.repository.RolRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;

/**
 * Los cuatro perfiles son fijos: sus permisos están en el código ({@code @RolesAllowed}) y se documentan en
 * {@link MatrizPermisos}. Solo se edita la descripción; un perfil nuevo no tendría permisos.
 */
@ApplicationScoped
public class RolService {

    private final RolRepository repository;
    private final BitacoraSistemaPort bitacora;

    public RolService(RolRepository repository, BitacoraSistemaPort bitacora) {
        this.repository = repository;
        this.bitacora = bitacora;
    }

    public List<Rol> listar() {
        return repository.listar();
    }

    @Transactional
    public Rol actualizarDescripcion(Long id, String descripcion) {
        if (descripcion == null || descripcion.isBlank()) {
            throw new ExcepcionAplicacion(ErrorAplicacion.SOLICITUD_INVALIDA, "La descripción es obligatoria.");
        }
        if (descripcion.trim().length() > 240) {
            throw new ExcepcionAplicacion(ErrorAplicacion.SOLICITUD_INVALIDA,
                    "La descripción no puede superar los 240 caracteres.");
        }
        Rol anterior = repository.buscarPorId(id)
                .orElseThrow(() -> new ExcepcionAplicacion(ErrorAplicacion.NO_ENCONTRADO, "El perfil no fue encontrado."));
        Rol actualizado = repository.actualizar(new Rol(anterior.id(), anterior.nombre(), descripcion.trim(),
                anterior.estado()));
        bitacora.registrar(EventoSistema.exito(TipoEventoSistema.ROL_MODIFICADO, null, null, null,
                "rol=" + actualizado.nombre() + ", campo=descripcion"));
        return actualizado;
    }
}
