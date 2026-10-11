package ar.edu.uai.tfi.management.application.b2b;

import ar.edu.uai.tfi.management.application.ErrorAplicacion;
import ar.edu.uai.tfi.management.application.ExcepcionAplicacion;
import ar.edu.uai.tfi.management.domain.model.EstadoRegistro;
import ar.edu.uai.tfi.management.domain.model.OperacionTransaccion;
import ar.edu.uai.tfi.management.domain.model.OrganizacionCliente;
import ar.edu.uai.tfi.management.domain.model.Sede;
import ar.edu.uai.tfi.management.domain.model.Transaccion;
import ar.edu.uai.tfi.management.domain.model.Usuario;
import ar.edu.uai.tfi.management.support.EnMemoriaGestion;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Bloque 2.3b · El alta de una organización queda en la bitácora de transacciones con su valor nuevo.
 * Bloque 6 · Modificación y baja lógica de organizaciones y sedes (CU-007-001 y CU-007-002).
 */
class OrganizacionClienteServiceTest {

    private final List<Transaccion> transacciones = new ArrayList<>();
    private final EnMemoriaGestion.Organizaciones organizaciones = new EnMemoriaGestion.Organizaciones();
    private final EnMemoriaGestion.Sedes sedes = new EnMemoriaGestion.Sedes();
    private final EnMemoriaGestion.Usuarios usuarios = new EnMemoriaGestion.Usuarios();
    private final OrganizacionClienteService service = new OrganizacionClienteService(organizaciones, transacciones::add,
            usuarios);
    private final SedeService sedeService = new SedeService(sedes, organizaciones, transacciones::add);

    @Test
    void altaDeOrganizacionRegistraTransaccion() {
        OrganizacionCliente creada = service.crear("cli-01", "Cliente Uno S.A.");

        assertEquals(1, transacciones.size());
        Transaccion alta = transacciones.get(0);
        assertEquals("ORGANIZACION", alta.entidad());
        assertEquals(OperacionTransaccion.ALTA, alta.operacion());
        assertEquals(creada.id(), alta.entidadId());
        assertEquals(creada.id(), alta.organizacionId());
        assertNull(alta.valorAnterior());
        assertEquals(creada, alta.valorNuevo());
    }

    @Test
    void modificarOrganizacionGuardaValorAnteriorYNuevo() {
        OrganizacionCliente creada = service.crear("cli-01", "Cliente Uno S.A.");

        OrganizacionCliente modificada = service.actualizar(creada.id(), "cli-01b", "Cliente Uno SRL");

        assertEquals("CLI-01B", modificada.identificador());
        Transaccion cambio = transacciones.get(1);
        assertEquals(OperacionTransaccion.MODIFICACION, cambio.operacion());
        assertEquals(creada, cambio.valorAnterior());
        assertEquals(modificada, cambio.valorNuevo());
    }

    @Test
    void modificarRechazaIdentificadorDeOtraOrganizacion() {
        service.crear("cli-01", "Cliente Uno");
        OrganizacionCliente otra = service.crear("cli-02", "Cliente Dos");

        ExcepcionAplicacion error = assertThrows(ExcepcionAplicacion.class,
                () -> service.actualizar(otra.id(), "CLI-01", "Cliente Dos"));

        assertEquals(ErrorAplicacion.CONFLICTO, error.tipo());
    }

    @Test
    void deshabilitarOrganizacionEsBajaLogicaEImpideNuevasSedes() {
        OrganizacionCliente creada = service.crear("cli-01", "Cliente Uno");
        usuarios.agregar(new Usuario(10L, creada.id(), "Ana", "ana@x.com", "ana", "h", EstadoRegistro.ACTIVO,
                Set.of("ANALISTA_RED")));
        service.reloj = new EnMemoriaGestion.RelojAjustable(Instant.parse("2026-10-11T12:00:00.750Z"));

        OrganizacionCliente deshabilitada = service.cambiarEstado(creada.id(), "DESHABILITADO");
        ExcepcionAplicacion repetido = assertThrows(ExcepcionAplicacion.class,
                () -> service.cambiarEstado(creada.id(), "DESHABILITADO"));
        ExcepcionAplicacion sede = assertThrows(ExcepcionAplicacion.class,
                () -> sedeService.crear(creada.id(), "Casa central", "CABA"));

        assertEquals(EstadoRegistro.DESHABILITADO, deshabilitada.estado());
        assertEquals(OperacionTransaccion.CAMBIO_ESTADO, transacciones.get(1).operacion());
        assertEquals(ErrorAplicacion.CONFLICTO, repetido.tipo());
        assertEquals(ErrorAplicacion.CONFLICTO, sede.tipo());
        assertEquals(1, organizaciones.listar().size(), "La organización no se borra");
        assertEquals(Instant.parse("2026-10-11T12:00:00Z"), usuarios.estadoAcceso(10L).sesionesValidasDesde(),
                "Al deshabilitarla se cierran las sesiones de sus usuarios (truncado al segundo)");
    }

    @Test
    void sedeSeModificaYSeDeshabilitaConTrazabilidad() {
        OrganizacionCliente org = service.crear("cli-01", "Cliente Uno");
        Sede central = sedeService.crear(org.id(), "Casa central", "CABA");
        sedeService.crear(org.id(), "Planta", "Pilar");

        ExcepcionAplicacion duplicada = assertThrows(ExcepcionAplicacion.class,
                () -> sedeService.actualizar(central.id(), "planta", "CABA"));
        Sede modificada = sedeService.actualizar(central.id(), "Sede central", "CABA, piso 3");
        Sede deshabilitada = sedeService.cambiarEstado(central.id(), "DESHABILITADO");

        assertEquals(ErrorAplicacion.CONFLICTO, duplicada.tipo());
        assertEquals("Sede central", modificada.nombre());
        assertEquals(EstadoRegistro.DESHABILITADO, deshabilitada.estado());
        Transaccion ultima = transacciones.get(transacciones.size() - 1);
        assertEquals("SEDE", ultima.entidad());
        assertEquals(OperacionTransaccion.CAMBIO_ESTADO, ultima.operacion());
        assertEquals(org.id(), ultima.organizacionId());
    }
}
