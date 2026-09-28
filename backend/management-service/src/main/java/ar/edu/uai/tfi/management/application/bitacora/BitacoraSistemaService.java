package ar.edu.uai.tfi.management.application.bitacora;

import ar.edu.uai.tfi.management.application.ErrorAplicacion;
import ar.edu.uai.tfi.management.application.ExcepcionAplicacion;
import ar.edu.uai.tfi.management.domain.model.FiltroBitacoraSistema;
import ar.edu.uai.tfi.management.domain.model.RegistroBitacoraSistema;
import ar.edu.uai.tfi.management.domain.model.ResultadoEvento;
import ar.edu.uai.tfi.management.domain.model.TipoEventoSistema;
import ar.edu.uai.tfi.management.domain.repository.BitacoraSistemaRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;

/**
 * Consulta de la bitácora de auditoría de sistema (solo lectura).
 * Valida los filtros recibidos y limita la cantidad de resultados.
 */
@ApplicationScoped
public class BitacoraSistemaService {

    static final int LIMITE_POR_DEFECTO = 100;
    static final int LIMITE_MAXIMO = 500;

    private final BitacoraSistemaRepository repository;

    public BitacoraSistemaService(BitacoraSistemaRepository repository) {
        this.repository = repository;
    }

    public List<RegistroBitacoraSistema> consultar(String evento, String resultado, String actor,
                                                   String desde, String hasta, Integer limite) {
        Instant fechaDesde = parsearFecha(desde, "desde");
        Instant fechaHasta = parsearFecha(hasta, "hasta");
        if (fechaDesde != null && fechaHasta != null && fechaDesde.isAfter(fechaHasta)) {
            throw invalido("La fecha 'desde' no puede ser posterior a 'hasta'.");
        }

        FiltroBitacoraSistema filtro = new FiltroBitacoraSistema(
                parsearEnum(TipoEventoSistema.class, evento, "evento"),
                parsearEnum(ResultadoEvento.class, resultado, "resultado"),
                actor == null || actor.isBlank() ? null : actor.trim(),
                fechaDesde,
                fechaHasta,
                validarLimite(limite)
        );
        return repository.buscar(filtro);
    }

    private int validarLimite(Integer limite) {
        if (limite == null) {
            return LIMITE_POR_DEFECTO;
        }
        if (limite < 1 || limite > LIMITE_MAXIMO) {
            throw invalido("El límite debe estar entre 1 y " + LIMITE_MAXIMO + ".");
        }
        return limite;
    }

    private Instant parsearFecha(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(valor.trim());
        } catch (DateTimeParseException exception) {
            throw invalido("El campo '" + campo + "' debe tener formato ISO-8601, por ejemplo 2026-09-28T00:00:00Z.");
        }
    }

    private <E extends Enum<E>> E parsearEnum(Class<E> tipo, String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(tipo, valor.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw invalido("El valor '" + valor + "' no es válido para el campo '" + campo + "'.");
        }
    }

    private ExcepcionAplicacion invalido(String mensaje) {
        return new ExcepcionAplicacion(ErrorAplicacion.SOLICITUD_INVALIDA, mensaje);
    }
}
