package ar.edu.uai.tfi.auditcore.api.rest;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Paginación opcional de hallazgos e historial. */
class PaginacionTest {

    private static final List<Integer> CINCUENTA_Y_DOS = IntStream.rangeClosed(1, 52).boxed().toList();

    @Test
    void sinParametrosNoPagina() {
        assertNull(Paginacion.de(null, null));
        assertNull(Paginacion.de(" ", ""));
    }

    @Test
    void cortaLaPaginaPedidaYLaUltimaIncompleta() {
        assertEquals(IntStream.rangeClosed(26, 50).boxed().toList(), Paginacion.de("2", "25").recortar(CINCUENTA_Y_DOS));
        assertEquals(List.of(51, 52), Paginacion.de("3", "25").recortar(CINCUENTA_Y_DOS));
        assertTrue(Paginacion.de("4", "25").recortar(CINCUENTA_Y_DOS).isEmpty());
    }

    @Test
    void completaElValorQueFalta() {
        assertEquals(new Paginacion(1, 10), Paginacion.de(null, "10"));
        assertEquals(new Paginacion(3, Paginacion.POR_PAGINA_POR_DEFECTO), Paginacion.de("3", null));
    }

    @Test
    void rechazaValoresInvalidos() {
        assertThrows(IllegalArgumentException.class, () -> Paginacion.de("0", "25"));
        assertThrows(IllegalArgumentException.class, () -> Paginacion.de("1", "0"));
        assertThrows(IllegalArgumentException.class, () -> Paginacion.de("1", "101"));
        assertThrows(IllegalArgumentException.class, () -> Paginacion.de("uno", "25"));
    }
}
