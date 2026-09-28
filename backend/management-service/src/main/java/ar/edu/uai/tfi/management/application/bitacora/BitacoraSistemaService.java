package ar.edu.uai.tfi.management.application.bitacora;

import ar.edu.uai.tfi.management.domain.model.FiltroBitacoraSistema;
import ar.edu.uai.tfi.management.domain.model.RegistroBitacoraSistema;
import ar.edu.uai.tfi.management.domain.model.ResultadoEvento;
import ar.edu.uai.tfi.management.domain.model.TipoEventoSistema;
import ar.edu.uai.tfi.management.domain.repository.BitacoraSistemaRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Instant;
import java.util.List;

/**
 * Consulta de la bitácora de auditoría de sistema (solo lectura).
 * Valida los filtros recibidos y limita la cantidad de resultados.
 */
@ApplicationScoped
public class BitacoraSistemaService {

    static final int LIMITE_POR_DEFECTO = FiltrosBitacora.LIMITE_POR_DEFECTO;
    static final int LIMITE_MAXIMO = FiltrosBitacora.LIMITE_MAXIMO;

    private final BitacoraSistemaRepository repository;

    public BitacoraSistemaService(BitacoraSistemaRepository repository) {
        this.repository = repository;
    }

    public List<RegistroBitacoraSistema> consultar(String evento, String resultado, String actor,
                                                   String desde, String hasta, Integer limite) {
        Instant fechaDesde = FiltrosBitacora.fecha(desde, "desde");
        Instant fechaHasta = FiltrosBitacora.fecha(hasta, "hasta");
        FiltrosBitacora.rango(fechaDesde, fechaHasta);

        return repository.buscar(new FiltroBitacoraSistema(
                FiltrosBitacora.enumerado(TipoEventoSistema.class, evento, "evento"),
                FiltrosBitacora.enumerado(ResultadoEvento.class, resultado, "resultado"),
                FiltrosBitacora.texto(actor),
                fechaDesde,
                fechaHasta,
                FiltrosBitacora.limite(limite)
        ));
    }
}
