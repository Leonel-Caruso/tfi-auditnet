package ar.edu.uai.tfi.auditcore.infrastructure.web;

import jakarta.enterprise.context.RequestScoped;

/**
 * Datos de la solicitud HTTP en curso que interesan a las bitácoras:
 * origen, navegador, método, ruta e identificador de correlación.
 * Lo completa {@link ContextoSolicitudFilter} al comienzo de cada solicitud.
 */
@RequestScoped
public class ContextoSolicitud {

    private String origenIp;
    private String userAgent;
    private String correlacion;
    private String metodoHttp;
    private String ruta;

    public void completar(String origenIp, String userAgent, String correlacion, String metodoHttp, String ruta) {
        this.origenIp = origenIp;
        this.userAgent = userAgent;
        this.correlacion = correlacion;
        this.metodoHttp = metodoHttp;
        this.ruta = ruta;
    }

    public String origenIp() {
        return origenIp;
    }

    public String userAgent() {
        return userAgent;
    }

    public String correlacion() {
        return correlacion;
    }

    public String metodoHttp() {
        return metodoHttp;
    }

    public String ruta() {
        return ruta;
    }
}
