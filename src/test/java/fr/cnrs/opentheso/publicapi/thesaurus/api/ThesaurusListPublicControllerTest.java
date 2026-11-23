package fr.cnrs.opentheso.publicapi.thesaurus.api;

import fr.cnrs.opentheso.publicapi.thesaurus.api.dto.PublicThesaurusSummaryResponse;
import fr.cnrs.opentheso.publicapi.thesaurus.service.ThesaurusPublicReadService;
import fr.cnrs.opentheso.ws.api.RestThesaurusVisibility;
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
    @Mock
    private RestThesaurusVisibility restThesaurusVisibility;

    private ThesaurusListPublicController controller;

    @BeforeEach
    void setUp() {
        controller = new ThesaurusListPublicController(thesaurusPublicReadService, restThesaurusVisibility);
    }

    @Test
    void listPublicThesauri_returnsServiceResult() {
        var expected = List.of(new PublicThesaurusSummaryResponse("TH1", "siamois", List.of()));
        when(restThesaurusVisibility.currentUserId()).thenReturn(null);
        when(restThesaurusVisibility.visibleIds(null)).thenReturn(List.of("TH1"));
        when(thesaurusPublicReadService.listByIds(List.of("TH1"))).thenReturn(expected);

        var response = controller.listPublicThesauri();

        assertEquals(expected, response);
    }
}
