package ar.edu.uai.tfi.management.application.correo;

import ar.edu.uai.tfi.management.domain.model.CorreoSaliente;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Arma el asunto y el cuerpo (texto y HTML) de cada aviso con la identidad de AuditNet: barra gris oscura con el
 * "A_" naranja, botón naranja y pie con el motivo del aviso. Todos los datos se escapan antes de ir al HTML.
 */
public class PlantillasCorreo {

    private static final String NARANJA = "#f57c00";
    private static final String GRIS_OSCURO = "#171a1f";
    private static final String PIE_CUENTA = "Correo automático de AuditNet: no hace falta responderlo.";
    private static final Map<String, String> PERFILES = Map.of(
            "ADMINISTRADOR_SISTEMA", "Administrador del sistema",
            "ADMINISTRADOR_ORGANIZACION", "Administrador de organización",
            "ANALISTA_RED", "Analista de redes",
            "AUDITOR_TECNICO", "Auditor técnico",
            "RESPONSABLE_GESTION_IT", "Responsable de gestión IT");
    private static final Map<String, String> SEVERIDADES = Map.of(
            "CRITICA", "crítica", "ALTA", "alta", "MEDIA", "media", "BAJA", "baja");

    /** Contenido de un aviso antes de darle formato. */
    private record Contenido(String asunto, String titulo, List<String> parrafos, List<String> items, String boton,
                             String enlace, String pie) {
    }

    /**
     * @param organizacion razón social de la organización del aviso (null si no tiene)
     * @param urlPublica   dirección del sitio, para los enlaces
     */
    public MensajeCorreo armar(CorreoSaliente correo, String organizacion, String urlPublica) {
        Map<String, Object> datos = correo.datos() == null ? Map.of() : correo.datos();
        String sitio = urlPublica.endsWith("/") ? urlPublica.substring(0, urlPublica.length() - 1) : urlPublica;
        Contenido contenido = switch (correo.tipo()) {
            case RECUPERACION_CONTRASENA -> recuperacion(correo, datos, sitio);
            case ALTA_USUARIO -> alta(datos, organizacion, sitio);
            case SOLICITUD_PLAN -> solicitudPlan(datos, organizacion, sitio);
            case SOLICITUD_PLAN_RESUELTA -> solicitudResuelta(datos, sitio);
            case ALERTAS_CRITICAS -> alertas(datos, organizacion, sitio);
            case VENCIMIENTOS -> vencimientos(datos, organizacion, sitio);
            case RESUMEN_MENSUAL -> resumen(datos, organizacion, sitio);
        };
        return new MensajeCorreo(correo.destinatario(), correo.nombreDestinatario(), contenido.asunto(),
                texto(contenido), html(contenido));
    }

    private static Contenido recuperacion(CorreoSaliente correo, Map<String, Object> datos, String sitio) {
        String saludo = correo.nombreDestinatario() == null ? "Hola:" : "Hola, " + correo.nombreDestinatario() + ":";
        return new Contenido("Restablecer tu contraseña de AuditNet", "Restablecer tu contraseña",
                List.of(saludo,
                        "Recibimos un pedido para restablecer la contraseña de tu usuario " + texto(datos, "usuario")
                                + ". El enlace vale " + numero(datos, "minutos") + " minutos y se usa una sola vez.",
                        "Si no lo pediste vos, ignorá este correo: tu contraseña no cambia."),
                List.of(), "Elegir una contraseña nueva",
                sitio + "/reset-password?token=" + URLEncoder.encode(texto(datos, CorreoSaliente.DATO_SENSIBLE),
                        StandardCharsets.UTF_8),
                PIE_CUENTA);
    }

    private static Contenido alta(Map<String, Object> datos, String organizacion, String sitio) {
        List<String> perfiles = lista(datos, "perfiles").stream()
                .map(perfil -> PERFILES.getOrDefault(String.valueOf(perfil), String.valueOf(perfil)))
                .toList();
        return new Contenido("Tu acceso a AuditNet", "Ya tenés acceso a AuditNet",
                List.of("Se creó tu usuario " + texto(datos, "usuario")
                                + (organizacion == null ? "" : " en " + organizacion) + ".",
                        "La contraseña inicial te la comunica quien administra tus accesos. Al ingresar podés "
                                + "cambiarla desde Mi cuenta."),
                perfiles.stream().map(perfil -> "Perfil: " + perfil).toList(),
                "Ingresar a AuditNet", sitio + "/login", PIE_CUENTA);
    }

    private static Contenido solicitudPlan(Map<String, Object> datos, String organizacion, String sitio) {
        List<String> items = new ArrayList<>(List.of(
                "Plan actual: " + texto(datos, "planActual"),
                "Plan pedido: " + texto(datos, "planSolicitado") + " (" + texto(datos, "modalidad").toLowerCase() + ")",
                "Pedido por: " + texto(datos, "solicitadoPor")));
        if (!texto(datos, "comentario").isBlank()) {
            items.add("Comentario: " + texto(datos, "comentario"));
        }
        return new Contenido("Solicitud de cambio de plan de " + nombre(organizacion),
                "Nueva solicitud de cambio de plan",
                List.of(nombre(organizacion) + " pidió cambiar de plan."),
                items, "Resolver la solicitud", sitio + "/plans", PIE_CUENTA);
    }

