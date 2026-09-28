package ar.edu.uai.tfi.management.application.bitacora;

import ar.edu.uai.tfi.management.application.ErrorAplicacion;
import ar.edu.uai.tfi.management.application.ExcepcionAplicacion;
import ar.edu.uai.tfi.management.domain.model.FiltroBitacoraTransacciones;
import ar.edu.uai.tfi.management.domain.model.OperacionTransaccion;
import ar.edu.uai.tfi.management.domain.model.RegistroTransaccion;
import ar.edu.uai.tfi.management.domain.repository.BitacoraTransaccionesRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Bloque 2.3b · Consulta de la bitácora de transacciones: validación de filtros.
 */
class BitacoraTransaccionesServiceTest {

    private FiltroBitacoraTransacciones filtroRecibido;

    private final BitacoraTransaccionesService service = new BitacoraTransaccionesService(
            new BitacoraTransaccionesRepository() {
                @Override
                public RegistroTransaccion guardar(RegistroTransaccion registro) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public List<RegistroTransaccion> buscar(FiltroBitacoraTransacciones filtro) {
                    filtroRecibido = filtro;
                    return List.of();
                }
            });

    @Test
    void convierteLosFiltrosDeHistorialDeUnaEntidad() {
        service.consultar("dispositivo", 4L, "alta", null, 1L, null, null, 20);

        assertEquals("DISPOSITIVO", filtroRecibido.entidad());
        assertEquals(4L, filtroRecibido.entidadId());
        assertEquals(OperacionTransaccion.ALTA, filtroRecibido.operacion());
        assertEquals(1L, filtroRecibido.organizacionId());
        assertEquals(20, filtroRecibido.limite());
    }

    @Test
    void sinFiltrosUsaElLimitePorDefecto() {
        service.consultar(null, null, null, null, null, null, null, null);

        assertNull(filtroRecibido.entidad());
        assertEquals(FiltrosBitacora.LIMITE_POR_DEFECTO, filtroRecibido.limite());
    }

    @Test
    void rechazaEntidadConCaracteresInvalidos() {
        ExcepcionAplicacion error = assertThrows(ExcepcionAplicacion.class,
                () -> service.consultar("DISPOSITIVO'; DROP", null, null, null, null, null, null, null));
        assertEquals(ErrorAplicacion.SOLICITUD_INVALIDA, error.tipo());
    }

    @Test
    void rechazaOperacionInexistente() {
        ExcepcionAplicacion error = assertThrows(ExcepcionAplicacion.class,
                () -> service.consultar(null, null, "BORRAR", null, null, null, null, null));
        assertEquals(ErrorAplicacion.SOLICITUD_INVALIDA, error.tipo());
    }
}
