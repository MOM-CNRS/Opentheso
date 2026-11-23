package fr.cnrs.opentheso.ws.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RestThesaurusAccessTest {

    @Test
    void distinguishesMissingPrivateAndForeignProject() {
        assertEquals(RestThesaurusAccess.NOT_FOUND, RestThesaurusAccess.decide(false, false, false, false));
        assertEquals(RestThesaurusAccess.ALLOWED, RestThesaurusAccess.decide(true, false, true, false));
        assertEquals(RestThesaurusAccess.ALLOWED, RestThesaurusAccess.decide(true, true, true, true));
        assertEquals(RestThesaurusAccess.PRIVATE, RestThesaurusAccess.decide(true, true, false, false));
        assertEquals(RestThesaurusAccess.FOREIGN_PROJECT, RestThesaurusAccess.decide(true, true, true, false));
    }

    @Test
    void messagesNameTheReason() {
        assertEquals("{\"message\":\"Thésaurus introuvable.\"}", RestThesaurusAccess.NOT_FOUND.body());
        assertEquals("{\"message\":\"Vous n'avez pas accès à ce thésaurus : il est privé.\"}", RestThesaurusAccess.PRIVATE.body());
        assertEquals(
                "{\"message\":\"Vous n'avez pas accès à ce thésaurus : il appartient à un projet auquel vous n'avez pas droit.\"}",
                RestThesaurusAccess.FOREIGN_PROJECT.body());
    }
}
