package fr.cnrs.opentheso.v2.concept.alignment.ui;

import fr.cnrs.opentheso.models.alignment.AlignementSource;
import fr.cnrs.opentheso.v2.concept.alignment.model.AlignmentProposition;
import fr.cnrs.opentheso.v2.concept.alignment.service.ConceptAlignmentAdminService;
import fr.cnrs.opentheso.v2.concept.model.ConceptAlignment;
import fr.cnrs.opentheso.v2.concept.model.ConceptAlignmentGroup;
import fr.cnrs.opentheso.v2.concept.model.ConceptDetail;
import fr.cnrs.opentheso.v2.concept.model.ConceptSummary;
import fr.cnrs.opentheso.v2.concept.session.ConceptSelectionContext;
import fr.cnrs.opentheso.v2.concept.ui.ThesaurusViewBean;
import fr.cnrs.opentheso.v2.concept.write.model.MutationResult;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteAlignmentCommand;
import fr.cnrs.opentheso.v2.concept.write.policy.ConceptWritePolicy;
import fr.cnrs.opentheso.v2.concept.write.service.ConceptAlignmentMutationService;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class ConceptAlignmentAutoSearchBeanTest {

    @Mock
    private ThesaurusViewBean thesaurusViewBean;
    @Mock
    private ConceptAlignmentAdminService conceptAlignmentAdminService;
    @Mock
    private ConceptAlignmentMutationService conceptAlignmentMutationService;
    @Mock
    private ConceptWritePolicy conceptWritePolicy;
    @Mock
    private UserSession userSession;
    @Mock
    private ConceptSelectionContext conceptSelectionContext;

    private ConceptAlignmentAutoSearchBean bean;
    private String ficheEditCard;

    @BeforeEach
    void setUp() {
        bean = new ConceptAlignmentAutoSearchBean(
                thesaurusViewBean,
                conceptAlignmentAdminService,
                conceptAlignmentMutationService,
                conceptWritePolicy,
                userSession,
                conceptSelectionContext
        );
        lenient().when(conceptWritePolicy.canMutateAlignments(eq(userSession), anyBoolean())).thenReturn(true);
        lenient().when(thesaurusViewBean.getId()).thenReturn("TH1");
        lenient().when(thesaurusViewBean.isSelectedConceptDeprecated()).thenReturn(false);
        lenient().when(userSession.getCurrentUserId()).thenReturn(7);
        lenient().when(userSession.getCurrentUsername()).thenReturn("alice");
        lenient().doAnswer(invocation -> {
            ficheEditCard = invocation.getArgument(0);
            return null;
        }).when(thesaurusViewBean).setFicheEditCard(nullable(String.class));
        lenient().when(thesaurusViewBean.getFicheEditCard()).thenAnswer(invocation -> ficheEditCard);
    }

    @Test
    void startSearching_loadsSourcesAndAutoSelectsWhenOnlyOne() {
        when(thesaurusViewBean.getSelectedConcept()).thenReturn(detail(List.of()));
        when(conceptAlignmentAdminService.listActiveSources("TH1")).thenReturn(List.of(wikidata()));
        when(conceptAlignmentAdminService.findActiveSource("TH1", 4)).thenReturn(wikidata());
        when(conceptAlignmentAdminService.searchPropositionsForConcept(
                eq("TH1"), eq("fr"), eq("C1"), eq("Chat"), eq(wikidata())
        )).thenReturn(List.of(hit("http://www.wikidata.org/entity/Q1", "Cat")));

        bean.startSearching();

        assertTrue(bean.isOpen());
        assertEquals(1, bean.getSources().size());
        assertEquals(4, bean.getSelectedSourceId());
        assertEquals(1, bean.getPropositions().size());
        assertEquals("Cat", bean.getPropositions().get(0).getTargetLabel());
        verify(conceptSelectionContext).update("TH1", thesaurusViewBean.getSelectedConcept());
    }

    @Test
    void startSearching_skipsWhenNotAuthorized() {
        when(conceptWritePolicy.canMutateAlignments(userSession, false)).thenReturn(false);
        when(thesaurusViewBean.getSelectedConcept()).thenReturn(detail(List.of()));

        bean.startSearching();

        assertFalse(bean.isOpen());
        verify(conceptAlignmentAdminService, never()).listActiveSources("TH1");
    }

    @Test
    void addProposition_persistsAndMarksAligned() {
        when(thesaurusViewBean.getSelectedConcept()).thenReturn(detail(List.of()));
        when(conceptAlignmentAdminService.listActiveSources("TH1")).thenReturn(List.of(wikidata()));
        when(conceptAlignmentAdminService.findActiveSource("TH1", 4)).thenReturn(wikidata());
        when(conceptAlignmentAdminService.searchPropositionsForConcept(
                eq("TH1"), eq("fr"), eq("C1"), eq("Chat"), eq(wikidata())
        )).thenReturn(List.of(hit("http://www.wikidata.org/entity/Q1", "Cat")));
        when(conceptAlignmentAdminService.acceptProposition(
                eq("TH1"),
                org.mockito.ArgumentMatchers.any(AlignmentProposition.class),
                eq(7),
                eq("alice")
        )).thenReturn(true);

        bean.startSearching();
        when(thesaurusViewBean.getSelectedConcept()).thenReturn(detail(List.of(
                new ConceptAlignmentGroup("exactMatch", "exactMatch", List.of(
                        new ConceptAlignment("1", "http://www.wikidata.org/entity/Q1", "exactMatch", "Wikidata", true, 1)
                ))
        )));

        bean.addProposition(0);

        verify(conceptAlignmentAdminService).acceptProposition(
                eq("TH1"),
                org.mockito.ArgumentMatchers.any(AlignmentProposition.class),
                eq(7),
                eq("alice")
        );
        assertTrue(bean.getPropositions().get(0).isAlreadyAligned());
        assertEquals("Alignement ajouté", bean.getFlashMessage());
        verify(thesaurusViewBean).reloadSelectedConcept();
    }

    @Test
    void isOpen_resetsWhenAnotherConceptIsOpened() {
        when(thesaurusViewBean.getSelectedConcept()).thenReturn(detail(List.of()));
        when(conceptAlignmentAdminService.listActiveSources("TH1")).thenReturn(List.of());
        bean.startSearching();
        assertTrue(bean.isOpen());

        when(thesaurusViewBean.getSelectedConcept()).thenReturn(detail("C2", List.of()));

        assertFalse(bean.isOpen());
    }

    @Test
    void startSearching_withSeveralSources_waitsForChoice() {
        when(thesaurusViewBean.getSelectedConcept()).thenReturn(detail(List.of()));
        when(conceptAlignmentAdminService.listActiveSources("TH1")).thenReturn(List.of(
                wikidata(),
                AlignementSource.builder().id(8).source("Gemet").source_filter("GEMET").build()
        ));

        bean.startSearching();

        assertTrue(bean.isOpen());
        assertEquals(2, bean.getSources().size());
        assertEquals(0, bean.getSelectedSourceId());
        assertTrue(bean.getPropositions().isEmpty());
        verify(conceptAlignmentAdminService, never()).searchPropositionsForConcept(
                any(), any(), any(), any(), any()
        );
    }

    @Test
    void deleteAlignment_persistsAndReloads() {
        when(thesaurusViewBean.getSelectedConcept()).thenReturn(detail(List.of()));
        bean.setAlignmentToDeleteId("12");
        when(conceptAlignmentMutationService.deleteAlignment(any(DeleteAlignmentCommand.class)))
                .thenReturn(MutationResult.ok("Alignement supprimé avec succès"));

        bean.deleteAlignment();

        verify(conceptAlignmentMutationService).deleteAlignment(any(DeleteAlignmentCommand.class));
        assertEquals("Alignement supprimé", bean.getFlashMessage());
        assertEquals("", bean.getAlignmentToDeleteId());
        verify(thesaurusViewBean).reloadSelectedConcept();
        verify(conceptSelectionContext).update("TH1", thesaurusViewBean.getSelectedConcept());
    }

    @Test
    void deleteAlignment_skipsWhenNotAuthorized() {
        when(conceptWritePolicy.canMutateAlignments(userSession, false)).thenReturn(false);
        when(thesaurusViewBean.getSelectedConcept()).thenReturn(detail(List.of()));
        bean.setAlignmentToDeleteId("12");

        bean.deleteAlignment();

        verify(conceptAlignmentMutationService, never()).deleteAlignment(any());
    }

    @Test
    void startComparing_withSeveralSources_waitsForChoice() {
        when(thesaurusViewBean.getSelectedConcept()).thenReturn(detail(List.of()));
        when(conceptAlignmentAdminService.listActiveSources("TH1")).thenReturn(List.of(
                wikidata(),
                AlignementSource.builder().id(8).source("Gemet").source_filter("GEMET").build()
        ));

        bean.startComparing();

        assertTrue(bean.isOpen());
        assertTrue(bean.isComparing());
        assertEquals(0, bean.getSelectedSourceId());
        verify(conceptAlignmentAdminService, never()).searchComparisonsForConcept(
                any(), any(), any(), any(), any(), any(), any()
        );
    }

    @Test
    void startComparing_withoutExistingAlignment_setsError() {
        when(thesaurusViewBean.getSelectedConcept()).thenReturn(detail(List.of()));
        when(conceptAlignmentAdminService.listActiveSources("TH1")).thenReturn(List.of(wikidata()));
        when(conceptAlignmentAdminService.findActiveSource("TH1", 4)).thenReturn(wikidata());
        when(conceptAlignmentAdminService.alignmentsTowardSource(any(), eq(wikidata())))
                .thenReturn(List.of());

        bean.startComparing();

        assertTrue(bean.isComparing());
        assertEquals("Aucun alignement existant vers cette source.", bean.getErrorMessage());
        assertTrue(bean.getPropositions().isEmpty());
        verify(conceptAlignmentAdminService, never()).searchComparisonsForConcept(
                any(), any(), any(), any(), any(), any(), any()
        );
    }

    @Test
    void startComparing_loadsRemoteHitsAgainstLocalAlignment() {
        var local = new ConceptAlignment("1", "http://www.wikidata.org/entity/Q1", "exactMatch", "Wikidata", true, 1);
        when(thesaurusViewBean.getSelectedConcept()).thenReturn(detail(List.of(
                new ConceptAlignmentGroup("exactMatch", "exactMatch", List.of(local))
        )));
        when(conceptAlignmentAdminService.listActiveSources("TH1")).thenReturn(List.of(wikidata()));
        when(conceptAlignmentAdminService.findActiveSource("TH1", 4)).thenReturn(wikidata());
        when(conceptAlignmentAdminService.alignmentsTowardSource(any(), eq(wikidata())))
                .thenReturn(List.of(local));
        when(conceptAlignmentAdminService.searchComparisonsForConcept(
                eq("TH1"), eq("fr"), eq("C1"), eq("Chat"), eq(""), any(), eq(wikidata())
        )).thenReturn(List.of(AlignmentProposition.builder()
                .conceptId("C1")
                .localLabel("Chat")
                .localUri("http://www.wikidata.org/entity/Q1")
                .targetLabel("Felis")
                .targetUri("http://www.wikidata.org/entity/Q9")
                .alreadyAligned(false)
                .build()));

        bean.startComparing();

        assertTrue(bean.isComparing());
        assertEquals(1, bean.getPropositions().size());
        assertEquals("Felis", bean.getPropositions().get(0).getTargetLabel());
        assertFalse(bean.getPropositions().get(0).isAlreadyAligned());
    }

    @Test
    void replaceProposition_persistsAndReloads() {
        var local = new ConceptAlignment("1", "http://www.wikidata.org/entity/Q1", "exactMatch", "Wikidata", true, 1);
        when(thesaurusViewBean.getSelectedConcept()).thenReturn(detail(List.of(
                new ConceptAlignmentGroup("exactMatch", "exactMatch", List.of(local))
        )));
        when(conceptAlignmentAdminService.listActiveSources("TH1")).thenReturn(List.of(wikidata()));
        when(conceptAlignmentAdminService.findActiveSource("TH1", 4)).thenReturn(wikidata());
        when(conceptAlignmentAdminService.alignmentsTowardSource(any(), eq(wikidata())))
                .thenReturn(List.of(local));
        AlignmentProposition remote = AlignmentProposition.builder()
                .conceptId("C1")
                .localUri("http://www.wikidata.org/entity/Q1")
                .targetUri("http://www.wikidata.org/entity/Q9")
                .sourceName("Wikidata")
                .alreadyAligned(false)
                .build();
        when(conceptAlignmentAdminService.searchComparisonsForConcept(
                eq("TH1"), eq("fr"), eq("C1"), eq("Chat"), eq(""), any(), eq(wikidata())
        )).thenReturn(List.of(remote));
        when(conceptAlignmentAdminService.replaceAlignmentFromProposition(
                eq("TH1"), any(AlignmentProposition.class), eq(7), eq("alice")
        )).thenReturn(true);

        bean.startComparing();
        bean.setPropositionToReplaceIndex("0");
        bean.replaceProposition();

        verify(conceptAlignmentAdminService).replaceAlignmentFromProposition(
                eq("TH1"), any(AlignmentProposition.class), eq(7), eq("alice")
        );
        assertEquals("Alignement remplacé", bean.getFlashMessage());
        verify(thesaurusViewBean).reloadSelectedConcept();
    }

    private static AlignementSource wikidata() {
        return AlignementSource.builder()
                .id(4)
                .source("Wikidata")
                .source_filter("WIKIDATA_REST")
                .requete("https://www.wikidata.org")
                .description("Wikidata")
                .build();
    }

    private static AlignmentProposition hit(String uri, String label) {
        return AlignmentProposition.builder()
                .conceptId("C1")
                .localLabel("Chat")
                .targetLabel(label)
                .targetUri(uri)
                .sourceName("Wikidata")
                .sourceId(4)
                .alignmentTypeId(1)
                .build();
    }

    private static ConceptDetail detail(List<ConceptAlignmentGroup> groups) {
        return detail("C1", groups);
    }

    private static ConceptDetail detail(String id, List<ConceptAlignmentGroup> groups) {
        var summary = new ConceptSummary(id, "TH1", "Chat", "fr", "D", "", "concept", "", "", "", "");
        return new ConceptDetail(
                summary,
                List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of(),
                groups,
                List.of(), List.of(), List.of(), List.of(),
                List.of(List.of()),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                null,
                "",
                ""
        );
    }
}
