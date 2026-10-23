package ar.edu.uai.tfi.management.application.auth;

import ar.edu.uai.tfi.management.domain.model.Idioma;
import ar.edu.uai.tfi.management.domain.model.Usuario;

public record AuthResult(String token, long expiresIn, Usuario usuario, Idioma idioma) {
}
