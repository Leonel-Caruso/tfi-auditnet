package ar.edu.uai.tfi.management.application.correo;

import java.time.Duration;

/**
 * El proveedor no aceptó el correo.
 *
 * @param reintentable si conviene volver a intentar (caída, límite de envíos); false si el pedido es inválido
 * @param esperar      cuánto esperar antes del próximo intento; null para usar la espera por defecto
 */
public class FalloEnvioCorreo extends RuntimeException {

    private final boolean reintentable;
    private final Duration esperar;

    public FalloEnvioCorreo(String mensaje, boolean reintentable, Duration esperar) {
        super(mensaje);
        this.reintentable = reintentable;
        this.esperar = esperar;
    }

    public boolean reintentable() {
        return reintentable;
    }

    public Duration esperar() {
        return esperar;
    }
}
