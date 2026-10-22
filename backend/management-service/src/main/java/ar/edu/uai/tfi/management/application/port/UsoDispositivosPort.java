package ar.edu.uai.tfi.management.application.port;

/**
 * Dispositivos activos de una organización. Los administra audit-core-service; este servicio solo los cuenta para
 * controlar el plan (comparten la base de datos).
 */
public interface UsoDispositivosPort {

    long dispositivosActivos(Long organizacionId);
}
