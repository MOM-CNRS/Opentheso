package fr.cnrs.opentheso.v2.toolbox.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InvalidToolboxDataExceptionTest {

    @Test
    void carriesMessage() {
        var exception = new InvalidToolboxDataException("données invalides");

        assertEquals("données invalides", exception.getMessage());
    }
}
