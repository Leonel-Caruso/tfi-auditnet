package ar.edu.uai.tfi.management.api.rest;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
class SystemStatusResourceTest {
    @Test
    void shouldReturnManagementStatus() {
        given()
                .when().get("/api/management/status")
                .then()
                .statusCode(200)
                .body("service", is("management-service"))
                .body("status", is("UP"))
                .body("delivery", is("Entrega 1"));
    }
}
