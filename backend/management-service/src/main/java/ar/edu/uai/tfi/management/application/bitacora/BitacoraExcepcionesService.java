package ar.edu.uai.tfi.management.application.bitacora;

import ar.edu.uai.tfi.management.domain.model.FiltroBitacoraExcepciones;
import ar.edu.uai.tfi.management.domain.model.NivelExcepcion;
import ar.edu.uai.tfi.management.domain.model.RegistroExcepcion;
import ar.edu.uai.tfi.management.domain.repository.BitacoraExcepcionesRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Consulta de la bitácora de excepciones (solo lectura), con errores de ambos servicios. */
@ApplicationScoped
public class BitacoraExcepcionesService {

    private static final Set<String> SERVICIOS = Set.of("management-service", "audit-core-service");

    private final BitacoraExcepcionesRepository repository;

    public BitacoraExcepcionesService(BitacoraExcepcionesRepository repository) {
        this.repository = repository;
    }

    public List<RegistroExcepcion> consultar(String servicio, String nivel, String desde, String hasta,
                                             Integer limite) {
        Instant fechaDesde = FiltrosBitacora.fecha(desde, "desde");
        Instant fechaHasta = FiltrosBitacora.fecha(hasta, "hasta");
        FiltrosBitacora.rango(fechaDesde, fechaHasta);

        return repository.buscar(new FiltroBitacoraExcepciones(
                validarServicio(servicio),
                FiltrosBitacora.enumerado(NivelExcepcion.class, nivel, "nivel"),
                fechaDesde,
                fechaHasta,
                FiltrosBitacora.limite(limite)
        ));
    }

    private String validarServicio(String servicio) {
        String valor = FiltrosBitacora.texto(servicio);
        if (valor == null) {
            return null;
        }
        valor = valor.toLowerCase(Locale.ROOT);
        if (!SERVICIOS.contains(valor)) {
            throw FiltrosBitacora.invalido("El servicio debe ser management-service o audit-core-service.");
        }
        return valor;
    }
}
