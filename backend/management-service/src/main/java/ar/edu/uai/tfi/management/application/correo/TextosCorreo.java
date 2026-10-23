package ar.edu.uai.tfi.management.application.correo;

import ar.edu.uai.tfi.management.domain.model.Idioma;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Textos de los correos de un idioma ({@code correo/textos_<idioma>.properties}), con datos entre llaves. */
final class TextosCorreo {

    private static final Map<Idioma, TextosCorreo> CARGADOS = new EnumMap<>(Idioma.class);
    private static final Pattern DATO = Pattern.compile("\\{(\\w+)}");

    private final Properties textos;
    private final Locale locale;

    private TextosCorreo(Properties textos) {
        this.textos = textos;
        this.locale = Locale.forLanguageTag(textos.getProperty("locale", "es-AR"));
    }

    static synchronized TextosCorreo de(Idioma idioma) {
        return CARGADOS.computeIfAbsent(idioma == null ? Idioma.ES : idioma, TextosCorreo::cargar);
    }

    private static TextosCorreo cargar(Idioma idioma) {
        String recurso = "correo/textos_" + idioma.codigo() + ".properties";
        try (InputStream entrada = TextosCorreo.class.getClassLoader().getResourceAsStream(recurso)) {
            if (entrada == null) {
                throw new IllegalStateException("Falta el recurso " + recurso + ".");
            }
            Properties textos = new Properties();
            textos.load(new InputStreamReader(entrada, StandardCharsets.UTF_8));
            return new TextosCorreo(textos);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    Locale locale() {
        return locale;
    }

    Set<String> claves() {
        return textos.stringPropertyNames();
    }

    /** @param datos pares nombre, valor: {@code texto("alta.usuario", "usuario", "ana")} */
    String texto(String clave, Object... datos) {
        String texto = textos.getProperty(clave);
        if (texto == null) {
            throw new IllegalStateException("Falta el texto de correo " + clave + ".");
        }
        Map<String, String> valores = new HashMap<>();
        for (int i = 0; i + 1 < datos.length; i += 2) {
            valores.put(String.valueOf(datos[i]), String.valueOf(datos[i + 1]));
        }
        // Una sola pasada: un dato que contenga llaves no se vuelve a reemplazar.
        return DATO.matcher(texto).replaceAll(dato -> Matcher.quoteReplacement(
                valores.getOrDefault(dato.group(1), dato.group())));
    }

    /** Nombre de un código (perfil, severidad...) o el código tal cual si no tiene traducción. */
    String etiqueta(String prefijo, String codigo) {
        return textos.getProperty(prefijo + "." + codigo, codigo);
    }
}
