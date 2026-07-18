package fr.cnrs.opentheso.v2.candidat.ui;

import fr.cnrs.opentheso.models.alignment.NodeAlignment;
import fr.cnrs.opentheso.v2.candidat.alignment.CandidatAutoAlignmentEngine;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CandidatAutoAlignmentBeanTest {

    @Mock private CandidatAutoAlignmentEngine candidatAutoAlignmentEngine;
    @Mock private ThesaurusContext thesaurusContext;

    private CandidatAutoAlignmentBean bean;

    @BeforeEach
    void setUp() {
        bean = new CandidatAutoAlignmentBean(candidatAutoAlignmentEngine, thesaurusContext);
    }

    @Test
    void prepareForCandidate_delegatesWithResolvedThesaurusContext() {
        when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");
        when(thesaurusContext.resolveWorkLanguage()).thenReturn("fr");

        bean.prepareForCandidate("Concept 1", "C1");

        verify(candidatAutoAlignmentEngine).prepare("Concept 1", "C1", "TH1", "fr");
    }

    @Test
    void hasAlignmentSources_delegatesToEngine() {
        when(candidatAutoAlignmentEngine.hasAlignmentSources()).thenReturn(true);

        assertTrue(bean.hasAlignmentSources());
    }

    @Test
    void searchAlignments_delegatesToEngine() {
        bean.searchAlignments();

        verify(candidatAutoAlignmentEngine).searchAlignments();
    }

    @Test
    void getUriAndOptions_delegatesWithResolvedThesaurusId() throws Exception {
        when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");
        var alignment = new NodeAlignment();

        bean.getUriAndOptions(alignment);

        verify(candidatAutoAlignmentEngine).getUriAndOptions(alignment, "TH1");
    }

    @Test
    void addAlignment_delegatesWithResolvedThesaurusId() {
        when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");

        bean.addAlignment("C1", 7);

        verify(candidatAutoAlignmentEngine).addAlignment("TH1", "C1", 7);
    }

    @Test
    void cancelManualAlignment_delegatesToEngine() {
        bean.cancelManualAlignment();

        verify(candidatAutoAlignmentEngine).cancelManualAlignment();
    }

    @Test
    void actionChoix_delegatesToEngine() {
        bean.actionChoix();

        verify(candidatAutoAlignmentEngine).actionChoix();
    }

    @Test
    void isNameAlignment_delegatesToEngine() {
        when(candidatAutoAlignmentEngine.isNameAlignment()).thenReturn(true);

        assertTrue(bean.isNameAlignment());
    }

    @Test
    void conceptValueForAlignment_getterAndSetterDelegateToEngine() {
        when(candidatAutoAlignmentEngine.getConceptValueForAlignment()).thenReturn("Value");

        bean.setConceptValueForAlignment("Value");
        var result = bean.getConceptValueForAlignment();

        verify(candidatAutoAlignmentEngine).setConceptValueForAlignment("Value");
        assertEquals("Value", result);
    }

    @Test
    void nom_getterAndSetterDelegateToEngine() {
        when(candidatAutoAlignmentEngine.getNom()).thenReturn("Dupont");

        bean.setNom("Dupont");

        verify(candidatAutoAlignmentEngine).setNom("Dupont");
        assertEquals("Dupont", bean.getNom());
    }

    @Test
    void prenom_getterAndSetterDelegateToEngine() {
        when(candidatAutoAlignmentEngine.getPrenom()).thenReturn("Jean");

        bean.setPrenom("Jean");

        verify(candidatAutoAlignmentEngine).setPrenom("Jean");
        assertEquals("Jean", bean.getPrenom());
    }

    @Test
    void selectedAlignement_getterAndSetterDelegateToEngine() {
        when(candidatAutoAlignmentEngine.getSelectedAlignement()).thenReturn("wikidata");

        bean.setSelectedAlignement("wikidata");

        verify(candidatAutoAlignmentEngine).setSelectedAlignement("wikidata");
        assertEquals("wikidata", bean.getSelectedAlignement());
    }

    @Test
    void alertWikidata_delegatesToEngine() {
        when(candidatAutoAlignmentEngine.getAlertWikidata()).thenReturn("alerte");

        assertEquals("alerte", bean.getAlertWikidata());
    }

    @Test
    void alignementSources_delegatesToEngine() {
        when(candidatAutoAlignmentEngine.getAlignementSources()).thenReturn(java.util.List.of());

        assertEquals(java.util.List.of(), bean.getAlignementSources());
    }

    @Test
    void isViewResult_delegatesToEngine() {
        when(candidatAutoAlignmentEngine.isViewResult()).thenReturn(true);

        assertTrue(bean.isViewResult());
    }

    @Test
    void listAlignValues_delegatesToEngine() {
        when(candidatAutoAlignmentEngine.getListAlignValues()).thenReturn(java.util.List.of());

        assertEquals(java.util.List.of(), bean.getListAlignValues());
    }

    @Test
    void alignmentTypes_delegatesToEngine() {
        when(candidatAutoAlignmentEngine.getAlignmentTypes()).thenReturn(java.util.List.of());

        assertEquals(java.util.List.of(), bean.getAlignmentTypes());
    }

    @Test
    void selectedAlignementType_getterAndSetterDelegateToEngine() {
        when(candidatAutoAlignmentEngine.getSelectedAlignementType()).thenReturn(3);

        bean.setSelectedAlignementType(3);

        verify(candidatAutoAlignmentEngine).setSelectedAlignementType(3);
        assertEquals(3, bean.getSelectedAlignementType());
    }

    @Test
    void manualAlignmentUri_getterAndSetterDelegateToEngine() {
        when(candidatAutoAlignmentEngine.getManualAlignmentUri()).thenReturn("http://x");

        bean.setManualAlignmentUri("http://x");

        verify(candidatAutoAlignmentEngine).setManualAlignmentUri("http://x");
        assertEquals("http://x", bean.getManualAlignmentUri());
    }

    @Test
    void isViewSelection_delegatesToEngine() {
        when(candidatAutoAlignmentEngine.isViewSelection()).thenReturn(true);

        assertTrue(bean.isViewSelection());
    }

    @Test
    void selectedNodeAlignment_delegatesToEngine() {
        var alignment = new NodeAlignment();
        when(candidatAutoAlignmentEngine.getSelectedNodeAlignment()).thenReturn(alignment);

        assertEquals(alignment, bean.getSelectedNodeAlignment());
    }

    @Test
    void traductionsOfAlignment_delegatesToEngine() {
        when(candidatAutoAlignmentEngine.getTraductionsOfAlignment()).thenReturn(java.util.List.of());

        assertEquals(java.util.List.of(), bean.getTraductionsOfAlignment());
    }

    @Test
    void descriptionsOfAlignment_delegatesToEngine() {
        when(candidatAutoAlignmentEngine.getDescriptionsOfAlignment()).thenReturn(java.util.List.of());

        assertEquals(java.util.List.of(), bean.getDescriptionsOfAlignment());
    }

    @Test
    void imagesOfAlignment_delegatesToEngine() {
        when(candidatAutoAlignmentEngine.getImagesOfAlignment()).thenReturn(java.util.List.of());

        assertEquals(java.util.List.of(), bean.getImagesOfAlignment());
    }
}
