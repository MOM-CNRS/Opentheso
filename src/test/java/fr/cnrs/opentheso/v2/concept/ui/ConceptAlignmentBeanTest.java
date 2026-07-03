package fr.cnrs.opentheso.v2.concept.ui;

import fr.cnrs.opentheso.models.alignment.NodeAlignmentType;
import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.candidat.session.CandidatAlignmentSupport;
import fr.cnrs.opentheso.v2.concept.model.ConceptAlignment;
import fr.cnrs.opentheso.v2.concept.model.ConceptSummary;
import fr.cnrs.opentheso.v2.concept.service.ConceptAlignmentMutationService;
import fr.cnrs.opentheso.v2.concept.session.ConceptNavigationSupport;
import fr.cnrs.opentheso.v2.concept.session.ConceptSelectionContext;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import fr.cnrs.opentheso.v2.shared.ui.V2LocaleBean;
import fr.cnrs.opentheso.v2.test.support.PrimeFacesTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConceptAlignmentBeanTest {

    @Mock
    private ConceptAlignmentMutationService conceptAlignmentMutationService;
    @Mock
    private CandidatAlignmentSupport candidatAlignmentSupport;
    @Mock
    private ConceptSelectionContext conceptSelectionContext;
    @Mock
    private ConceptNavigationSupport conceptNavigationSupport;
    @Mock
    private ThesaurusContext thesaurusContext;
    @Mock
    private UserSession userSession;
    @Mock
    private V2LocaleBean v2LocaleBean;

    private ConceptAlignmentBean bean;

    @BeforeEach
    void setUp() {
        bean = new ConceptAlignmentBean(
                conceptAlignmentMutationService,
                candidatAlignmentSupport,
                conceptSelectionContext,
                conceptNavigationSupport,
                thesaurusContext,
                userSession,
                v2LocaleBean
        );
    }

    @Test
    void addManualAlignment_refreshesConceptWhenSaved() {
        when(userSession.getCurrentUserId()).thenReturn(42);
        when(conceptSelectionContext.hasSelection()).thenReturn(true);
        when(conceptSelectionContext.getSummary()).thenReturn(summary());
        when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");
        bean.setManualAlignmentUri("http://example.org");
        bean.setManualAlignmentSource("");
        bean.setManualAlignmentType(1);
        when(conceptAlignmentMutationService.addManualAlignment(
                eq(42), eq(""), eq("http://example.org"), eq(1), eq("C1"), eq("TH1")
        )).thenReturn(true);

        try (var pf = PrimeFacesTestSupport.open();
             var messages = mockStatic(MessageUtils.class)) {
            bean.addManualAlignment();
        }

        verify(conceptNavigationSupport).refreshSelectedConcept();
    }

    @Test
    void addManualAlignment_skipsWhenNoSelection() {
        when(userSession.getCurrentUserId()).thenReturn(42);
        when(conceptSelectionContext.hasSelection()).thenReturn(false);
        bean.setManualAlignmentUri("http://example.org");

        try (var messages = mockStatic(MessageUtils.class)) {
            bean.addManualAlignment();
        }

        verify(conceptAlignmentMutationService, never()).addManualAlignment(
                anyInt(), anyString(), anyString(), anyInt(), anyString(), anyString());
    }

    @Test
    void deleteAlignment_refreshesConceptWhenDeleted() {
        when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");
        when(conceptAlignmentMutationService.deleteAlignment(7, "TH1")).thenReturn(true);
        bean.prepareDelete(new ConceptAlignment("7", "http://example.org", "exactMatch", "source", true));

        try (var pf = PrimeFacesTestSupport.open();
             var messages = mockStatic(MessageUtils.class)) {
            bean.deleteAlignment();
        }

        verify(conceptNavigationSupport).refreshSelectedConcept();
    }

    private static ConceptSummary summary() {
        return new ConceptSummary("C1", "TH1", "Label", "fr", "D", "", "concept", "", "", "", "");
    }

    private static NodeAlignmentType type(int id, String label) {
        return NodeAlignmentType.builder().id(id).label(label).labelSkos(label).isocode("").build();
    }
}
