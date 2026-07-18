package fr.cnrs.opentheso.v2.publicapi.thesaurus.api;

import fr.cnrs.opentheso.v2.publicapi.thesaurus.api.dto.PublicThesaurusSummaryResponse;
import fr.cnrs.opentheso.v2.publicapi.thesaurus.service.ThesaurusPublicReadService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ThesaurusListPublicControllerTest {

    @Mock
    private ThesaurusPublicReadService thesaurusPublicReadService;

    private ThesaurusListPublicController controller;

    @BeforeEach
    void setUp() {
        controller = new ThesaurusListPublicController(thesaurusPublicReadService);
    }

    @Test
    void listPublicThesauri_returnsServiceResult() {
        var expected = List.of(new PublicThesaurusSummaryResponse("TH1", "siamois", List.of()));
        when(thesaurusPublicReadService.listPublicThesauri()).thenReturn(expected);

        var response = controller.listPublicThesauri();

        assertEquals(expected, response);
    }
}
