package ar.edu.uai.tfi.management.application.port;

import ar.edu.uai.tfi.management.domain.model.Usuario;

public interface JwtPort {
    String generar(Usuario usuario);
    long duracionSegundos();
}
