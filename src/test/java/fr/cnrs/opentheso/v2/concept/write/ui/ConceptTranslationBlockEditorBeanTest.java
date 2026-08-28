package fr.cnrs.opentheso.v2.concept.write.ui;

import fr.cnrs.opentheso.v2.concept.model.ConceptDetail;
import fr.cnrs.opentheso.v2.concept.model.ConceptLabel;
import fr.cnrs.opentheso.v2.concept.model.ConceptSummary;
import fr.cnrs.opentheso.v2.concept.session.ConceptSelectionContext;
import fr.cnrs.opentheso.v2.concept.ui.ThesaurusViewBean;
import fr.cnrs.opentheso.v2.concept.write.model.ConceptWriteLanguage;
import fr.cnrs.opentheso.v2.concept.write.model.MutationResult;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddSynonymCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddTranslationCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteSynonymCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteTranslationCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.UpdateTranslationCommand;
import fr.cnrs.opentheso.v2.concept.write.policy.ConceptWritePolicy;
import fr.cnrs.opentheso.v2.concept.write.service.ConceptLexicalMutationService;
import fr.cnrs.opentheso.v2.concept.write.service.ConceptWriteMetadataService;
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
class ConceptTranslationBlockEditorBeanTest {

    @Mock
    private ThesaurusViewBean thesaurusViewBean;
    @Mock
    private ConceptLexicalMutationService conceptLexicalMutationService;
    @Mock
    private ConceptWriteMetadataService conceptWriteMetadataService;
    @Mock
    private ConceptWritePolicy conceptWritePolicy;
    @Mock
    private UserSession userSession;
    @Mock
    private ConceptSelectionContext conceptSelectionContext;

    private ConceptTranslationBlockEditorBean bean;
    private String ficheEditCard;

    @BeforeEach
    void setUp() {
        bean = new ConceptTranslationBlockEditorBean(
                thesaurusViewBean,
                conceptLexicalMutationService,
                conceptWriteMetadataService,
                conceptWritePolicy,
                userSession,
                conceptSelectionContext
        );
        lenient().when(conceptWritePolicy.canMutateLexicalContent(eq(userSession), anyBoolean())).thenReturn(true);
        lenient().when(thesaurusViewBean.getId()).thenReturn("TH1");
        lenient().when(thesaurusViewBean.getSelectedLang()).thenReturn("fr");
        lenient().when(thesaurusViewBean.isSelectedConceptDeprecated()).thenReturn(false);
        lenient().when(userSession.getCurrentUserId()).thenReturn(7);
        lenient().when(userSession.getCurrentUsername()).thenReturn("alice");
        lenient().when(conceptWriteMetadataService.listUsedLanguages("TH1", "fr")).thenReturn(List.of(
                new ConceptWriteLanguage("fr", "Français"),
                new ConceptWriteLanguage("en", "English"),
                new ConceptWriteLanguage("de", "Deutsch")
        ));
        lenient().doAnswer(invocation -> {
            ficheEditCard = invocation.getArgument(0);
            return null;
        }).when(thesaurusViewBean).setFicheEditCard(nullable(String.class));
        lenient().when(thesaurusViewBean.getFicheEditCard()).thenAnswer(invocation -> ficheEditCard);
    }

    @Test
    void startEditing_skipsWorkLanguageAndFillsAlts() {
        when(thesaurusViewBean.getSelectedConcept()).thenReturn(detail(List.of(
                new ConceptLabel("fr", "Chat", true, false),
                new ConceptLabel("en", "Cat", true, false),
                new ConceptLabel("en", "Kitty", false, false)
        )));

        bean.startEditing();

        assertTrue(bean.isEditing());
        assertEquals(1, bean.getRows().size());
        assertEquals("en", bean.getRows().get(0).getLang());
        assertEquals("Cat", bean.getRows().get(0).getValue());
        assertEquals("Kitty", bean.getRows().get(0).getAlts());
        assertTrue(bean.getRows().get(0).isExisting());
        assertTrue(bean.languagesFor(bean.getRows().get(0)).stream()
                .noneMatch(lang -> "fr".equals(lang.code())));
        verify(conceptSelectionContext).update("TH1", thesaurusViewBean.getSelectedConcept());
    }

    @Test
    void save_skipsWhenNotAuthorized() {
        when(conceptWritePolicy.canMutateLexicalContent(userSession, false)).thenReturn(false);
        when(thesaurusViewBean.getSelectedConcept()).thenReturn(detail(List.of()));

        bean.startEditing();
        bean.save();

        verify(conceptLexicalMutationService, never()).addTranslation(any());
        assertFalse(bean.isEditable());
    }

