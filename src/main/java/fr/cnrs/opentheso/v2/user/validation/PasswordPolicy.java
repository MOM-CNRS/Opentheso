package fr.cnrs.opentheso.v2.user.validation;

import fr.cnrs.opentheso.v2.user.exception.InvalidPasswordException;
import org.apache.commons.lang3.StringUtils;

public final class PasswordPolicy {

    private static final String STRENGTH_REGEX = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{8,}$";

    private PasswordPolicy() {
    }

    public static void validate(String password, String confirmation) {
        if (StringUtils.isBlank(password) || StringUtils.isBlank(confirmation)) {
            throw new InvalidPasswordException("Un mot de passe est obligatoire.");
        }
        if (!password.equals(confirmation)) {
            throw new InvalidPasswordException("Mot de passe non identique.");
        }
        if (!password.matches(STRENGTH_REGEX)) {
            throw new InvalidPasswordException(
                    "Le mot de passe doit contenir au moins 8 caractères, une majuscule, "
                            + "une minuscule, un chiffre et un caractère spécial."
            );
        }
    }
}
