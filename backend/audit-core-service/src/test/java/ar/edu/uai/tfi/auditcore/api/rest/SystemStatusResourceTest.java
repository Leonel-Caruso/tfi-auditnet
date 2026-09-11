package ar.edu.uai.tfi.auditcore.api.rest;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
class SystemStatusResourceTest {

    @Test
    void shouldReturnAuditCoreStatus() {
        given()
                .when().get("/api/audit-core/status")
                .then()
                .statusCode(200)
                .body("service", is("audit-core-service"))
                .body("status", is("UP"))
                .body("delivery", is("Entrega 1"));
    }
}
