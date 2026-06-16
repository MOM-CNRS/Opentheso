package fr.cnrs.opentheso.v2.user.validation;

import fr.cnrs.opentheso.v2.user.exception.InvalidPasswordException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PasswordPolicyTest {

    @Test
    void validate_rejectsBlankPassword() {
        assertThrows(InvalidPasswordException.class,
                () -> PasswordPolicy.validate(" ", "Abcd1234!"));
    }

    @Test
    void validate_acceptsStrongMatchingPasswords() {
        assertDoesNotThrow(() -> PasswordPolicy.validate("Abcd1234!", "Abcd1234!"));
    }

    @Test
    void validate_rejectsMismatch() {
        assertThrows(InvalidPasswordException.class,
                () -> PasswordPolicy.validate("Abcd1234!", "Abcd1234?"));
    }

    @Test
    void validate_rejectsWeakPassword() {
        assertThrows(InvalidPasswordException.class,
                () -> PasswordPolicy.validate("password", "password"));
    }
}
