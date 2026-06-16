package fr.cnrs.opentheso.v2.user.validation;

import fr.cnrs.opentheso.v2.user.exception.InvalidProfileDataException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.EmailValidator;

public final class ProfileValidator {

    private static final EmailValidator EMAIL_VALIDATOR = EmailValidator.getInstance();

    private ProfileValidator() {
    }

    public static String requireUsername(String username) {
        String value = StringUtils.trimToNull(username);
        if (value == null) {
            throw new InvalidProfileDataException("Le pseudo est obligatoire.");
        }
        return value;
    }

    public static String requireEmail(String email) {
        String value = StringUtils.trimToNull(email);
        if (value == null) {
            throw new InvalidProfileDataException("Un email est obligatoire.");
        }
        if (!EMAIL_VALIDATOR.isValid(value)) {
            throw new InvalidProfileDataException("L'adresse email n'est pas valide.");
        }
        return value;
    }
}
