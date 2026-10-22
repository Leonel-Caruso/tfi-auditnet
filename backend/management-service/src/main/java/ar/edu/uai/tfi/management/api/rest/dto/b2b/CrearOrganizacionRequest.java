package ar.edu.uai.tfi.management.api.rest.dto.b2b;

/** Alta de organización con su plan inicial; límites y días como en la asignación de plan. */
public record CrearOrganizacionRequest(String identificador, String razonSocial, String plan,
                                       Integer limiteDispositivos, Integer limiteUsuarios, Integer diasPrueba) {
}
