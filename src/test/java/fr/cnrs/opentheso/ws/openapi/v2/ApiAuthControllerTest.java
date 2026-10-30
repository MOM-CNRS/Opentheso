package fr.cnrs.opentheso.ws.openapi.v2;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApiAuthControllerTest {

    @Test
    void redirectPath_keepsTokenOnTheConceptPath() {
        assertEquals(
                "/idt=th3&idc=42470&ssoToken=abc",
                ApiAuthController.redirectPath("abc", "th3", "42470"));
    }

    @Test
    void redirectPath_withoutConcept_usesQueryToken() {
        assertEquals("/?ssoToken=abc", ApiAuthController.redirectPath("abc", "", ""));
    }
}
