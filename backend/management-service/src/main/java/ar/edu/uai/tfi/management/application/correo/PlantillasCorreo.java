package ar.edu.uai.tfi.management.application.correo;

import ar.edu.uai.tfi.management.domain.model.CorreoSaliente;
import ar.edu.uai.tfi.management.domain.model.Idioma;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Arma el asunto y el cuerpo (texto y HTML) de cada aviso en el idioma del destinatario, con la identidad de AuditNet:
 * barra gris oscura con el "A_" naranja, botón naranja y pie con el motivo del aviso. Los textos están en
 * {@code correo/textos_<idioma>.properties}; todos los datos se escapan antes de ir al HTML.
 */
public class PlantillasCorreo {

    private static final String NARANJA = "#f57c00";
    private static final String GRIS_OSCURO = "#171a1f";

    /** Contenido de un aviso antes de darle formato. */
    private record Contenido(String asunto, String titulo, List<String> parrafos, List<String> items, String boton,
                             String enlace, String pie) {
    }

    /**
     * @param organizacion razón social de la organización del aviso (null si no tiene)
     * @param urlPublica   dirección del sitio, para los enlaces
     */
    public MensajeCorreo armar(CorreoSaliente correo, String organizacion, String urlPublica, Idioma idioma) {
        TextosCorreo t = TextosCorreo.de(idioma);
        Map<String, Object> datos = correo.datos() == null ? Map.of() : correo.datos();
        String sitio = urlPublica.endsWith("/") ? urlPublica.substring(0, urlPublica.length() - 1) : urlPublica;
        String nombreOrganizacion = organizacion == null ? t.texto("organizacion.generica") : organizacion;
        Contenido contenido = switch (correo.tipo()) {
            case RECUPERACION_CONTRASENA -> recuperacion(t, correo, datos, sitio);
            case ALTA_USUARIO -> alta(t, datos, organizacion, sitio);
            case SOLICITUD_PLAN -> solicitudPlan(t, datos, nombreOrganizacion, sitio);
            case SOLICITUD_PLAN_RESUELTA -> solicitudResuelta(t, datos, sitio);
            case ALERTAS_CRITICAS -> alertas(t, datos, nombreOrganizacion, sitio);
            case VENCIMIENTOS -> vencimientos(t, datos, nombreOrganizacion, sitio);
            case RESUMEN_MENSUAL -> resumen(t, datos, nombreOrganizacion, sitio);
        };
        return new MensajeCorreo(correo.destinatario(), correo.nombreDestinatario(), contenido.asunto(),
                texto(contenido), html(contenido, idioma, t.texto("marca")));
    }

    private static Contenido recuperacion(TextosCorreo t, CorreoSaliente correo, Map<String, Object> datos,
                                          String sitio) {
        String saludo = correo.nombreDestinatario() == null
                ? t.texto("saludo.anonimo")
                : t.texto("saludo", "nombre", correo.nombreDestinatario());
        return new Contenido(t.texto("recuperacion.asunto"), t.texto("recuperacion.titulo"),
                List.of(saludo,
                        t.texto("recuperacion.pedido", "usuario", texto(datos, "usuario"),
                                "minutos", numero(datos, "minutos")),
                        t.texto("recuperacion.ignorar")),
                List.of(), t.texto("recuperacion.boton"),
                sitio + "/reset-password?token=" + URLEncoder.encode(texto(datos, CorreoSaliente.DATO_SENSIBLE),
                        StandardCharsets.UTF_8),
                t.texto("pie.cuenta"));
    }

    private static Contenido alta(TextosCorreo t, Map<String, Object> datos, String organizacion, String sitio) {
        String usuario = organizacion == null
                ? t.texto("alta.usuario", "usuario", texto(datos, "usuario"))
                : t.texto("alta.usuario.organizacion", "usuario", texto(datos, "usuario"), "organizacion", organizacion);
        List<String> perfiles = lista(datos, "perfiles").stream()
                .map(perfil -> t.texto("alta.perfil", "perfil", t.etiqueta("perfil", String.valueOf(perfil))))
                .toList();
        return new Contenido(t.texto("alta.asunto"), t.texto("alta.titulo"),
                List.of(usuario, t.texto("alta.contrasena")), perfiles,
                t.texto("alta.boton"), sitio + "/login", t.texto("pie.cuenta"));
    }

    private static Contenido solicitudPlan(TextosCorreo t, Map<String, Object> datos, String organizacion,
                                           String sitio) {
        List<String> items = new ArrayList<>(List.of(
                t.texto("solicitud.actual", "plan", texto(datos, "planActual")),
                t.texto("solicitud.solicitado", "plan", texto(datos, "planSolicitado"),
                        "modalidad", t.etiqueta("modalidad", texto(datos, "modalidad"))),
                t.texto("solicitud.por", "usuario", texto(datos, "solicitadoPor"))));
        if (!texto(datos, "comentario").isBlank()) {
            items.add(t.texto("solicitud.comentario", "comentario", texto(datos, "comentario")));
        }
        return new Contenido(t.texto("solicitud.asunto", "organizacion", organizacion), t.texto("solicitud.titulo"),
                List.of(t.texto("solicitud.pedido", "organizacion", organizacion)),
                items, t.texto("solicitud.boton"), sitio + "/plans", t.texto("pie.cuenta"));
    }

