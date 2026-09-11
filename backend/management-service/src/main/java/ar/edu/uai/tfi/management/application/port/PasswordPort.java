package ar.edu.uai.tfi.management.application.port;

public interface PasswordPort {
    String hashear(String passwordPlano);
    boolean coincide(String passwordPlano, String passwordHash);
}
