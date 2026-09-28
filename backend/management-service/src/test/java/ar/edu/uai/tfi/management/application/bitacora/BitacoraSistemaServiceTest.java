package ar.edu.uai.tfi.management.application.bitacora;

import ar.edu.uai.tfi.management.application.ErrorAplicacion;
import ar.edu.uai.tfi.management.application.ExcepcionAplicacion;
import ar.edu.uai.tfi.management.domain.model.FiltroBitacoraSistema;
import ar.edu.uai.tfi.management.domain.model.RegistroBitacoraSistema;
import ar.edu.uai.tfi.management.domain.model.ResultadoEvento;
import ar.edu.uai.tfi.management.domain.model.TipoEventoSistema;
import ar.edu.uai.tfi.management.domain.repository.BitacoraSistemaRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Bloque 2.3a · Consulta de la bitácora de sistema: validación de filtros y límite.
 */
class BitacoraSistemaServiceTest {

    private FiltroBitacoraSistema filtroRecibido;

    private final BitacoraSistemaService service = new BitacoraSistemaService(new BitacoraSistemaRepository() {
        @Override
        public RegistroBitacoraSistema guardar(RegistroBitacoraSistema registro) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<RegistroBitacoraSistema> buscar(FiltroBitacoraSistema filtro) {
            filtroRecibido = filtro;
            return List.of();
        }
    });

    @Test
    void sinFiltrosUsaElLimitePorDefecto() {
        service.consultar(null, null, null, null, null, null);

        assertNull(filtroRecibido.tipo());
        assertNull(filtroRecibido.resultado());
        assertEquals(BitacoraSistemaService.LIMITE_POR_DEFECTO, filtroRecibido.limite());
    }

    @Test
    void convierteLosFiltrosRecibidos() {
        service.consultar("login_rechazado", "fallo", "  ana ", "2026-09-01T00:00:00Z", "2026-09-30T23:59:59Z", 50);

        assertEquals(TipoEventoSistema.LOGIN_RECHAZADO, filtroRecibido.tipo());
        assertEquals(ResultadoEvento.FALLO, filtroRecibido.resultado());
        assertEquals("ana", filtroRecibido.actor());
        assertEquals(Instant.parse("2026-09-01T00:00:00Z"), filtroRecibido.desde());
        assertEquals(50, filtroRecibido.limite());
    }

    @Test
    void rechazaLimiteFueraDeRango() {
        assertInvalido(() -> service.consultar(null, null, null, null, null, 0));
        assertInvalido(() -> service.consultar(null, null, null, null, null, BitacoraSistemaService.LIMITE_MAXIMO + 1));
    }

    @Test
    void rechazaEventoInexistenteYFechaMalFormada() {
        assertInvalido(() -> service.consultar("BORRADO_TOTAL", null, null, null, null, null));
        assertInvalido(() -> service.consultar(null, null, null, "28/09/2026", null, null));
    }

    @Test
    void rechazaRangoDeFechasInvertido() {
        assertInvalido(() -> service.consultar(null, null, null, "2026-09-30T00:00:00Z", "2026-09-01T00:00:00Z", null));
    }

    private void assertInvalido(org.junit.jupiter.api.function.Executable accion) {
        ExcepcionAplicacion error = assertThrows(ExcepcionAplicacion.class, accion);
        assertEquals(ErrorAplicacion.SOLICITUD_INVALIDA, error.tipo());
    }
}