    private static Contenido solicitudResuelta(Map<String, Object> datos, String sitio) {
        boolean aprobada = "APROBADA".equals(texto(datos, "estado"));
        String plan = texto(datos, "planSolicitado");
        List<String> parrafos = new ArrayList<>();
        parrafos.add(aprobada
                ? "La organización ya tiene el plan " + plan + ": los límites nuevos rigen desde ahora."
                : "La solicitud de cambio al plan " + plan + " fue rechazada.");
        if (!texto(datos, "respuesta").isBlank()) {
            parrafos.add("Respuesta: " + texto(datos, "respuesta"));
        }
        return new Contenido(aprobada ? "Plan " + plan + " aprobado" : "Solicitud de plan rechazada",
                aprobada ? "Solicitud aprobada" : "Solicitud rechazada", parrafos, List.of(),
                "Ver Mi plan", sitio + "/plan", PIE_CUENTA);
    }

    private static Contenido alertas(Map<String, Object> datos, String organizacion, String sitio) {
        long alertas = numero(datos, "alertas");
        long criticas = numero(datos, "criticas");
        String dispositivo = texto(datos, "dispositivo");
        List<String> items = lista(datos, "reglas").stream()
                .map(PlantillasCorreo::comoMapa)
                .map(regla -> texto(regla, "codigo") + " · " + texto(regla, "nombre") + " (severidad "
                        + SEVERIDADES.getOrDefault(texto(regla, "severidad"), texto(regla, "severidad")) + ")")
                .toList();
        return new Contenido((alertas == 1 ? "1 alerta nueva" : alertas + " alertas nuevas") + " en " + dispositivo,
                "Alertas nuevas en " + dispositivo,
                List.of("La auditoría de " + dispositivo + " (" + texto(datos, "dispositivoNombre") + ") generó "
                        + alertas + (alertas == 1 ? " alerta" : " alertas")
                        + (criticas > 0 ? ", " + criticas + " de severidad crítica" : "") + "."),
                items, "Ver alertas", sitio + "/alerts", piePreferencia(organizacion));
    }

    private static Contenido vencimientos(Map<String, Object> datos, String organizacion, String sitio) {
        List<String> items = new ArrayList<>();
        lista(datos, "vencidos").stream().map(PlantillasCorreo::comoMapa).forEach(hallazgo -> items.add(
                "Vencido hace " + numero(hallazgo, "dias") + " días: " + texto(hallazgo, "codigo") + " · "
                        + texto(hallazgo, "nombre") + " en " + texto(hallazgo, "dispositivo") + " (severidad "
                        + SEVERIDADES.getOrDefault(texto(hallazgo, "severidad"), texto(hallazgo, "severidad")) + ")"));
        lista(datos, "desactualizados").stream().map(PlantillasCorreo::comoMapa).forEach(dispositivo -> items.add(
                "Sin configuración nueva hace " + numero(dispositivo, "dias") + " días: "
                        + texto(dispositivo, "identificador") + " · " + texto(dispositivo, "nombre")));
        long vencidos = numero(datos, "totalVencidos");
        long desactualizados = numero(datos, "totalDesactualizados");
        List<String> parrafos = new ArrayList<>();
        if (vencidos > 0) {
            parrafos.add(vencidos + (vencidos == 1 ? " hallazgo superó" : " hallazgos superaron")
                    + " el plazo de corrección de su severidad.");
        }
        if (desactualizados > 0) {
            parrafos.add(desactualizados + (desactualizados == 1 ? " dispositivo no tiene" : " dispositivos no tienen")
                    + " una configuración nueva cargada hace tiempo: lo auditado puede no reflejar el estado real.");
        }
        if (items.size() < vencidos + desactualizados) {
            parrafos.add("Se muestran los primeros; el detalle completo está en la consola.");
        }
        return new Contenido("Revisión periódica de " + nombre(organizacion), "Revisión periódica", parrafos, items,
                "Ver hallazgos", sitio + "/results", piePreferencia(organizacion));
    }

    private static Contenido resumen(Map<String, Object> datos, String organizacion, String sitio) {
        String periodo = texto(datos, "periodo");
        return new Contenido("Resumen de " + periodo + " · " + nombre(organizacion), "Resumen de " + periodo,
                List.of("Así terminó " + periodo + " en " + nombre(organizacion) + "."),
                List.of("Auditorías ejecutadas: " + numero(datos, "auditorias"),
                        "Hallazgos detectados: " + numero(datos, "detectados"),
                        "Hallazgos cerrados: " + numero(datos, "cerrados"),
                        "Hallazgos pendientes al cierre: " + numero(datos, "pendientes")
                                + " (vencidos: " + numero(datos, "vencidos") + ")",
                        "Alertas vigentes: " + numero(datos, "alertasVigentes"),
                        "Nivel de riesgo actual: " + texto(datos, "nivelRiesgo").toLowerCase()),
                "Abrir el tablero", sitio + "/dashboard", piePreferencia(organizacion));
    }

    private static String piePreferencia(String organizacion) {
        return "Recibís este aviso por tu perfil en " + nombre(organizacion)
                + ". Podés elegir qué avisos recibir en Mi cuenta.";
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

    private static String html(Contenido contenido) {
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
                <html lang="es"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>%s</title></head>
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
                <tr><td style="padding:14px 24px 20px;border-top:1px solid #eceff1;font-size:12px;line-height:1.5;color:#697078;">%s<br>AuditNet · auditoría de configuraciones de red</td></tr>
                </table>
                </td></tr></table>
                </body></html>
                """.formatted(escapar(contenido.asunto()), GRIS_OSCURO, NARANJA, escapar(contenido.titulo()), cuerpo,
                escapar(contenido.enlace()), NARANJA, escapar(contenido.boton()), escapar(contenido.pie()));
    }

    static String escapar(String valor) {
        return valor == null ? "" : valor
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private static String nombre(String organizacion) {
        return organizacion == null ? "tu organización" : organizacion;
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
