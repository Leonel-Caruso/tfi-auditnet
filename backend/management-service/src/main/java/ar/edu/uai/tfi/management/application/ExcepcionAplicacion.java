package ar.edu.uai.tfi.management.application;

public class ExcepcionAplicacion extends RuntimeException {

    private final ErrorAplicacion tipo;

    public ExcepcionAplicacion(ErrorAplicacion tipo, String mensaje) {
        super(mensaje);
        this.tipo = tipo;
    }

    public ErrorAplicacion tipo() {
        return tipo;
    }
}