    private static Contenido solicitudResuelta(TextosCorreo t, Map<String, Object> datos, String sitio) {
        String clave = "APROBADA".equals(texto(datos, "estado")) ? "resuelta.aprobada" : "resuelta.rechazada";
        String plan = texto(datos, "planSolicitado");
        List<String> parrafos = new ArrayList<>();
        parrafos.add(t.texto(clave + ".texto", "plan", plan));
        if (!texto(datos, "respuesta").isBlank()) {
            parrafos.add(t.texto("resuelta.respuesta", "respuesta", texto(datos, "respuesta")));
        }
        return new Contenido(t.texto(clave + ".asunto", "plan", plan), t.texto(clave + ".titulo"), parrafos,
                List.of(), t.texto("resuelta.boton"), sitio + "/plan", t.texto("pie.cuenta"));
    }

    private static Contenido alertas(TextosCorreo t, Map<String, Object> datos, String organizacion, String sitio) {
        long alertas = numero(datos, "alertas");
        long criticas = numero(datos, "criticas");
        String dispositivo = texto(datos, "dispositivo");
        String detalle = alertas == 1
                ? t.texto("alertas.detalle.una")
                : t.texto("alertas.detalle.varias", "cantidad", alertas);
        if (criticas > 0) {
            detalle = t.texto("alertas.detalle.criticas", "detalle", detalle, "criticas", criticas);
        }
        List<String> items = lista(datos, "reglas").stream()
                .map(PlantillasCorreo::comoMapa)
                .map(regla -> t.texto("alertas.regla", "codigo", texto(regla, "codigo"), "nombre",
                        texto(regla, "nombre"), "severidad", t.etiqueta("severidad", texto(regla, "severidad"))))
                .toList();
        String asunto = alertas == 1
                ? t.texto("alertas.asunto.una", "dispositivo", dispositivo)
                : t.texto("alertas.asunto.varias", "cantidad", alertas, "dispositivo", dispositivo);
        return new Contenido(asunto, t.texto("alertas.titulo", "dispositivo", dispositivo),
                List.of(t.texto("alertas.resumen", "dispositivo", dispositivo, "nombre",
                        texto(datos, "dispositivoNombre"), "detalle", detalle)),
                items, t.texto("alertas.boton"), sitio + "/alerts",
                t.texto("pie.preferencia", "organizacion", organizacion));
    }

    private static Contenido vencimientos(TextosCorreo t, Map<String, Object> datos, String organizacion,
                                          String sitio) {
        List<String> items = new ArrayList<>();
        lista(datos, "vencidos").stream().map(PlantillasCorreo::comoMapa).forEach(hallazgo -> items.add(
                t.texto("vencimientos.vencido", "dias", numero(hallazgo, "dias"), "codigo", texto(hallazgo, "codigo"),
                        "nombre", texto(hallazgo, "nombre"), "dispositivo", texto(hallazgo, "dispositivo"),
                        "severidad", t.etiqueta("severidad", texto(hallazgo, "severidad")))));
        lista(datos, "desactualizados").stream().map(PlantillasCorreo::comoMapa).forEach(dispositivo -> items.add(
                t.texto("vencimientos.desactualizado", "dias", numero(dispositivo, "dias"),
                        "identificador", texto(dispositivo, "identificador"), "nombre", texto(dispositivo, "nombre"))));
        long vencidos = numero(datos, "totalVencidos");
        long desactualizados = numero(datos, "totalDesactualizados");
        List<String> parrafos = new ArrayList<>();
        if (vencidos > 0) {
            parrafos.add(vencidos == 1
                    ? t.texto("vencimientos.vencidos.uno")
                    : t.texto("vencimientos.vencidos.varios", "cantidad", vencidos));
        }
        if (desactualizados > 0) {
            parrafos.add(desactualizados == 1
                    ? t.texto("vencimientos.desactualizados.uno")
                    : t.texto("vencimientos.desactualizados.varios", "cantidad", desactualizados));
        }
        if (items.size() < vencidos + desactualizados) {
            parrafos.add(t.texto("vencimientos.parcial"));
        }
        return new Contenido(t.texto("vencimientos.asunto", "organizacion", organizacion),
                t.texto("vencimientos.titulo"), parrafos, items, t.texto("vencimientos.boton"), sitio + "/results",
                t.texto("pie.preferencia", "organizacion", organizacion));
    }

