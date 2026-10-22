package ar.edu.uai.tfi.management.application.correo;

/** Correo listo para enviar: texto plano y HTML con el mismo contenido. */
public record MensajeCorreo(String destinatario, String nombreDestinatario, String asunto, String texto,
                            String html) {
}
