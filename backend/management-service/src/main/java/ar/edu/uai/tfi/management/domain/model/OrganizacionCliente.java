package ar.edu.uai.tfi.management.domain.model;

public record OrganizacionCliente(
        Long id,
        String identificador,
        String razonSocial,
        EstadoRegistro estado
) {
}
