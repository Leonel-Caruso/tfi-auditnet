package ar.edu.uai.tfi.management.domain.model;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/** Idiomas de la consola y de los correos. El portugués es el de Brasil. */
public enum Idioma {
    ES("es"),
    EN("en"),
    PT("pt");

    private final String codigo;

    Idioma(String codigo) {
        this.codigo = codigo;
    }

    public String codigo() {
        return codigo;
    }

    /** Acepta el código solo o con región ("pt-BR", "en_US"). */
    public static Optional<Idioma> desde(String valor) {
        if (valor == null || valor.isBlank()) {
            return Optional.empty();
        }
        String base = valor.trim().toLowerCase(Locale.ROOT).split("[-_]")[0];
        return Arrays.stream(values()).filter(idioma -> idioma.codigo.equals(base)).findFirst();
    }
}
