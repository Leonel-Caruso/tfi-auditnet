package ar.edu.uai.tfi.management.api.rest;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * Herramienta de diagnóstico del panel de administración: genera un error controlado para
 * verificar, de punta a punta, que la bitácora de excepciones registra correctamente.
 * Solo administradores.
 */
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
