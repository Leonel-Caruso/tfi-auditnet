package ar.edu.uai.tfi.management.application.bitacora;

import ar.edu.uai.tfi.management.application.ErrorAplicacion;
import ar.edu.uai.tfi.management.application.ExcepcionAplicacion;
import ar.edu.uai.tfi.management.domain.model.FiltroBitacoraExcepciones;
import ar.edu.uai.tfi.management.domain.model.NivelExcepcion;
import ar.edu.uai.tfi.management.domain.model.RegistroExcepcion;
import ar.edu.uai.tfi.management.domain.repository.BitacoraExcepcionesRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Bloque 2.3c · Consulta de la bitácora de excepciones: filtros por servicio y nivel.
 */
class BitacoraExcepcionesServiceTest {

    private FiltroBitacoraExcepciones filtroRecibido;

    private final BitacoraExcepcionesService service = new BitacoraExcepcionesService(
            new BitacoraExcepcionesRepository() {
                @Override
                public void guardar(RegistroExcepcion registro) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public List<RegistroExcepcion> buscar(FiltroBitacoraExcepciones filtro) {
                    filtroRecibido = filtro;
                    return List.of();
                }
            });

    @Test
    void filtraPorServicioYNivel() {
        service.consultar("AUDIT-CORE-SERVICE", "error", null, null, 10);

        assertEquals("audit-core-service", filtroRecibido.servicio());
        assertEquals(NivelExcepcion.ERROR, filtroRecibido.nivel());
        assertEquals(10, filtroRecibido.limite());
    }

    @Test
    void rechazaServicioDesconocido() {
        ExcepcionAplicacion error = assertThrows(ExcepcionAplicacion.class,
                () -> service.consultar("otro-servicio", null, null, null, null));
        assertEquals(ErrorAplicacion.SOLICITUD_INVALIDA, error.tipo());
    }
}
