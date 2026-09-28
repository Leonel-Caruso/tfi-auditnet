package ar.edu.uai.tfi.management.application.bitacora;

import ar.edu.uai.tfi.management.domain.model.FiltroBitacoraTransacciones;
import ar.edu.uai.tfi.management.domain.model.OperacionTransaccion;
import ar.edu.uai.tfi.management.domain.model.RegistroTransaccion;
import ar.edu.uai.tfi.management.domain.repository.BitacoraTransaccionesRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Consulta de la bitácora de transacciones (solo lectura). Incluye las operaciones registradas
 * por management-service y por audit-core-service.
 */
@ApplicationScoped
public class BitacoraTransaccionesService {

    private static final Pattern ENTIDAD_VALIDA = Pattern.compile("^[A-Z_]{1,40}$");

    private final BitacoraTransaccionesRepository repository;

    public BitacoraTransaccionesService(BitacoraTransaccionesRepository repository) {
        this.repository = repository;
    }

    public List<RegistroTransaccion> consultar(String entidad, Long entidadId, String operacion, String actor,
                                               Long organizacionId, String desde, String hasta, Integer limite) {
        Instant fechaDesde = FiltrosBitacora.fecha(desde, "desde");
        Instant fechaHasta = FiltrosBitacora.fecha(hasta, "hasta");
        FiltrosBitacora.rango(fechaDesde, fechaHasta);

        return repository.buscar(new FiltroBitacoraTransacciones(
                validarEntidad(entidad),
                entidadId,
                FiltrosBitacora.enumerado(OperacionTransaccion.class, operacion, "operacion"),
                FiltrosBitacora.texto(actor),
                organizacionId,
                fechaDesde,
                fechaHasta,
                FiltrosBitacora.limite(limite)
        ));
    }

    private String validarEntidad(String entidad) {
        String valor = FiltrosBitacora.texto(entidad);
        if (valor == null) {
            return null;
        }
        valor = valor.toUpperCase(Locale.ROOT);
        if (!ENTIDAD_VALIDA.matcher(valor).matches()) {
            throw FiltrosBitacora.invalido("El campo 'entidad' solo admite letras y guiones bajos, por ejemplo DISPOSITIVO.");
        }
        return valor;
    }
}
