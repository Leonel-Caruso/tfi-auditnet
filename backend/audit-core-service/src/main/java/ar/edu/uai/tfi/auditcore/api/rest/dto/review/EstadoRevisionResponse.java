package ar.edu.uai.tfi.auditcore.api.rest.dto.review;

import ar.edu.uai.tfi.auditcore.application.review.RevisionPeriodicaService;

/** Indicadores del tablero. {@code politica} es null cuando abarca todas las organizaciones. */
public record EstadoRevisionResponse(PoliticaRevisionDto politica, long hallazgosVencidos,
                                     long dispositivosDesactualizados) {

    public static EstadoRevisionResponse de(RevisionPeriodicaService.Estado estado) {
        return new EstadoRevisionResponse(PoliticaRevisionDto.de(estado.politica()), estado.hallazgosVencidos(),
                estado.dispositivosDesactualizados());
    }
}
