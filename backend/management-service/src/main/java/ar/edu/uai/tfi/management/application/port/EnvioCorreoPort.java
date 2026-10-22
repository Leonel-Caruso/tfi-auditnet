package ar.edu.uai.tfi.management.application.port;

import ar.edu.uai.tfi.management.application.correo.FalloEnvioCorreo;
import ar.edu.uai.tfi.management.application.correo.MensajeCorreo;

/** Proveedor de correo: Azure Communication Services en la nube, Mailpit en local. */
public interface EnvioCorreoPort {

    /** Nombre del proveedor configurado, o null si el envío está apagado (los avisos quedan pendientes). */
    String proveedor();

    /**
     * @return identificador que asignó el proveedor
     * @throws FalloEnvioCorreo si el proveedor no lo aceptó
     */
    String enviar(MensajeCorreo mensaje);
}
