package com.starlight;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
class DriverTest {
    @Test
    void testHelloEndpoint() {
        given()
          .when().get("/status")
          .then()
             .statusCode(200)
             .body(is("Hello from Quarkus REST"));
    }

}