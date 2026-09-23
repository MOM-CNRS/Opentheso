package fr.cnrs.opentheso.v2.collection.read;

import fr.cnrs.opentheso.v2.shared.repository.CollectionTreeQueryRepository;
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
class CollectionReadServiceTest {

    @Mock
    private CollectionTreeQueryRepository collectionTreeQueryRepository;

    @InjectMocks
    private CollectionReadService service;

    @Test
    void loadDetail_returnsEmptyWhenBlankIds() {
        assertTrue(service.loadDetail("", "g1", "fr").isEmpty());
        assertTrue(service.loadDetail("TH1", "", "fr").isEmpty());
    }

    @Test
    void loadDetail_mapsGroupOverview() {
        when(collectionTreeQueryRepository.findGroupHeader("g1", "TH1", "fr", true))
                .thenReturn(Optional.of(new Object[]{"g1", "Collection", "N1", "ark:/1", "hdl:1", "MT"}));
        when(collectionTreeQueryRepository.findGroupType("MT"))
                .thenReturn(Optional.of(new Object[]{"Collection", "skos:Collection"}));
        when(collectionTreeQueryRepository.countMemberConcepts("TH1", "g1")).thenReturn(2);
        when(collectionTreeQueryRepository.findGroupTranslations("g1", "TH1", "fr"))
                .thenReturn(List.<Object[]>of(new Object[]{"en", "Collection EN"}));
        when(collectionTreeQueryRepository.findNotesByIdentifier("g1", "TH1", "fr"))
                .thenReturn(List.<Object[]>of(new Object[]{1, "note", "fr", "Value"}));
        when(collectionTreeQueryRepository.findMemberConcepts("g1", "TH1", "fr", 4000))
                .thenReturn(List.<Object[]>of(new Object[]{"C1", "Concept 1"}));

        var detail = service.loadDetail("TH1", "g1", "fr");

        assertTrue(detail.isPresent());
        assertEquals("g1", detail.get().groupId());
        assertEquals("Collection", detail.get().label());
        assertEquals("MT", detail.get().typeCode());
        assertEquals("Collection", detail.get().typeLabel());
        assertEquals("skos:Collection", detail.get().typeSkosLabel());
        assertEquals(2, detail.get().memberCount());
        assertEquals("N1", detail.get().notation());
        assertEquals(1, detail.get().translations().size());
        assertEquals(1, detail.get().notes().size());
        assertEquals(1, detail.get().members().size());
        assertEquals("C1", detail.get().members().get(0).conceptId());
        assertEquals("Concept 1", detail.get().members().get(0).label());
    }
}
