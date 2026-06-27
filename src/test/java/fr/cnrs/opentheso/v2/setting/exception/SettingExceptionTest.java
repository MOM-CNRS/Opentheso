package fr.cnrs.opentheso.v2.setting.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SettingExceptionTest {

    @Test
    void invalidSettingDataException_carriesMessage() {
        var exception = new InvalidSettingDataException("données invalides");
        assertEquals("données invalides", exception.getMessage());
    }

    @Test
    void settingAccessDeniedException_hasDefaultMessage() {
        var exception = new SettingAccessDeniedException();
        assertEquals("Accès refusé aux paramètres du thésaurus.", exception.getMessage());
    }
}
