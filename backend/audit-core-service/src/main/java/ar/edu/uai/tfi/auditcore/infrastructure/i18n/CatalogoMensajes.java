package ar.edu.uai.tfi.auditcore.infrastructure.i18n;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Traducciones de los mensajes que ve el usuario ({@value #RECURSO}). Como en gettext, la clave es el texto en
 * español tal como lo arma el código; las partes variables se escriben {0}, {1}... y se copian en la traducción.
 * Un mensaje sin traducción se devuelve en español.
 */
public final class CatalogoMensajes {

    static final String RECURSO = "i18n/mensajes.txt";
    static final Set<String> IDIOMAS = Set.of("es", "en", "pt");
    private static final Pattern VARIABLE = Pattern.compile("\\{(\\d+)}");

    /** Mensaje con partes variables: la expresión que lo reconoce y sus traducciones. */
    private record Plantilla(Pattern patron, int largoFijo, Map<String, String> traducciones) {
    }

    private final Map<String, Map<String, String>> fijos = new HashMap<>();
    private final List<Plantilla> plantillas = new ArrayList<>();

    private CatalogoMensajes() {
    }

    public static CatalogoMensajes desdeRecursos() {
        try (InputStream entrada = CatalogoMensajes.class.getClassLoader().getResourceAsStream(RECURSO)) {
            if (entrada == null) {
                throw new IllegalStateException("Falta el recurso " + RECURSO + ".");
            }
            return leer(entrada);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    static CatalogoMensajes leer(InputStream entrada) throws IOException {
        CatalogoMensajes catalogo = new CatalogoMensajes();
        Map<String, String> bloque = new LinkedHashMap<>();
        BufferedReader lector = new BufferedReader(new InputStreamReader(entrada, StandardCharsets.UTF_8));
        String linea;
        while ((linea = lector.readLine()) != null) {
            if (linea.isBlank()) {
                catalogo.agregar(bloque);
                bloque = new LinkedHashMap<>();
            } else if (!linea.startsWith("#")) {
                int separador = linea.indexOf(':');
                if (separador != 2 || !IDIOMAS.contains(linea.substring(0, 2))) {
                    throw new IllegalStateException("Línea inválida en el catálogo de mensajes: " + linea);
                }
                bloque.put(linea.substring(0, 2), linea.substring(3).trim());
            }
        }
        catalogo.agregar(bloque);
        catalogo.plantillas.sort(Comparator.comparingInt(Plantilla::largoFijo).reversed());
        return catalogo;
    }

    private void agregar(Map<String, String> bloque) {
        if (bloque.isEmpty()) {
            return;
        }
        if (!bloque.keySet().equals(IDIOMAS)) {
            throw new IllegalStateException("El mensaje no tiene los tres idiomas: " + bloque);
        }
        String espanol = bloque.get("es");
        for (String idioma : List.of("en", "pt")) {
            if (!variables(espanol).equals(variables(bloque.get(idioma)))) {
                throw new IllegalStateException("Las partes variables no coinciden en " + idioma + ": " + espanol);
            }
        }
        if (variables(espanol).isEmpty()) {
            fijos.put(espanol, bloque);
            return;
        }
        StringBuilder expresion = new StringBuilder("^");
        Matcher variable = VARIABLE.matcher(espanol);
        int desde = 0;
        int largoFijo = 0;
        while (variable.find()) {
            String fijo = espanol.substring(desde, variable.start());
            expresion.append(Pattern.quote(fijo)).append("(?<v").append(variable.group(1)).append(">.+?)");
            largoFijo += fijo.length();
            desde = variable.end();
        }
        expresion.append(Pattern.quote(espanol.substring(desde))).append('$');
        largoFijo += espanol.length() - desde;
        plantillas.add(new Plantilla(Pattern.compile(expresion.toString(), Pattern.DOTALL), largoFijo, bloque));
    }

    private static Set<String> variables(String texto) {
        Set<String> encontradas = new TreeSet<>();
        Matcher variable = VARIABLE.matcher(texto);
        while (variable.find()) {
            encontradas.add(variable.group(1));
        }
        return encontradas;
    }

    /** @param idioma es, en o pt; cualquier otro valor devuelve el mensaje sin cambios */
    public String traducir(String mensaje, String idioma) {
        if (mensaje == null || idioma == null || "es".equals(idioma) || !IDIOMAS.contains(idioma)) {
            return mensaje;
        }
        Map<String, String> fijo = fijos.get(mensaje);
        if (fijo != null) {
            return fijo.get(idioma);
        }
        // La plantilla con más texto fijo es la más específica.
        for (Plantilla plantilla : plantillas) {
            Matcher coincidencia = plantilla.patron().matcher(mensaje);
            if (coincidencia.matches()) {
                return VARIABLE.matcher(plantilla.traducciones().get(idioma))
                        .replaceAll(variable -> Matcher.quoteReplacement(coincidencia.group("v" + variable.group(1))));
            }
        }
        return mensaje;
    }

    /** Toma el primer idioma de un encabezado Accept-Language ("en-US,en;q=0.9" → en); si no lo reconoce, es. */
    public static String idiomaPedido(String acceptLanguage) {
        if (acceptLanguage == null || acceptLanguage.isBlank()) {
            return "es";
        }
        String primero = acceptLanguage.split(",")[0].split(";")[0].trim().toLowerCase(Locale.ROOT);
        String base = primero.length() >= 2 ? primero.substring(0, 2) : primero;
        return IDIOMAS.contains(base) ? base : "es";
    }
}
