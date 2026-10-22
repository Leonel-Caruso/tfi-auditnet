package ar.edu.uai.tfi.management.api.rest;

import ar.edu.uai.tfi.management.application.correo.CorreoService;
import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Optional;

/**
 * Tareas que disparan el trabajo programado de Azure y audit-core-service. No pasan por el gateway: se autorizan con
 * un secreto compartido en el encabezado {@value #ENCABEZADO}. Sin secreto configurado, la ruta no existe.
 */
@Path("/internal")
@Produces(MediaType.APPLICATION_JSON)
@PermitAll
public class TareaInternaResource {

    static final String ENCABEZADO = "X-AuditNet-Tarea";

    private final CorreoService correos;

    @ConfigProperty(name = "auditnet.tareas.secreto")
    Optional<String> secreto;

    public TareaInternaResource(CorreoService correos) {
        this.correos = correos;
    }

    @POST
    @Path("/mail/dispatch")
    public CorreoService.ResultadoDespacho despacharCorreo(@HeaderParam(ENCABEZADO) String recibido) {
        verificar(recibido);
        return correos.despachar();
    }

    private void verificar(String recibido) {
        String esperado = secreto.map(String::trim).filter(valor -> !valor.isEmpty()).orElseThrow(NotFoundException::new);
        if (recibido == null || !MessageDigest.isEqual(esperado.getBytes(StandardCharsets.UTF_8),
                recibido.trim().getBytes(StandardCharsets.UTF_8))) {
            throw new ForbiddenException("Secreto de tarea inválido.");
        }
    }
}
