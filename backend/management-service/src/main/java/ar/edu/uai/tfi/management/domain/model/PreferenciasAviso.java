package ar.edu.uai.tfi.management.domain.model;

/** Avisos por correo que el usuario quiere recibir. Los de su cuenta (alta, recuperación) llegan siempre. */
public record PreferenciasAviso(boolean alertas, boolean vencimientos, boolean resumen) {

    public static PreferenciasAviso todas() {
        return new PreferenciasAviso(true, true, true);
    }
}
