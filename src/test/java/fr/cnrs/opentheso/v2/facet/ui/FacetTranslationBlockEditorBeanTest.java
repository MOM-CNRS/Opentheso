package fr.cnrs.opentheso.v2.facet.ui;

import fr.cnrs.opentheso.v2.concept.model.FacetDetailOverview;
import fr.cnrs.opentheso.v2.concept.model.GroupTranslationItem;
import fr.cnrs.opentheso.v2.concept.ui.ThesaurusViewBean;
import fr.cnrs.opentheso.v2.concept.write.model.ConceptWriteLanguage;
import fr.cnrs.opentheso.v2.concept.write.model.MutationResult;
import fr.cnrs.opentheso.v2.concept.write.policy.ConceptWritePolicy;
import fr.cnrs.opentheso.v2.concept.write.service.ConceptWriteMetadataService;
import fr.cnrs.opentheso.v2.facet.write.model.command.AddFacetTranslationCommand;
import fr.cnrs.opentheso.v2.facet.write.model.command.DeleteFacetTranslationCommand;
import fr.cnrs.opentheso.v2.facet.write.model.command.UpdateFacetTranslationCommand;
import fr.cnrs.opentheso.v2.facet.write.service.FacetMutationService;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FacetTranslationBlockEditorBeanTest {

    @Mock
    private ThesaurusViewBean thesaurusViewBean;
    @Mock
    private FacetMutationService facetMutationService;
    @Mock
    private ConceptWriteMetadataService conceptWriteMetadataService;
    @Mock
    private ConceptWritePolicy conceptWritePolicy;
    @Mock
    private UserSession userSession;

    private FacetTranslationBlockEditorBean bean;
    private String ficheEditCard;

    @BeforeEach
    void setUp() {
        bean = new FacetTranslationBlockEditorBean(
                thesaurusViewBean,
                facetMutationService,
                conceptWriteMetadataService,
                conceptWritePolicy,
                userSession
        );
        lenient().when(conceptWritePolicy.canMutateLexicalContent(eq(userSession), anyBoolean())).thenReturn(true);
        lenient().when(thesaurusViewBean.getId()).thenReturn("TH1");
        lenient().when(thesaurusViewBean.getSelectedLang()).thenReturn("fr");
        lenient().when(userSession.getCurrentUserId()).thenReturn(7);
        lenient().when(conceptWriteMetadataService.listUsedLanguages("TH1", "fr")).thenReturn(List.of(
                new ConceptWriteLanguage("fr", "Français"),
                new ConceptWriteLanguage("en", "English"),
                new ConceptWriteLanguage("de", "Deutsch"),
                new ConceptWriteLanguage("es", "Español")
        ));
        lenient().doAnswer(invocation -> {
            ficheEditCard = invocation.getArgument(0);
            return null;
        }).when(thesaurusViewBean).setFicheEditCard(nullable(String.class));
        lenient().when(thesaurusViewBean.getFicheEditCard()).thenAnswer(invocation -> ficheEditCard);
    }

    @Test
    void startEditing_skipsWorkLanguage() {
        when(thesaurusViewBean.getSelectedFacet()).thenReturn(facet(
                new GroupTranslationItem("fr", "Techniques"),
                new GroupTranslationItem("en", "Techniques")
        ));

        bean.startEditing();

        assertTrue(bean.isEditing());
        assertEquals(1, bean.getRows().size());
        assertEquals("en", bean.getRows().get(0).getLang());
        assertEquals("Techniques", bean.getRows().get(0).getValue());
        assertTrue(bean.getRows().get(0).isExisting());
        assertTrue(bean.getPickerLanguages().stream().noneMatch(lang -> "fr".equals(lang.code())));
        assertEquals("f-tr", ficheEditCard);
    }

    @Test
    void save_skipsWhenNotAuthorized() {
        when(conceptWritePolicy.canMutateLexicalContent(userSession, false)).thenReturn(false);
        when(thesaurusViewBean.getSelectedFacet()).thenReturn(facet());

        bean.startEditing();
        bean.save();

        verify(facetMutationService, never()).addTranslation(any());
        assertFalse(bean.isEditable());
    }

    @Test
    void isEditing_resetsWhenAnotherFacetIsOpened() {
        when(thesaurusViewBean.getSelectedFacet()).thenReturn(facet());
        bean.startEditing();
        assertTrue(bean.isEditing());

        when(thesaurusViewBean.getSelectedFacet()).thenReturn(facet("F2"));

        assertFalse(bean.isEditing());
    }

    @Test
    void save_addsUpdatesAndRemoves() {
        when(thesaurusViewBean.getSelectedFacet()).thenReturn(facet(
                new GroupTranslationItem("en", "Techniques"),
                new GroupTranslationItem("de", "Techniken")
        ));
        when(facetMutationService.deleteTranslation(any())).thenReturn(MutationResult.ok("ok"));
        when(facetMutationService.updateTranslation(any())).thenReturn(MutationResult.ok("ok"));
        when(facetMutationService.addTranslation(any())).thenReturn(MutationResult.ok("ok"));
        bean.startEditing();
        bean.setTranslationsPayload("en\tCrafts\nes\tTécnicas");

        bean.save();

        ArgumentCaptor<UpdateFacetTranslationCommand> updated =
                ArgumentCaptor.forClass(UpdateFacetTranslationCommand.class);
        verify(facetMutationService).updateTranslation(updated.capture());
        assertEquals("en", updated.getValue().lang());
        assertEquals("Crafts", updated.getValue().label());

        ArgumentCaptor<DeleteFacetTranslationCommand> removed =
                ArgumentCaptor.forClass(DeleteFacetTranslationCommand.class);
        verify(facetMutationService).deleteTranslation(removed.capture());
        assertEquals("de", removed.getValue().lang());

        ArgumentCaptor<AddFacetTranslationCommand> added =
                ArgumentCaptor.forClass(AddFacetTranslationCommand.class);
        verify(facetMutationService).addTranslation(added.capture());
        assertEquals("es", added.getValue().lang());
        assertEquals("Técnicas", added.getValue().label());

        assertFalse(bean.isEditing());
        assertEquals("Traductions enregistrées", bean.getFlashMessage());
        verify(thesaurusViewBean).reloadSelectedConcept();
    }

    @Test
    void save_rejectsWorkLanguageInPayload() {
        when(thesaurusViewBean.getSelectedFacet()).thenReturn(facet());
        bean.startEditing();
        bean.setTranslationsPayload("fr\tTechniques");

        bean.save();

        verify(facetMutationService, never()).addTranslation(any());
        assertTrue(bean.isEditing());
        assertEquals("La langue de travail s'édite dans le bloc Libellé.", bean.getErrorMessage());
    }

    private static FacetDetailOverview facet(GroupTranslationItem... translations) {
        return facet("F1", translations);
    }

    private static FacetDetailOverview facet(String facetId, GroupTranslationItem... translations) {
        return new FacetDetailOverview(
                facetId, "Techniques", "fr", "C1", "Adobe",
                List.of(), List.of(translations), List.of());
    }
}
