package ar.edu.uai.tfi.management.application.auth;

import ar.edu.uai.tfi.management.domain.model.Idioma;
import ar.edu.uai.tfi.management.domain.model.Tema;
import ar.edu.uai.tfi.management.domain.model.Usuario;

/** @param tema null si el usuario todavía no eligió uno */
public record AuthResult(String token, long expiresIn, Usuario usuario, Idioma idioma, Tema tema) {
}
