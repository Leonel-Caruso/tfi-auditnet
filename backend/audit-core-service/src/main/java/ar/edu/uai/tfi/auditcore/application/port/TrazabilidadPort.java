package ar.edu.uai.tfi.auditcore.application.port;

public interface TrazabilidadPort {

    void registrar(String actor, String accion, String detalle);
}
