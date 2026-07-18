package fr.cnrs.opentheso.v2.publicapi.resolver.api;

import fr.cnrs.opentheso.v2.publicapi.resolver.api.dto.GroupArkLookupResponse;
import fr.cnrs.opentheso.v2.publicapi.resolver.service.GroupArkPublicService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GroupArkPublicControllerTest {

    @Mock
    private GroupArkPublicService groupArkPublicService;

    private GroupArkPublicController controller;

    @BeforeEach
    void setUp() {
        controller = new GroupArkPublicController(groupArkPublicService);
    }

    @Test
    void resolveGroupByArk_returnsServiceResult() {
        var expected = new GroupArkLookupResponse("TH1", "G1");
        when(groupArkPublicService.resolveGroupByArk("naan", "ark1")).thenReturn(expected);

        var response = controller.resolveGroupByArk("naan", "ark1");

        assertEquals(expected, response);
    }
}
