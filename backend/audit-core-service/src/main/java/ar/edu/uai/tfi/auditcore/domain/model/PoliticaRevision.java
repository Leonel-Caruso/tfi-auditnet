package ar.edu.uai.tfi.auditcore.domain.model;

import java.time.Duration;
import java.time.Instant;

/**
 * Plazos de revisión de una organización: días para corregir un hallazgo según su severidad, cada cuántos días se
 * vuelve a auditar un equipo y a partir de cuántos días sin una configuración nueva se la considera desactualizada.
 *
 * @param modificadaEn null mientras la organización use los valores por defecto
 */
public record PoliticaRevision(
        Long organizacionId,
        int diasCritica,
        int diasAlta,
        int diasMedia,
        int diasBaja,
        int diasReauditoria,
        int diasConfiguracion,
        boolean reauditoriaAutomatica,
        Instant modificadaEn,
        String modificadaPor
) {

    public static final int MAXIMO_DIAS = 365;

    public static PoliticaRevision predeterminada(Long organizacionId) {
        return new PoliticaRevision(organizacionId, 7, 15, 30, 60, 30, 30, true, null, null);
    }

    public int plazo(SeveridadRegla severidad) {
        return switch (severidad) {
            case CRITICA -> diasCritica;
            case ALTA -> diasAlta;
            case MEDIA -> diasMedia;
            case BAJA -> diasBaja;
        };
    }

    /** Fecha límite para corregir el hallazgo; null si ya está cerrado. */
    public Instant vence(HallazgoAuditoria hallazgo) {
        if (!hallazgo.estado().pendiente() || hallazgo.fechaDeteccion() == null) {
            return null;
        }
        return hallazgo.fechaDeteccion().plus(Duration.ofDays(plazo(hallazgo.severidad())));
    }

    public boolean vencido(HallazgoAuditoria hallazgo, Instant ahora) {
        Instant vence = vence(hallazgo);
        return vence != null && vence.isBefore(ahora);
    }

    public void validar() {
        int[] dias = {diasCritica, diasAlta, diasMedia, diasBaja, diasReauditoria, diasConfiguracion};
        for (int valor : dias) {
            if (valor < 1 || valor > MAXIMO_DIAS) {
                throw new IllegalArgumentException("Cada plazo debe estar entre 1 y " + MAXIMO_DIAS + " días.");
            }
        }
        if (diasCritica > diasAlta || diasAlta > diasMedia || diasMedia > diasBaja) {
            throw new IllegalArgumentException(
                    "Los plazos de corrección van de menor a mayor: crítica ≤ alta ≤ media ≤ baja.");
        }
    }
}
