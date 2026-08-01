package fr.cnrs.opentheso.v2.facet.ui;

import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.concept.model.FacetDetailOverview;
import fr.cnrs.opentheso.v2.concept.ui.ThesaurusBrowseBean;
import fr.cnrs.opentheso.v2.concept.write.model.ConceptSearchSuggestion;
import fr.cnrs.opentheso.v2.concept.write.model.MutationResult;
import fr.cnrs.opentheso.v2.concept.write.service.ConceptLifecycleMutationService;
import fr.cnrs.opentheso.v2.concept.write.service.ConceptNoteMutationService;
import fr.cnrs.opentheso.v2.concept.write.service.ConceptWriteMetadataService;
import fr.cnrs.opentheso.v2.concept.write.service.ConceptWriteSearchService;
import fr.cnrs.opentheso.v2.facet.read.FacetReadService;
import fr.cnrs.opentheso.v2.facet.write.model.command.AddFacetMemberCommand;
import fr.cnrs.opentheso.v2.facet.write.service.FacetMutationService;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.v2.concept.write.policy.ConceptWritePolicy;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.primefaces.PrimeFaces;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FacetDetailEditorBeanTest {

    @Mock private FacetMutationService facetMutationService;
    @Mock private FacetReadService facetReadService;
    @Mock private ConceptWriteSearchService conceptWriteSearchService;
    @Mock private ConceptWriteMetadataService conceptWriteMetadataService;
    @Mock private ConceptLifecycleMutationService conceptLifecycleMutationService;
    @Mock private ConceptNoteMutationService conceptNoteMutationService;
    @Mock private ThesaurusContext thesaurusContext;
    @Mock private UserSession userSession;
    @Mock private ConceptWritePolicy conceptWritePolicy;
    @Mock private ThesaurusBrowseBean thesaurusBrowseBean;

    private FacetDetailEditorBean bean;

    private static final FacetDetailOverview FACET = new FacetDetailOverview(
            "F1", "Facet", "fr", "C1", "Parent", List.of(), List.of(), List.of());

    @BeforeEach
    void setUp() {
        bean = new FacetDetailEditorBean(
                facetMutationService, facetReadService, conceptWriteSearchService,
                conceptWriteMetadataService, conceptLifecycleMutationService, conceptNoteMutationService,
                thesaurusContext, userSession, conceptWritePolicy, thesaurusBrowseBean);
        lenient().when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");
        lenient().when(thesaurusContext.resolveWorkLanguage()).thenReturn("fr");
        lenient().when(conceptWritePolicy.canMutateHierarchicalRelations(userSession, false)).thenReturn(true);
    }

    @Test
    void isManagerActionsAvailable_trueForManager() {
        when(conceptWritePolicy.canMutateHierarchicalRelations(userSession, false)).thenReturn(true);

        assertTrue(bean.isManagerActionsAvailable());
    }

    @Test
    void isManagerActionsAvailable_falseForGuest() {
        when(conceptWritePolicy.canMutateHierarchicalRelations(userSession, false)).thenReturn(false);

        assertFalse(bean.isManagerActionsAvailable());
    }

    @Test
    void prepareCreateUnderCurrentConcept_prefillParentFromSelectedConcept() {
        var summary = mock(fr.cnrs.opentheso.v2.concept.model.ConceptSummary.class);
        when(summary.conceptId()).thenReturn("C9");
        when(summary.preferredLabel()).thenReturn("Concept neuf");
        var detail = mock(fr.cnrs.opentheso.v2.concept.model.ConceptDetail.class);
        when(detail.summary()).thenReturn(summary);
        when(thesaurusBrowseBean.getSelectedConcept()).thenReturn(detail);

        bean.prepareCreateUnderCurrentConcept();

        assertEquals("Concept neuf", bean.getParentConceptLabel());
        assertEquals("C9", bean.getSelectedParentConcept().conceptId());
        assertEquals("", bean.getLabel());
    }

    @Test
    void prepareModify_loadsFacetLabel() {
        when(thesaurusBrowseBean.getSelectedFacet()).thenReturn(FACET);

        bean.prepareModify();

        assertEquals("Facet", bean.getLabel());
    }

    @Test
    void submitAddMember_rejectsMissingConcept() {
        when(thesaurusBrowseBean.getSelectedFacet()).thenReturn(FACET);

        try (MockedStatic<MessageUtils> messageUtils = mockStatic(MessageUtils.class)) {
            bean.submitAddMember();
            messageUtils.verify(() -> MessageUtils.showErrorMessage("Sélection invalide !"));
        }
        verify(facetMutationService, never()).addMember(any());
    }

    @Test
    void submitAddMember_withBranch_delegatesToService() {
        when(thesaurusBrowseBean.getSelectedFacet()).thenReturn(FACET);
        bean.setSelectedConcept(new ConceptSearchSuggestion("C1", "Concept", null, false));
        bean.setApplyToBranch(true);
        when(facetMutationService.addMember(new AddFacetMemberCommand("TH1", "F1", "C1", true)))
                .thenReturn(MutationResult.ok("La branche a bien été ajoutée à la facette"));
        when(facetReadService.loadDetail("TH1", "F1", "fr")).thenReturn(Optional.of(FACET));

        PrimeFaces primeFaces = mock(PrimeFaces.class);
        PrimeFaces.Ajax ajax = mock(PrimeFaces.Ajax.class);
        lenient().when(primeFaces.ajax()).thenReturn(ajax);
        try (MockedStatic<PrimeFaces> primeFacesStatic = mockStatic(PrimeFaces.class);
             MockedStatic<MessageUtils> messageUtils = mockStatic(MessageUtils.class)) {
            primeFacesStatic.when(PrimeFaces::current).thenReturn(primeFaces);

            bean.submitAddMember();

            verify(facetMutationService).addMember(new AddFacetMemberCommand("TH1", "F1", "C1", true));
            messageUtils.verify(() -> MessageUtils.showInformationMessage("La branche a bien été ajoutée à la facette"));
        }
    }
}
