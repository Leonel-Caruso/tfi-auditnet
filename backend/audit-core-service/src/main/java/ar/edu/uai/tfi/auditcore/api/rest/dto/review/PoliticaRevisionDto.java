package ar.edu.uai.tfi.auditcore.api.rest.dto.review;

import ar.edu.uai.tfi.auditcore.domain.model.PoliticaRevision;

import java.time.Instant;

/**
 * Plazos de revisión de una organización. En la respuesta, {@code modificadaEn} es null mientras use los valores por
 * defecto; en el pedido se ignoran {@code modificadaEn} y {@code modificadaPor}.
 */
public record PoliticaRevisionDto(
        Integer diasCritica,
        Integer diasAlta,
        Integer diasMedia,
        Integer diasBaja,
        Integer diasReauditoria,
        Integer diasConfiguracion,
        Boolean reauditoriaAutomatica,
        Instant modificadaEn,
        String modificadaPor
) {

    public static PoliticaRevisionDto de(PoliticaRevision politica) {
        return politica == null ? null : new PoliticaRevisionDto(politica.diasCritica(), politica.diasAlta(),
                politica.diasMedia(), politica.diasBaja(), politica.diasReauditoria(), politica.diasConfiguracion(),
                politica.reauditoriaAutomatica(), politica.modificadaEn(), politica.modificadaPor());
    }

    public PoliticaRevision aPolitica(Long organizacionId) {
        if (diasCritica == null || diasAlta == null || diasMedia == null || diasBaja == null || diasReauditoria == null
                || diasConfiguracion == null || reauditoriaAutomatica == null) {
            throw new IllegalArgumentException("Completá todos los plazos de revisión.");
        }
        return new PoliticaRevision(organizacionId, diasCritica, diasAlta, diasMedia, diasBaja, diasReauditoria,
                diasConfiguracion, reauditoriaAutomatica, null, null);
    }
}
