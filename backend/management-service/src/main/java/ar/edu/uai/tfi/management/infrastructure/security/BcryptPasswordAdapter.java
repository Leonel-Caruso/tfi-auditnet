package ar.edu.uai.tfi.management.infrastructure.security;

import ar.edu.uai.tfi.management.application.port.PasswordPort;
import io.quarkus.elytron.security.common.BcryptUtil;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class BcryptPasswordAdapter implements PasswordPort {
    @Override
    public String hashear(String passwordPlano) {
        return BcryptUtil.bcryptHash(passwordPlano);
    }

    @Override
    public boolean coincide(String passwordPlano, String passwordHash) {
        return BcryptUtil.matches(passwordPlano, passwordHash);
    }
}
