package ar.edu.uai.tfi.management.application.auth;

import ar.edu.uai.tfi.management.infrastructure.security.BcryptPasswordAdapter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BcryptPasswordAdapterTest {
    @Test
    void shouldHashAndVerifyPassword() {
        BcryptPasswordAdapter adapter = new BcryptPasswordAdapter();
        String plain = "clave-local-de-prueba";
        String hash = adapter.hashear(plain);

        assertNotEquals(plain, hash);
        assertTrue(adapter.coincide(plain, hash));
        assertFalse(adapter.coincide("otra-clave", hash));
    }
}
