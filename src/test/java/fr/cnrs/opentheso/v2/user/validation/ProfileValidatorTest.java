package fr.cnrs.opentheso.v2.user.validation;

import fr.cnrs.opentheso.v2.user.exception.InvalidProfileDataException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProfileValidatorTest {

    @Test
    void requireUsername_trimsAndReturnsValue() {
        assertEquals("alice", ProfileValidator.requireUsername("  alice  "));
    }

    @Test
    void requireUsername_rejectsBlank() {
        assertThrows(InvalidProfileDataException.class, () -> ProfileValidator.requireUsername(" "));
    }

    @Test
    void requireEmail_rejectsNaiveValidFormats() {
        assertThrows(InvalidProfileDataException.class, () -> ProfileValidator.requireEmail("a@"));
        assertThrows(InvalidProfileDataException.class, () -> ProfileValidator.requireEmail("@b.com"));
    }

    @Test
    void requireEmail_trimsAndReturnsValue() {
        assertEquals("alice@example.com", ProfileValidator.requireEmail(" alice@example.com "));
    }

    @Test
    void requireEmail_rejectsInvalidFormat() {
        assertThrows(InvalidProfileDataException.class, () -> ProfileValidator.requireEmail("not-an-email"));
    }
}
