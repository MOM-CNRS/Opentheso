package fr.cnrs.opentheso.v2.collection.read;

import fr.cnrs.opentheso.v2.collection.model.CollectionDetail;
import fr.cnrs.opentheso.v2.collection.model.CollectionTreeNode;
import fr.cnrs.opentheso.v2.shared.repository.CollectionTreeQueryRepository;
import fr.cnrs.opentheso.v2.shared.session.AuthenticatedUserSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CollectionTreeConsultationServiceTest {

    @Mock
    private CollectionTreeQueryRepository collectionTreeQueryRepository;
    @Mock
    private AuthenticatedUserSource authenticatedUserSource;

    private CollectionTreeConsultationService service;

    @BeforeEach
    void setUp() {
        service = new CollectionTreeConsultationService(collectionTreeQueryRepository, authenticatedUserSource);
    }

    @Test
    void loadRoots_mapsPublicGroupsAlphabetically() {
        when(authenticatedUserSource.isLoggedIn()).thenReturn(false);
        when(collectionTreeQueryRepository.findRootGroups("TH1", "fr", false)).thenReturn(List.<Object[]>of(
                new Object[]{"G2", "Verre", "02", true},
                new Object[]{"G1", "Matière", "01", false}
        ));

        List<CollectionTreeNode> roots = service.loadRoots("TH1", "fr", false);

        assertEquals(2, roots.size());
        assertEquals("G1", roots.get(0).id());
        assertEquals("Matière", roots.get(0).label());
        assertEquals("group", roots.get(0).nodeType());
        assertFalse(roots.get(0).hasChildren());
        assertEquals("G2", roots.get(1).id());
        assertTrue(roots.get(1).hasChildren());
        verify(collectionTreeQueryRepository).findRootGroups("TH1", "fr", false);
    }

    @Test
    void loadRoots_includesPrivateWhenLoggedIn() {
        when(authenticatedUserSource.isLoggedIn()).thenReturn(true);
        when(collectionTreeQueryRepository.findRootGroups("TH1", "fr", true)).thenReturn(List.of());

        service.loadRoots("TH1", "fr", true);

        verify(collectionTreeQueryRepository).findRootGroups("TH1", "fr", true);
    }

    @Test
    void loadAll_listsEveryCollectionAlphabetically() {
        when(authenticatedUserSource.isLoggedIn()).thenReturn(false);
        when(collectionTreeQueryRepository.findAllGroups("TH1", "fr", false)).thenReturn(List.<Object[]>of(
                new Object[]{"G2", "Verre", "02"},
                new Object[]{"G1", "Matière", "01"},
                new Object[]{"G3", "Objet", "03"}
        ));

        List<CollectionTreeNode> all = service.loadAll("TH1", "fr", false);

        assertEquals(List.of("G1", "G3", "G2"), all.stream().map(CollectionTreeNode::id).toList());
        assertEquals("group", all.get(0).nodeType());
        verify(collectionTreeQueryRepository).findAllGroups("TH1", "fr", false);
    }

    @Test
    void loadAll_returnsEmptyWhenThesaurusMissing() {
        assertTrue(service.loadAll(" ", "fr", false).isEmpty());
        verifyNoInteractions(collectionTreeQueryRepository);
    }

    @Test
    void loadChildren_putsSubcollectionsBeforeConcepts() {
        when(authenticatedUserSource.isLoggedIn()).thenReturn(false);
        when(collectionTreeQueryRepository.findChildGroups("G1", "TH1", "fr", false)).thenReturn(List.<Object[]>of(
                new Object[]{"G1a", "Céramique", "01.1", true}
        ));
        when(collectionTreeQueryRepository.findMemberConcepts(eq("G1"), eq("TH1"), eq("fr"), anyInt()))
                .thenReturn(List.<Object[]>of(
                        new Object[]{"C2", "Adobe", "N2", "DEP"},
                        new Object[]{"C1", "Argile", "N1", "C"}
                ));

        List<CollectionTreeNode> children = service.loadChildren("G1", "TH1", "fr", false);

        assertEquals("subGroup", children.get(0).nodeType());
        assertEquals("G1a", children.get(0).id());
        assertEquals("file", children.get(1).nodeType());
        assertEquals("C2", children.get(1).id());
        assertEquals("deprecie", children.get(1).status());
        assertEquals("C1", children.get(2).id());
        assertEquals("valide", children.get(2).status());
    }

    @Test
    void loadChildren_truncatesAfter4000Concepts() {
        when(authenticatedUserSource.isLoggedIn()).thenReturn(false);
        when(collectionTreeQueryRepository.findChildGroups("G1", "TH1", "fr", false)).thenReturn(List.of());
        List<Object[]> members = new ArrayList<>();
        for (int i = 0; i < 4001; i++) {
            members.add(new Object[]{"C" + i, "Label " + i, "", "C"});
        }
        when(collectionTreeQueryRepository.findMemberConcepts(eq("G1"), eq("TH1"), eq("fr"), anyInt()))
                .thenReturn(members);

        List<CollectionTreeNode> children = service.loadChildren("G1", "TH1", "fr", false);

        assertEquals(4001, children.size());
        assertEquals("more", children.get(4000).nodeType());
        assertEquals("....", children.get(4000).id());
    }

    @Test
    void loadDetail_mapsHeaderNotesAndTranslations() {
        when(authenticatedUserSource.isLoggedIn()).thenReturn(true);
        when(collectionTreeQueryRepository.findGroupHeader("G1", "TH1", "fr", true))
                .thenReturn(Optional.of(new Object[]{
                        "G1", "Matière", "01", "ark:/1", "hdl:1", "MT", "2020-01-02", "2024-03-04"
                }));
        when(collectionTreeQueryRepository.findGroupType("MT"))
                .thenReturn(Optional.of(new Object[]{"Microthesaurus", "skos:Collection"}));
        when(collectionTreeQueryRepository.countMemberConcepts("TH1", "G1")).thenReturn(12);
        when(collectionTreeQueryRepository.findMemberConcepts(eq("G1"), eq("TH1"), eq("fr"), anyInt()))
                .thenReturn(List.<Object[]>of(
                        new Object[]{"C1", "Argile", "N1", "C"},
                        new Object[]{"C2", "Adobe", "N2", "C"}
                ));
        when(collectionTreeQueryRepository.findGroupTranslations("G1", "TH1", "fr"))
                .thenReturn(List.<Object[]>of(new Object[]{"en", "Material"}));
        when(collectionTreeQueryRepository.findGroupNotes("G1", "TH1", "fr"))
                .thenReturn(List.<Object[]>of(new Object[]{"definition", "fr", "Note de définition"}));

        CollectionDetail detail = service.loadDetail("TH1", "G1", "fr");

        assertEquals("G1", detail.groupId());
        assertEquals("Matière", detail.label());
        assertEquals("skos:Collection", detail.typeSkosLabel());
        assertEquals(12, detail.memberCount());
        assertEquals(2, detail.members().size());
        assertEquals("C1", detail.members().get(0).conceptId());
        assertEquals("Argile", detail.members().get(0).label());
        assertEquals("2020-01-02", detail.created());
        assertEquals(1, detail.translations().size());
        assertEquals("Définition", detail.notes().get(0).typeLabel());
    }

    @Test
    void loadDetail_returnsEmptyWhenMissing() {
        when(authenticatedUserSource.isLoggedIn()).thenReturn(false);
        when(collectionTreeQueryRepository.findGroupHeader("GX", "TH1", "fr", false))
                .thenReturn(Optional.empty());

        CollectionDetail detail = service.loadDetail("TH1", "GX", "fr");

        assertEquals("", detail.groupId());
    }
}
