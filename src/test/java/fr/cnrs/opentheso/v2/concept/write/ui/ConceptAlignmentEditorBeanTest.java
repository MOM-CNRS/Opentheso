package fr.cnrs.opentheso.v2.concept.write.ui;

import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.concept.alignment.ui.ConceptAlignmentAdminBean;
import fr.cnrs.opentheso.v2.concept.model.ConceptAlignment;
import fr.cnrs.opentheso.v2.concept.model.ConceptSummary;
import fr.cnrs.opentheso.v2.concept.session.ConceptNavigationSupport;
import fr.cnrs.opentheso.v2.concept.session.ConceptSelectionContext;
import fr.cnrs.opentheso.v2.concept.write.model.ConceptWriteAlignmentType;
import fr.cnrs.opentheso.v2.concept.write.model.MutationResult;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddManualAlignmentCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteAlignmentCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.UpdateAlignmentCommand;
import fr.cnrs.opentheso.v2.concept.write.policy.ConceptWritePolicy;
import fr.cnrs.opentheso.v2.concept.write.service.ConceptAlignmentMutationService;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.primefaces.PrimeFaces;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConceptAlignmentEditorBeanTest {

    @Mock
    private ConceptAlignmentMutationService conceptAlignmentMutationService;
    @Mock
    private ConceptSelectionContext conceptSelectionContext;
    @Mock
    private ConceptNavigationSupport conceptNavigationSupport;
    @Mock
    private ThesaurusContext thesaurusContext;
    @Mock
    private UserSession userSession;
    @Mock
    private ConceptWritePolicy conceptWritePolicy;
    @Mock
    private ObjectProvider<ConceptAlignmentAdminBean> conceptAlignmentAdminBean;

    private ConceptAlignmentEditorBean bean;

    private static final ConceptSummary ACTIVE_CONCEPT = new ConceptSummary(
            "C1", "TH1", "Label", "fr", "val", null, null, null, null, null, null);

    @BeforeEach
    void setUp() {
        bean = new ConceptAlignmentEditorBean(
                conceptAlignmentMutationService, conceptSelectionContext, conceptNavigationSupport,
                thesaurusContext, userSession, conceptWritePolicy, conceptAlignmentAdminBean);
        lenient().when(conceptWritePolicy.canMutateAlignments(eq(userSession), anyBoolean())).thenReturn(true);
        lenient().when(conceptSelectionContext.hasSelection()).thenReturn(true);
        lenient().when(conceptSelectionContext.getSummary()).thenReturn(ACTIVE_CONCEPT);
        lenient().when(conceptAlignmentAdminBean.getIfAvailable()).thenReturn(null);
    }

    @Test
    void isManagerActionsAvailable_falseWhenDeniedByPolicy() {
        when(conceptWritePolicy.canMutateAlignments(userSession, false)).thenReturn(false);

        assertEquals(false, bean.isManagerActionsAvailable());
    }

    @Test
    void isManagerActionsAvailable_trueWhenAllowedByPolicy() {
        when(conceptWritePolicy.canMutateAlignments(userSession, false)).thenReturn(true);

        assertEquals(true, bean.isManagerActionsAvailable());
    }

    @Test
    void prepareEdit_populatesFieldsFromSelectedAlignment() {
        when(conceptAlignmentMutationService.listAlignmentTypes())
                .thenReturn(List.of(new ConceptWriteAlignmentType(1, "Exact", "skos:exactMatch")));
        var alignment = new ConceptAlignment("7", "http://example.org/x", "skos:exactMatch", "Wikidata", true, 1);

        bean.prepareEdit(alignment);

        assertEquals(7, bean.getEditingAlignmentId());
        assertEquals(1, bean.getEditAlignmentType());
        assertEquals("http://example.org/x", bean.getEditAlignmentUri());
        assertEquals("Wikidata", bean.getEditAlignmentSource());
    }

    @Test
    void prepareDelete_setsTargetId() {
        var alignment = new ConceptAlignment("12", "http://example.org/y", "skos:closeMatch", "Getty", true, 2);

        bean.prepareDelete(alignment);

        assertEquals(12, bean.getAlignmentToDeleteId());
    }

    @Test
    void addManualAlignment_notAuthorized_showsErrorAndSkipsService() {
        when(conceptWritePolicy.canMutateAlignments(userSession, false)).thenReturn(false);

        try (MockedStatic<MessageUtils> messageUtils = mockStatic(MessageUtils.class)) {
            bean.addManualAlignment();

            messageUtils.verify(() -> MessageUtils.showErrorMessage(any()));
        }
        verify(conceptAlignmentMutationService, never()).addManualAlignment(any());
    }

    @Test
    void addManualAlignment_success_buildsCommandAndRefreshesConcept() {
        when(userSession.getCurrentUserId()).thenReturn(42);
        when(userSession.getCurrentUsername()).thenReturn("admin");
        when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");
        bean.setManualAlignmentType(1);
        bean.setManualAlignmentUri("http://example.org/x");
        bean.setManualAlignmentSource("Wikidata");

        var expectedCommand = new AddManualAlignmentCommand("TH1", "C1", 1, "http://example.org/x", "Wikidata", 42, "admin");
        when(conceptAlignmentMutationService.addManualAlignment(expectedCommand))
                .thenReturn(MutationResult.ok("Alignement ajouté avec succès"));

        PrimeFaces primeFaces = mock(PrimeFaces.class);
        PrimeFaces.Ajax ajax = mock(PrimeFaces.Ajax.class);
        lenient().when(primeFaces.ajax()).thenReturn(ajax);
        try (MockedStatic<PrimeFaces> primeFacesStatic = mockStatic(PrimeFaces.class);
             MockedStatic<MessageUtils> messageUtils = mockStatic(MessageUtils.class)) {
            primeFacesStatic.when(PrimeFaces::current).thenReturn(primeFaces);

            bean.addManualAlignment();

            verify(conceptNavigationSupport).openConcept("C1");
            messageUtils.verify(() -> MessageUtils.showInformationMessage("Alignement ajouté avec succès"));
        }
    }

    @Test
    void updateAlignment_skipsServiceWhenNoAlignmentBeingEdited() {
        try (MockedStatic<MessageUtils> messageUtils = mockStatic(MessageUtils.class)) {
            bean.updateAlignment();

            messageUtils.verify(() -> MessageUtils.showErrorMessage(any()));
        }
        verify(conceptAlignmentMutationService, never()).updateAlignment(any(UpdateAlignmentCommand.class));
    }

    @Test
    void deleteAlignment_skipsServiceWhenNoTargetSelected() {
        try (MockedStatic<MessageUtils> messageUtils = mockStatic(MessageUtils.class)) {
            bean.deleteAlignment();

            messageUtils.verify(() -> MessageUtils.showErrorMessage(any()));
        }
        verify(conceptAlignmentMutationService, never()).deleteAlignment(any(DeleteAlignmentCommand.class));
    }
}
