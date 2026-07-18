package fr.cnrs.opentheso.v2.publicapi.system.api;

import fr.cnrs.opentheso.v2.publicapi.system.api.dto.ApiKeyValidationResponse;
import fr.cnrs.opentheso.v2.publicapi.system.service.ApiKeyValidationPublicService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApiKeyValidationPublicControllerTest {

    @Mock
    private ApiKeyValidationPublicService apiKeyValidationPublicService;

    private ApiKeyValidationPublicController controller;

    @BeforeEach
    void setUp() {
        controller = new ApiKeyValidationPublicController(apiKeyValidationPublicService);
    }

    @Test
    void validate_returnsServiceResult() {
        var expected = new ApiKeyValidationResponse(true, 7, "admin", true);
        when(apiKeyValidationPublicService.validate("x-key", null)).thenReturn(expected);

        var response = controller.validate("x-key", null);

        assertEquals(expected, response);
    }
}
