package ar.edu.uai.tfi.management.application.port;

/** Pide despachar la bandeja de salida cuando se confirme la transacción en curso (sin demorar la respuesta). */
public interface AvisoDespachoPort {

    void solicitarDespacho();
}
