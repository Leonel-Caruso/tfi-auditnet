package ar.edu.uai.tfi.management.infrastructure.security;

import ar.edu.uai.tfi.management.application.port.JwtPort;
import ar.edu.uai.tfi.management.domain.model.Usuario;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class JwtTokenAdapter implements JwtPort {
    private static final long DURACION_SEGUNDOS = 3600L;

    @ConfigProperty(name = "app.jwt.secret")
    String secret;

    @Override
    public String generar(Usuario usuario) {
        validarSecreto();

        return Jwt.issuer("tfi-auditoria")
                .subject(usuario.id().toString())
                .upn(usuario.nombreUsuario())
                .groups(usuario.roles())
                .claim("organizationId", usuario.organizacionId())
                .claim("name", usuario.nombre())
                .signWithSecret(secret);
    }

    @Override
    public long duracionSegundos() {
        return DURACION_SEGUNDOS;
    }

    private void validarSecreto() {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("JWT_SECRET debe tener al menos 32 caracteres.");
        }
    }
}
