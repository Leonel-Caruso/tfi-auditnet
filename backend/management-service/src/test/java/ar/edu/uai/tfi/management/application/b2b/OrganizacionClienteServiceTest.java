package ar.edu.uai.tfi.management.application.b2b;

import ar.edu.uai.tfi.management.domain.model.OperacionTransaccion;
import ar.edu.uai.tfi.management.domain.model.OrganizacionCliente;
import ar.edu.uai.tfi.management.domain.model.Transaccion;
import ar.edu.uai.tfi.management.domain.repository.OrganizacionClienteRepository;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Bloque 2.3b · El alta de una organización queda en la bitácora de transacciones con su valor nuevo.
 */
class OrganizacionClienteServiceTest {

    @Test
    void altaDeOrganizacionRegistraTransaccion() {
        List<Transaccion> transacciones = new ArrayList<>();
        OrganizacionClienteService service = new OrganizacionClienteService(new EnMemoria(), transacciones::add);

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

    private static class EnMemoria implements OrganizacionClienteRepository {
        private final List<OrganizacionCliente> items = new ArrayList<>();

        @Override
        public OrganizacionCliente guardar(OrganizacionCliente o) {
            OrganizacionCliente creada = new OrganizacionCliente((long) items.size() + 1, o.identificador(),
                    o.razonSocial(), o.estado());
            items.add(creada);
            return creada;
        }

        @Override
        public List<OrganizacionCliente> listar() {
            return items;
        }

        @Override
        public Optional<OrganizacionCliente> buscarPorId(Long id) {
            return items.stream().filter(o -> o.id().equals(id)).findFirst();
        }

        @Override
        public Optional<OrganizacionCliente> buscarPorIdentificador(String identificador) {
            return items.stream().filter(o -> o.identificador().equals(identificador)).findFirst();
        }

        @Override
        public Optional<OrganizacionCliente> buscarPorRazonSocial(String razonSocial) {
            return items.stream().filter(o -> o.razonSocial().equals(razonSocial)).findFirst();
        }
    }
}
