package ar.edu.uai.tfi.management.application.port;

import ar.edu.uai.tfi.management.domain.model.NivelExcepcion;

/**
 * Puerto de la bitácora de excepciones. Registrar un error nunca debe interrumpir la respuesta
 * al usuario: si la bitácora no está disponible, el error queda igualmente en el log del servicio.
 */
public interface BitacoraExcepcionesPort {

    void registrar(Throwable error, NivelExcepcion nivel, int estadoHttp);
}
