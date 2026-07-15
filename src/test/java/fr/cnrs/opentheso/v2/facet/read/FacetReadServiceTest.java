package fr.cnrs.opentheso.v2.facet.read;

import fr.cnrs.opentheso.v2.concept.model.ConceptHeaderRow;
import fr.cnrs.opentheso.v2.concept.model.ConceptTreeRow;
import fr.cnrs.opentheso.v2.shared.repository.ConceptQueryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FacetReadServiceTest {

    @Mock
    private ConceptQueryRepository conceptQueryRepository;

    @InjectMocks
    private FacetReadService service;

    @Test
    void loadDetail_returnsEmptyWhenBlankIds() {
        assertTrue(service.loadDetail("TH1", "", "fr").isEmpty());
    }

    @Test
    void loadDetail_mapsFacetOverview() {
        when(conceptQueryRepository.findFacetHeader("F1", "TH1", "fr"))
                .thenReturn(Optional.of(new Object[]{"F1", "C1", "Facet", "fr"}));
        when(conceptQueryRepository.findConceptHeader("C1", "TH1", "fr"))
                .thenReturn(Optional.of(new ConceptHeaderRow(
                        "C1", "TH1", "Parent", "fr", "val", null, null, null, null, null, null)));
        when(conceptQueryRepository.findFacetMembers("F1", "TH1", "fr"))
                .thenReturn(List.of(new ConceptTreeRow("C2", "", "Member", "D", false)));
        when(conceptQueryRepository.findFacetTranslations("F1", "TH1", "fr"))
                .thenReturn(List.<Object[]>of(new Object[]{"en", "Facet EN"}));
        when(conceptQueryRepository.findNotesByIdentifier("F1", "TH1", "fr"))
                .thenReturn(List.<Object[]>of(new Object[]{3, "definition", "fr", "Def"}));

        var detail = service.loadDetail("TH1", "F1", "fr");

        assertTrue(detail.isPresent());
        assertEquals("F1", detail.get().facetId());
        assertEquals("Facet", detail.get().label());
        assertEquals("C1", detail.get().parentConceptId());
        assertEquals("Parent", detail.get().parentConceptLabel());
        assertEquals(1, detail.get().members().size());
        assertEquals(1, detail.get().translations().size());
        assertEquals(1, detail.get().notes().size());
    }
}
