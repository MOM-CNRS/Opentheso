package fr.cnrs.opentheso.v2.publicapi.system.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PingPublicControllerTest {

    @Test
    void ping_returnsPong() {
        var controller = new PingPublicController();

        assertEquals("pong", controller.ping());
    }
}
