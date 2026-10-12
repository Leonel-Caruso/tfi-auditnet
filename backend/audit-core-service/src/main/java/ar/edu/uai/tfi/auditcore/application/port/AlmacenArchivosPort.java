package ar.edu.uai.tfi.auditcore.application.port;

import java.util.Map;

/**
 * Almacenamiento de archivos. En Azure es un contenedor privado de Blob Storage; en local, el emulador Azurite.
 * Los archivos no se sobrescriben ni se borran.
 */
public interface AlmacenArchivosPort {

    /** false si el servicio no tiene configurado el almacenamiento (por ejemplo, en las pruebas). */
    boolean configurado();

    /** Nombre del contenedor (para informarlo en el panel). */
    String contenedor();

    /** Sube el archivo. Falla si ya existe un archivo con esa ruta (los archivos no se reemplazan). */
    void subir(String ruta, byte[] contenido, String tipoContenido, Map<String, String> metadatos);

    byte[] descargar(String ruta);

    /** Comprueba que el contenedor responda (salud del servicio). */
    void verificar();

    /** Error al hablar con el almacenamiento (sin credenciales en el mensaje). */
    class AlmacenamientoException extends RuntimeException {
        private final int estadoHttp;

        public AlmacenamientoException(String mensaje, int estadoHttp, Throwable causa) {
            super(mensaje, causa);
            this.estadoHttp = estadoHttp;
        }

        public int estadoHttp() {
            return estadoHttp;
        }
    }
}