    @Test
    void isEditing_resetsWhenAnotherConceptIsOpened() {
        when(thesaurusViewBean.getSelectedConcept()).thenReturn(detail(List.of()));
        bean.startEditing();
        assertTrue(bean.isEditing());

        when(thesaurusViewBean.getSelectedConcept()).thenReturn(detail("C2", List.of()));

        assertFalse(bean.isEditing());
    }

    @Test
    void addRow_usesFirstFreeLanguage() {
        when(thesaurusViewBean.getSelectedConcept()).thenReturn(detail(List.of(
                new ConceptLabel("en", "Cat", true, false))));
        bean.startEditing();

        bean.addRow();

        assertEquals(2, bean.getRows().size());
        assertEquals("de", bean.getRows().get(1).getLang());
        assertFalse(bean.getRows().get(1).isExisting());
        assertFalse(bean.isCanAddRow());
    }

    @Test
    void save_addsUpdatesRemovesAndDiffsAlts() {
        when(conceptWriteMetadataService.listUsedLanguages("TH1", "fr")).thenReturn(List.of(
                new ConceptWriteLanguage("fr", "Français"),
                new ConceptWriteLanguage("en", "English"),
                new ConceptWriteLanguage("es", "Español"),
                new ConceptWriteLanguage("de", "Deutsch")
        ));
        when(thesaurusViewBean.getSelectedConcept()).thenReturn(detail(List.of(
                new ConceptLabel("en", "Cat", true, false),
                new ConceptLabel("en", "Kitty", false, false),
                new ConceptLabel("de", "Katze", true, false)
        )));
        when(conceptLexicalMutationService.deleteTranslation(any())).thenReturn(MutationResult.ok("ok"));
        when(conceptLexicalMutationService.updateTranslation(any())).thenReturn(MutationResult.ok("ok"));
        when(conceptLexicalMutationService.deleteSynonym(any())).thenReturn(MutationResult.ok("ok"));
        when(conceptLexicalMutationService.addSynonym(any())).thenReturn(MutationResult.ok("ok"));
        when(conceptLexicalMutationService.addTranslation(any())).thenReturn(MutationResult.ok("ok"));
        bean.startEditing();
        bean.getRows().get(0).setValue("Feline");
        bean.getRows().get(0).setAlts("Puss");
        bean.removeRow(1);
        bean.addRow();
        bean.getRows().get(1).setValue("Gato");

        bean.save();

        ArgumentCaptor<UpdateTranslationCommand> updated =
                ArgumentCaptor.forClass(UpdateTranslationCommand.class);
        verify(conceptLexicalMutationService).updateTranslation(updated.capture());
        assertEquals("en", updated.getValue().lang());
        assertEquals("Feline", updated.getValue().value());

        ArgumentCaptor<DeleteSynonymCommand> removedAlt =
                ArgumentCaptor.forClass(DeleteSynonymCommand.class);
        verify(conceptLexicalMutationService).deleteSynonym(removedAlt.capture());
        assertEquals("Kitty", removedAlt.getValue().value());

        ArgumentCaptor<AddSynonymCommand> addedAlt =
                ArgumentCaptor.forClass(AddSynonymCommand.class);
        verify(conceptLexicalMutationService).addSynonym(addedAlt.capture());
        assertEquals("Puss", addedAlt.getValue().value());
        assertEquals("en", addedAlt.getValue().lang());

        ArgumentCaptor<DeleteTranslationCommand> removed =
                ArgumentCaptor.forClass(DeleteTranslationCommand.class);
        verify(conceptLexicalMutationService).deleteTranslation(removed.capture());
        assertEquals("de", removed.getValue().lang());

        ArgumentCaptor<AddTranslationCommand> added =
                ArgumentCaptor.forClass(AddTranslationCommand.class);
        verify(conceptLexicalMutationService).addTranslation(added.capture());
        assertEquals("es", added.getValue().lang());
        assertEquals("Gato", added.getValue().value());

        assertFalse(bean.isEditing());
        assertEquals("Traductions enregistrées", bean.getFlashMessage());
        verify(thesaurusViewBean).reloadSelectedConcept();
    }

    private static ConceptDetail detail(List<ConceptLabel> translations) {
        return detail("C1", translations);
    }

    private static ConceptDetail detail(String id, List<ConceptLabel> translations) {
        var summary = new ConceptSummary(id, "TH1", "Chat", "fr", "D", "", "concept", "", "", "", "");
        return new ConceptDetail(
                summary,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                translations,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList()
        );
    }
}
