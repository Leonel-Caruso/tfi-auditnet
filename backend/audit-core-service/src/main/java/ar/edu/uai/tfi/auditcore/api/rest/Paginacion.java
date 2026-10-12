package ar.edu.uai.tfi.auditcore.api.rest;

import jakarta.ws.rs.core.Response;

import java.util.List;

/**
 * Paginación opcional de listados: sin {@code pagina} ni {@code porPagina} se devuelve la lista completa. El total
 * que cumple los filtros viaja siempre en {@code X-Total-Count}, así la pantalla arma el paginador sin otra consulta.
 *
 * @param pagina    número de página desde 1
 * @param porPagina cantidad de elementos por página (1 a {@value #MAXIMO_POR_PAGINA})
 */
record Paginacion(int pagina, int porPagina) {

    static final String TOTAL = "X-Total-Count";
    static final int POR_PAGINA_POR_DEFECTO = 25;
    static final int MAXIMO_POR_PAGINA = 100;

    /** null si no se pidió paginar; si llega solo uno de los dos valores, el otro toma su valor por defecto. */
    static Paginacion de(String pagina, String porPagina) {
        boolean sinPagina = pagina == null || pagina.isBlank();
        boolean sinTamanio = porPagina == null || porPagina.isBlank();
        if (sinPagina && sinTamanio) {
            return null;
        }
        int numero = sinPagina ? 1 : entero(pagina, "pagina");
        int tamanio = sinTamanio ? POR_PAGINA_POR_DEFECTO : entero(porPagina, "porPagina");
        if (numero < 1) {
            throw new IllegalArgumentException("El parámetro 'pagina' empieza en 1.");
        }
        if (tamanio < 1 || tamanio > MAXIMO_POR_PAGINA) {
            throw new IllegalArgumentException("El parámetro 'porPagina' debe estar entre 1 y " + MAXIMO_POR_PAGINA + ".");
        }
        return new Paginacion(numero, tamanio);
    }

    /** La porción de la lista que corresponde a la página; vacía si la página está después del final. */
    <T> List<T> recortar(List<T> todos) {
        long desde = (long) (pagina - 1) * porPagina;
        if (desde >= todos.size()) {
            return List.of();
        }
        int hasta = (int) Math.min(todos.size(), desde + porPagina);
        return List.copyOf(todos.subList((int) desde, hasta));
    }

    /** 200 con la página pedida (o todo, si {@code paginacion} es null) y el total en X-Total-Count. */
    static <T> Response responder(List<T> todos, Paginacion paginacion) {
        List<T> cuerpo = paginacion == null ? todos : paginacion.recortar(todos);
        return Response.ok(cuerpo).header(TOTAL, todos.size()).build();
    }

    private static int entero(String valor, String campo) {
        try {
            return Integer.parseInt(valor.trim());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("El parámetro '" + campo + "' debe ser un número entero.");
        }
    }
}
