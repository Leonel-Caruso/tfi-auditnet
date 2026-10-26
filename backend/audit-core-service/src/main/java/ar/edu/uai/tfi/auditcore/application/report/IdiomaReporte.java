package ar.edu.uai.tfi.auditcore.application.report;

import java.util.Locale;

/** Idioma en que se arma un reporte: título al generarlo y CSV al descargarlo. */
public enum IdiomaReporte {
    ES, EN, PT;

    /** Primer idioma de un encabezado Accept-Language ("pt-BR,pt;q=0.9" → PT); si no es en ni pt, español. */
    public static IdiomaReporte de(String idioma) {
        String codigo = idioma == null ? "" : idioma.trim().toLowerCase(Locale.ROOT);
        if (codigo.startsWith("en")) {
            return EN;
        }
        return codigo.startsWith("pt") ? PT : ES;
    }
}
