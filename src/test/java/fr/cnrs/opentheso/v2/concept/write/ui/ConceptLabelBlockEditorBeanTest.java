package fr.cnrs.opentheso.v2.concept.write.ui;

import fr.cnrs.opentheso.v2.concept.model.ConceptDetail;
import fr.cnrs.opentheso.v2.concept.model.ConceptRelation;
import fr.cnrs.opentheso.v2.concept.model.ConceptSummary;
import fr.cnrs.opentheso.v2.concept.session.ConceptSelectionContext;
import fr.cnrs.opentheso.v2.concept.ui.ThesaurusViewBean;
import fr.cnrs.opentheso.v2.concept.write.model.MutationResult;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddSynonymCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteSynonymCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.RenamePreferredLabelCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.UpdateSynonymCommand;
import fr.cnrs.opentheso.v2.concept.write.policy.ConceptWritePolicy;
import fr.cnrs.opentheso.v2.concept.write.service.ConceptLexicalMutationService;
import fr.cnrs.opentheso.v2.concept.write.service.ConceptLifecycleMutationService;
import fr.cnrs.opentheso.v2.facet.write.model.command.AddFacetMemberCommand;
import fr.cnrs.opentheso.v2.facet.write.model.command.RemoveFacetMemberCommand;
import fr.cnrs.opentheso.v2.facet.write.service.FacetMutationService;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConceptLabelBlockEditorBeanTest {

    @Mock
    private ThesaurusViewBean thesaurusViewBean;
    @Mock
    private ConceptLifecycleMutationService conceptLifecycleMutationService;
    @Mock
    private ConceptLexicalMutationService conceptLexicalMutationService;
    @Mock
    private FacetMutationService facetMutationService;
    @Mock
    private ConceptWritePolicy conceptWritePolicy;
    @Mock
    private UserSession userSession;
    @Mock
    private ThesaurusContext thesaurusContext;
    @Mock
    private ConceptSelectionContext conceptSelectionContext;

    private ConceptLabelBlockEditorBean bean;
    private String ficheEditCard;

    @BeforeEach
    void setUp() {
        bean = new ConceptLabelBlockEditorBean(
                thesaurusViewBean,
                conceptLifecycleMutationService,
                conceptLexicalMutationService,
                facetMutationService,
                conceptWritePolicy,
                userSession,
                thesaurusContext,
                conceptSelectionContext
        );
        lenient().when(conceptWritePolicy.canMutateLexicalContent(eq(userSession), anyBoolean())).thenReturn(true);
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
    void parseCsv_trimsSkipsBlanksAndKeepsOrder() {
        assertEquals(List.of("a", "b"), ConceptLabelBlockEditorBean.parseCsv(" a, , b ,"));
        assertEquals(List.of(), ConceptLabelBlockEditorBean.parseCsv("  "));
        assertEquals(List.of("un"), ConceptLabelBlockEditorBean.parseCsv("un, un"));
    }

    @Test
    void startEditing_fillsFieldsFromSelectedConcept() {
        when(thesaurusViewBean.getSelectedConcept()).thenReturn(detail(
                "Bronze", List.of("cuivre"), List.of("bronz"),
                List.of(new ConceptRelation("F1", "Matériaux", null))));

        bean.startEditing();

        assertTrue(bean.isEditing());
        assertEquals("Bronze", bean.getPreferredLabel());
        assertEquals("cuivre", bean.getAltLabels());
        assertEquals("bronz", bean.getHiddenLabels());
        assertEquals(1, bean.getSelectedFacets().size());
        assertEquals("F1", bean.getSelectedFacets().get(0).getId());
        assertEquals("[{\"id\":\"F1\",\"label\":\"Matériaux\"}]", bean.getSelectedFacetsJson());
        verify(conceptSelectionContext).update("TH1", thesaurusViewBean.getSelectedConcept());
    }

    @Test
    void save_renamesPreferredLabelWhenChanged() {
        when(thesaurusViewBean.getSelectedConcept()).thenReturn(detail("Bronze", List.of(), List.of()));
        when(conceptLifecycleMutationService.renamePreferredLabel(any()))
                .thenReturn(MutationResult.ok("ok"));
        bean.startEditing();
        bean.setPreferredLabel("Cuivre");

        bean.save();

        ArgumentCaptor<RenamePreferredLabelCommand> captor = ArgumentCaptor.forClass(RenamePreferredLabelCommand.class);
        verify(conceptLifecycleMutationService).renamePreferredLabel(captor.capture());
        assertEquals("Cuivre", captor.getValue().label());
        assertEquals("fr", captor.getValue().lang());
        assertFalse(captor.getValue().forced());
        verify(thesaurusViewBean).reloadSelectedConcept();
        assertFalse(bean.isEditing());
        assertEquals("Libellé enregistré", bean.getFlashMessage());
    }

    @Test
    void save_addsAndDeletesSynonymsFromCommaLists() {
        when(thesaurusViewBean.getSelectedConcept())
                .thenReturn(detail("Bronze", List.of("old"), List.of("secret")));
        when(conceptLexicalMutationService.deleteSynonym(any())).thenReturn(MutationResult.ok("ok"));
        when(conceptLexicalMutationService.addSynonym(any())).thenReturn(MutationResult.ok("ok"));
        bean.startEditing();
        bean.setAltLabels("new-a, new-b");
        bean.setHiddenLabels("");

        bean.save();

        ArgumentCaptor<DeleteSynonymCommand> deleted = ArgumentCaptor.forClass(DeleteSynonymCommand.class);
        verify(conceptLexicalMutationService, org.mockito.Mockito.times(2)).deleteSynonym(deleted.capture());
        assertEquals(List.of("old", "secret"), deleted.getAllValues().stream().map(DeleteSynonymCommand::value).toList());

        ArgumentCaptor<AddSynonymCommand> added = ArgumentCaptor.forClass(AddSynonymCommand.class);
        verify(conceptLexicalMutationService, org.mockito.Mockito.times(2)).addSynonym(added.capture());
        assertEquals("new-a", added.getAllValues().get(0).value());
        assertFalse(added.getAllValues().get(0).hidden());
        assertEquals("new-b", added.getAllValues().get(1).value());
        assertFalse(added.getAllValues().get(1).hidden());
        verify(conceptLifecycleMutationService, never()).renamePreferredLabel(any());
        assertFalse(bean.isEditing());
    }

    @Test
    void save_movesAltToHiddenViaUpdate() {
        when(thesaurusViewBean.getSelectedConcept()).thenReturn(detail("Bronze", List.of("cuivre"), List.of()));
        when(conceptLexicalMutationService.updateSynonym(any())).thenReturn(MutationResult.ok("ok"));
        bean.startEditing();
        bean.setAltLabels("");
        bean.setHiddenLabels("cuivre");

        bean.save();

        ArgumentCaptor<UpdateSynonymCommand> captor = ArgumentCaptor.forClass(UpdateSynonymCommand.class);
        verify(conceptLexicalMutationService).updateSynonym(captor.capture());
        assertEquals("cuivre", captor.getValue().oldValue());
        assertTrue(captor.getValue().hidden());
        verify(conceptLexicalMutationService, never()).addSynonym(any());
        verify(conceptLexicalMutationService, never()).deleteSynonym(any());
    }

    @Test
    void save_keepsEditingOnDuplicateAndExposesForceSave() {
        when(thesaurusViewBean.getSelectedConcept()).thenReturn(detail("Bronze", List.of(), List.of()));
        when(conceptLifecycleMutationService.renamePreferredLabel(any()))
                .thenReturn(MutationResult.duplicate("Le label 'Cuivre' existe déjà ! voulez-vous continuer ?"));
        bean.startEditing();
        bean.setPreferredLabel("Cuivre");

        bean.save();

        assertTrue(bean.isEditing());
        assertTrue(bean.isDuplicateWarning());
        assertTrue(bean.getErrorMessage().contains("existe déjà"));
        verify(thesaurusViewBean, never()).reloadSelectedConcept();

        when(conceptLifecycleMutationService.renamePreferredLabel(any())).thenReturn(MutationResult.ok("ok"));
        bean.saveForced();

        ArgumentCaptor<RenamePreferredLabelCommand> captor = ArgumentCaptor.forClass(RenamePreferredLabelCommand.class);
        verify(conceptLifecycleMutationService, org.mockito.Mockito.times(2)).renamePreferredLabel(captor.capture());
        assertTrue(captor.getAllValues().get(1).forced());
        assertFalse(bean.isEditing());
    }

    @Test
    void save_rejectsOverlapBetweenAltAndHidden() {
        when(thesaurusViewBean.getSelectedConcept()).thenReturn(detail("Bronze", List.of(), List.of()));
        bean.startEditing();
        bean.setAltLabels("cuivre");
        bean.setHiddenLabels("cuivre");

        bean.save();

        verify(conceptLexicalMutationService, never()).addSynonym(any());
        assertTrue(bean.isEditing());
        assertEquals("Une forme ne peut pas être à la fois alternative et cachée.", bean.getErrorMessage());
    }

    @Test
    void save_skipsWhenNotAuthorized() {
        when(conceptWritePolicy.canMutateLexicalContent(userSession, false)).thenReturn(false);
        when(thesaurusViewBean.getSelectedConcept()).thenReturn(detail("Bronze", List.of(), List.of()));

        bean.startEditing();
        bean.save();

        verify(conceptLifecycleMutationService, never()).renamePreferredLabel(any());
        assertFalse(bean.isEditable());
    }

    @Test
    void isEditing_resetsWhenAnotherConceptIsOpened() {
        when(thesaurusViewBean.getSelectedConcept()).thenReturn(detail("Bronze", List.of(), List.of()));
        bean.startEditing();
        assertTrue(bean.isEditing());

        when(thesaurusViewBean.getSelectedConcept()).thenReturn(detail("C2", "Autre", List.of(), List.of()));

        assertFalse(bean.isEditing());
    }

    @Test
    void selectedFacetsJson_roundTripsAndIgnoresMalformed() {
        assertEquals("[]", ConceptLabelBlockEditorBean.toFacetsJson(List.of()));
        assertEquals(
                "[{\"id\":\"F1\",\"label\":\"Matériaux\"}]",
                ConceptLabelBlockEditorBean.toFacetsJson(List.of(new FacetEditRow("F1", "Matériaux")))
        );

        bean.setSelectedFacetsJson("[{\"id\":\"F2\",\"label\":\"Matières\"},{\"id\":\"F2\",\"label\":\"dup\"}]");
        assertEquals(1, bean.getSelectedFacets().size());
        assertEquals("F2", bean.getSelectedFacets().get(0).getId());
        assertEquals("Matières", bean.getSelectedFacets().get(0).getLabel());

        bean.setSelectedFacetsJson("not-json");
        assertEquals("F2", bean.getSelectedFacets().get(0).getId());

        bean.setSelectedFacetsJson("[]");
        assertTrue(bean.getSelectedFacets().isEmpty());
        verify(facetMutationService, never()).addMember(any());
    }

    @Test
    void save_addsAndRemovesFacetMemberships() {
        when(thesaurusViewBean.getSelectedConcept()).thenReturn(detail(
                "Bronze", List.of(), List.of(),
                List.of(new ConceptRelation("F1", "Matériaux", null))));
        when(facetMutationService.removeMember(any())).thenReturn(MutationResult.ok("ok"));
        when(facetMutationService.addMember(any())).thenReturn(MutationResult.ok("ok"));
        bean.startEditing();
        bean.setSelectedFacetsJson("[{\"id\":\"F2\",\"label\":\"Matières\"}]");

        bean.save();

        ArgumentCaptor<RemoveFacetMemberCommand> removed = ArgumentCaptor.forClass(RemoveFacetMemberCommand.class);
        verify(facetMutationService).removeMember(removed.capture());
        assertEquals("F1", removed.getValue().facetId());
        ArgumentCaptor<AddFacetMemberCommand> added = ArgumentCaptor.forClass(AddFacetMemberCommand.class);
        verify(facetMutationService).addMember(added.capture());
        assertEquals("F2", added.getValue().facetId());
        assertFalse(bean.isEditing());
    }

    private static ConceptDetail detail(String pref, List<String> alts, List<String> hidden) {
        return detail("C1", pref, alts, hidden, List.of());
    }

    private static ConceptDetail detail(String pref, List<String> alts, List<String> hidden, List<ConceptRelation> facets) {
        return detail("C1", pref, alts, hidden, facets);
    }

    private static ConceptDetail detail(String id, String pref, List<String> alts, List<String> hidden) {
        return detail(id, pref, alts, hidden, List.of());
    }

    private static ConceptDetail detail(String id, String pref, List<String> alts, List<String> hidden, List<ConceptRelation> facets) {
        var summary = new ConceptSummary(id, "TH1", pref, "fr", "D", "", "concept", "", "", "", "");
        return new ConceptDetail(
                summary,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                alts,
                hidden,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                facets,
                Collections.emptyList(),
                Collections.emptyList()
        );
    }
}
