package ar.edu.uai.tfi.management.api.rest;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/** Genera un error controlado para comprobar de punta a punta que la bitácora de excepciones registra. */
@Path("/api/admin/diagnostico")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("ADMINISTRADOR_SISTEMA")
public class DiagnosticoResource {

    @POST
    @Path("/excepcion")
    public void generarExcepcionDePrueba() {
        throw new IllegalStateException("Excepción de prueba generada desde el diagnóstico de administración.");
    }
}
