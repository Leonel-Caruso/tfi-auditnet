package ar.edu.uai.tfi.auditcore.api.rest;

import ar.edu.uai.tfi.auditcore.application.review.RevisionPeriodicaService;
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
 * Tarea que dispara el trabajo programado de Azure. No pasa por el gateway: se autoriza con un secreto compartido en
 * el encabezado {@value #ENCABEZADO}. Sin secreto configurado, la ruta no existe.
 */
@Path("/internal")
@Produces(MediaType.APPLICATION_JSON)
@PermitAll
public class TareaInternaResource {

    static final String ENCABEZADO = "X-AuditNet-Tarea";

    private final RevisionPeriodicaService revision;

    @ConfigProperty(name = "auditnet.tareas.secreto")
    Optional<String> secreto;

    public TareaInternaResource(RevisionPeriodicaService revision) {
        this.revision = revision;
    }

    @POST
    @Path("/review/run")
    public RevisionPeriodicaService.Resultado ejecutarRevision(@HeaderParam(ENCABEZADO) String recibido) {
        verificar(recibido);
        return revision.ejecutar(RevisionPeriodicaService.ACTOR);
    }

    private void verificar(String recibido) {
        String esperado = secreto.map(String::trim).filter(valor -> !valor.isEmpty()).orElseThrow(NotFoundException::new);
        if (recibido == null || !MessageDigest.isEqual(esperado.getBytes(StandardCharsets.UTF_8),
                recibido.trim().getBytes(StandardCharsets.UTF_8))) {
            throw new ForbiddenException("Secreto de tarea inválido.");
        }
    }
}