    private static Contenido resumen(TextosCorreo t, Map<String, Object> datos, String organizacion, String sitio) {
        String periodo = periodo(t, texto(datos, "periodo"));
        return new Contenido(t.texto("resumen.asunto", "periodo", periodo, "organizacion", organizacion),
                t.texto("resumen.titulo", "periodo", periodo),
                List.of(t.texto("resumen.introduccion", "periodo", periodo, "organizacion", organizacion)),
                List.of(t.texto("resumen.auditorias", "cantidad", numero(datos, "auditorias")),
                        t.texto("resumen.detectados", "cantidad", numero(datos, "detectados")),
                        t.texto("resumen.cerrados", "cantidad", numero(datos, "cerrados")),
                        t.texto("resumen.pendientes", "cantidad", numero(datos, "pendientes"),
                                "vencidos", numero(datos, "vencidos")),
                        t.texto("resumen.alertas", "cantidad", numero(datos, "alertasVigentes")),
                        t.texto("resumen.riesgo", "nivel", t.etiqueta("riesgo", texto(datos, "nivelRiesgo")))),
                t.texto("resumen.boton"), sitio + "/dashboard", t.texto("pie.preferencia", "organizacion", organizacion));
    }

    /** "2026-09" pasa a "septiembre de 2026" / "September 2026"; si no tiene ese formato, se muestra tal cual. */
    static String periodo(TextosCorreo t, String valor) {
        try {
            return DateTimeFormatter.ofPattern(t.texto("periodo.formato"), t.locale()).format(YearMonth.parse(valor));
        } catch (DateTimeParseException | IllegalArgumentException exception) {
            return valor;
        }
    }

    private static String texto(Contenido contenido) {
        StringBuilder texto = new StringBuilder(contenido.titulo()).append("\n\n");
        contenido.parrafos().forEach(parrafo -> texto.append(parrafo).append("\n\n"));
        contenido.items().forEach(item -> texto.append("- ").append(item).append('\n'));
        if (!contenido.items().isEmpty()) {
            texto.append('\n');
        }
        texto.append(contenido.boton()).append(": ").append(contenido.enlace()).append("\n\n-- \n")
                .append(contenido.pie()).append('\n');
        return texto.toString();
    }

    private static String html(Contenido contenido, Idioma idioma, String marca) {
        StringBuilder cuerpo = new StringBuilder();
        contenido.parrafos().forEach(parrafo -> cuerpo.append(
                "<p style=\"margin:0 0 12px;font-size:14px;line-height:1.55;\">").append(escapar(parrafo))
                .append("</p>"));
        if (!contenido.items().isEmpty()) {
            cuerpo.append("<ul style=\"margin:4px 0 14px;padding-left:18px;font-size:13px;line-height:1.6;\">");
            contenido.items().forEach(item -> cuerpo.append("<li>").append(escapar(item)).append("</li>"));
            cuerpo.append("</ul>");
        }
        return """
                <!doctype html>
                <html lang="%s"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>%s</title></head>
                <body style="margin:0;padding:0;background:#eceff1;font-family:Inter,'Segoe UI',Arial,sans-serif;color:#20242a;">
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background:#eceff1;padding:24px 12px;"><tr><td align="center">
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="max-width:560px;background:#ffffff;border:1px solid #d7dbdf;border-radius:10px;">
                <tr><td style="background:%s;padding:16px 24px;border-radius:10px 10px 0 0;">
                <span style="display:inline-block;width:34px;height:34px;line-height:34px;text-align:center;background:%s;color:#ffffff;font-family:'JetBrains Mono',Consolas,monospace;font-weight:800;border-radius:7px;">A_</span>
                <span style="color:#ffffff;font-size:16px;font-weight:700;margin-left:10px;vertical-align:middle;">AuditNet</span>
                </td></tr>
                <tr><td style="padding:26px 24px 6px;">
                <h1 style="margin:0 0 14px;font-size:20px;line-height:1.3;color:#111418;">%s</h1>
                %s
                <p style="margin:20px 0 22px;"><a href="%s" style="display:inline-block;background:%s;color:#ffffff;text-decoration:none;font-weight:700;font-size:14px;padding:11px 18px;border-radius:7px;">%s</a></p>
                </td></tr>
                <tr><td style="padding:14px 24px 20px;border-top:1px solid #eceff1;font-size:12px;line-height:1.5;color:#697078;">%s<br>%s</td></tr>
                </table>
                </td></tr></table>
                </body></html>
                """.formatted(idioma.codigo(), escapar(contenido.asunto()), GRIS_OSCURO, NARANJA,
                escapar(contenido.titulo()), cuerpo, escapar(contenido.enlace()), NARANJA, escapar(contenido.boton()),
                escapar(contenido.pie()), escapar(marca));
    }

    static String escapar(String valor) {
        return valor == null ? "" : valor
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private static String texto(Map<String, Object> datos, String clave) {
        Object valor = datos.get(clave);
        return valor == null ? "" : String.valueOf(valor);
    }

    private static long numero(Map<String, Object> datos, String clave) {
        return datos.get(clave) instanceof Number numero ? numero.longValue() : 0;
    }

    private static List<?> lista(Map<String, Object> datos, String clave) {
        return datos.get(clave) instanceof List<?> lista ? lista : List.of();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> comoMapa(Object valor) {
        return valor instanceof Map<?, ?> mapa ? (Map<String, Object>) mapa : Map.of();
    }
}
