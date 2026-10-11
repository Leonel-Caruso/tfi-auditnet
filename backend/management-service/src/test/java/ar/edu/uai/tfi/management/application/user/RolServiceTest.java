package ar.edu.uai.tfi.management.application.user;

import ar.edu.uai.tfi.management.application.ErrorAplicacion;
import ar.edu.uai.tfi.management.application.ExcepcionAplicacion;
import ar.edu.uai.tfi.management.domain.model.EventoSistema;
import ar.edu.uai.tfi.management.domain.model.Rol;
import ar.edu.uai.tfi.management.domain.model.TipoEventoSistema;
import ar.edu.uai.tfi.management.support.EnMemoriaGestion;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Bloque 6 · Perfiles fijos (D-29): solo se edita la descripción; la matriz cubre los cuatro perfiles. */
class RolServiceTest {

    @Test
    void actualizarDescripcionRegistraElCambioSinTocarElNombre() {
        List<EventoSistema> eventos = new ArrayList<>();
        RolService service = new RolService(new EnMemoriaGestion.Roles(), eventos::add);

        Rol actualizado = service.actualizarDescripcion(2L, "  Opera la red del cliente.  ");

        assertEquals("ANALISTA_RED", actualizado.nombre());
        assertEquals("Opera la red del cliente.", actualizado.descripcion());
        assertEquals(TipoEventoSistema.ROL_MODIFICADO, eventos.get(0).tipo());
        assertEquals(ErrorAplicacion.SOLICITUD_INVALIDA, assertThrows(ExcepcionAplicacion.class,
                () -> service.actualizarDescripcion(2L, " ")).tipo());
        assertEquals(ErrorAplicacion.SOLICITUD_INVALIDA, assertThrows(ExcepcionAplicacion.class,
                () -> service.actualizarDescripcion(2L, "x".repeat(241))).tipo());
        assertEquals(ErrorAplicacion.NO_ENCONTRADO, assertThrows(ExcepcionAplicacion.class,
                () -> service.actualizarDescripcion(99L, "Nada")).tipo());
    }

    @Test
    void laMatrizDePermisosUsaSoloLosCuatroPerfilesYElAdministradorTieneTodo() {
        Set<String> usados = new HashSet<>();
        for (MatrizPermisos.Permiso permiso : MatrizPermisos.PERMISOS) {
            assertFalse(permiso.perfiles().isEmpty(), permiso.modulo());
            assertTrue(permiso.perfiles().contains(MatrizPermisos.ADMIN), permiso.modulo() + " / " + permiso.accion());
            usados.addAll(permiso.perfiles());
        }
        assertEquals(Set.copyOf(MatrizPermisos.PERFILES), usados);
        assertTrue(MatrizPermisos.PERMISOS.stream().anyMatch(p -> p.modulo().equals("Mi cuenta")
                && p.perfiles().size() == 4));
    }
}
