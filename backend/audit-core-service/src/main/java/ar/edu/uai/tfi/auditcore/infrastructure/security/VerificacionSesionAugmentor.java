package ar.edu.uai.tfi.auditcore.infrastructure.security;

import ar.edu.uai.tfi.auditcore.application.admin.ReglaSesion;
import ar.edu.uai.tfi.auditcore.application.admin.ReglaSesion.DatosCuenta;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.SecurityIdentityAugmentor;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Optional;

/**
 * Verifica en cada solicitud autenticada que el usuario y su organización sigan activos y que el token no sea
 * anterior a un cambio de contraseña, roles u organización; si no, responde 401. Usa JDBC directo porque corre
 * durante la autenticación, antes de que exista el contexto de Hibernate.
 */
@ApplicationScoped
public class VerificacionSesionAugmentor implements SecurityIdentityAugmentor {

    private static final Logger LOG = Logger.getLogger(VerificacionSesionAugmentor.class);
    static final String ADMIN = "ADMINISTRADOR_SISTEMA";
    static final String CONSULTA = """
            SELECT u.estado, o.estado, u.sesiones_validas_desde
            FROM usuarios u
            JOIN organizaciones_cliente o ON o.id_organizacion = u.id_organizacion
            WHERE u.id_usuario = ?
            """;

    private final Instance<DataSource> dataSource;

    public VerificacionSesionAugmentor(Instance<DataSource> dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Uni<SecurityIdentity> augment(SecurityIdentity identity, AuthenticationRequestContext context) {
        if (identity.isAnonymous() || !(identity.getPrincipal() instanceof JsonWebToken jwt)) {
            return Uni.createFrom().item(identity);
        }
        return context.runBlocking(() -> verificar(identity, jwt));
    }

    private SecurityIdentity verificar(SecurityIdentity identity, JsonWebToken jwt) {
        long usuarioId;
        try {
            usuarioId = Long.parseLong(jwt.getSubject());
        } catch (NumberFormatException exception) {
            throw new AuthenticationFailedException("El token no identifica a un usuario.");
        }
        Optional<String> motivo = ReglaSesion.evaluar(consultar(usuarioId), identity.hasRole(ADMIN),
                jwt.getIssuedAtTime());
        if (motivo.isPresent()) {
            LOG.infof("Sesión rechazada para el usuario %d: %s", usuarioId, motivo.get());
            throw new AuthenticationFailedException(motivo.get());
        }
        return identity;
    }

    private DatosCuenta consultar(long usuarioId) {
        try (Connection conexion = dataSource.get().getConnection();
             PreparedStatement consulta = conexion.prepareStatement(CONSULTA)) {
            consulta.setLong(1, usuarioId);
            try (ResultSet fila = consulta.executeQuery()) {
                if (!fila.next()) {
                    return null;
                }
                Timestamp desde = fila.getTimestamp(3);
                return new DatosCuenta(fila.getString(1), fila.getString(2), desde == null ? null : desde.toInstant());
            }
        } catch (SQLException exception) {
            // Sin base no se puede confirmar la sesión: se rechaza (falla segura).
            LOG.errorf("No se pudo verificar la sesión del usuario %d (%s)", usuarioId, exception.getSQLState());
            throw new AuthenticationFailedException("No se pudo verificar la sesión.");
        }
    }
}
