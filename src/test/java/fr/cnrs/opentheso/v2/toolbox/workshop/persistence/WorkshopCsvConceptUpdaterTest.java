package fr.cnrs.opentheso.v2.toolbox.workshop.persistence;

import fr.cnrs.opentheso.entites.HierarchicalRelationship;
import fr.cnrs.opentheso.entites.HierarchicalRelationshipHistorique;
import fr.cnrs.opentheso.entites.NonPreferredTerm;
import fr.cnrs.opentheso.entites.NonPreferredTermHistorique;
import fr.cnrs.opentheso.entites.PreferredTerm;
import fr.cnrs.opentheso.entites.Term;
import fr.cnrs.opentheso.entites.TermHistorique;
import fr.cnrs.opentheso.models.relations.NodeReplaceValueByValue;
import fr.cnrs.opentheso.models.skosapi.SKOSProperty;
import fr.cnrs.opentheso.repositories.AlignementRepository;
import fr.cnrs.opentheso.repositories.AlignementSourceRepository;
import fr.cnrs.opentheso.repositories.AlignementTypeRepository;
import fr.cnrs.opentheso.repositories.ConceptRepository;
import fr.cnrs.opentheso.repositories.ExternalResourcesRepository;
import fr.cnrs.opentheso.repositories.GpsRepository;
import fr.cnrs.opentheso.repositories.HierarchicalRelationshipHistoriqueRepository;
import fr.cnrs.opentheso.repositories.HierarchicalRelationshipRepository;
import fr.cnrs.opentheso.repositories.ImagesRepository;
import fr.cnrs.opentheso.repositories.NoteHistoriqueRepository;
import fr.cnrs.opentheso.repositories.NoteRepository;
import fr.cnrs.opentheso.repositories.NonPreferredTermHistoriqueRepository;
import fr.cnrs.opentheso.repositories.NonPreferredTermRepository;
import fr.cnrs.opentheso.repositories.PreferredTermRepository;
import fr.cnrs.opentheso.repositories.TermHistoriqueRepository;
import fr.cnrs.opentheso.repositories.TermRepository;
import fr.cnrs.opentheso.v2.toolbox.edition.model.ThesaurusCsvConceptLabel;
import fr.cnrs.opentheso.v2.toolbox.edition.model.ThesaurusCsvConceptObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkshopCsvConceptUpdaterTest {

    @Mock
    private PreferredTermRepository preferredTermRepository;
    @Mock
    private TermRepository termRepository;
    @Mock
    private TermHistoriqueRepository termHistoriqueRepository;
    @Mock
    private NonPreferredTermRepository nonPreferredTermRepository;
    @Mock
    private NonPreferredTermHistoriqueRepository nonPreferredTermHistoriqueRepository;
    @Mock
    private NoteRepository noteRepository;
    @Mock
    private NoteHistoriqueRepository noteHistoriqueRepository;
    @Mock
    private AlignementRepository alignementRepository;
    @Mock
    private AlignementTypeRepository alignementTypeRepository;
    @Mock
    private AlignementSourceRepository alignementSourceRepository;
    @Mock
    private ExternalResourcesRepository externalResourcesRepository;
    @Mock
    private GpsRepository gpsRepository;
    @Mock
    private ImagesRepository imagesRepository;
    @Mock
    private HierarchicalRelationshipRepository hierarchicalRelationshipRepository;
    @Mock
    private HierarchicalRelationshipHistoriqueRepository hierarchicalRelationshipHistoriqueRepository;
    @Mock
    private ConceptRepository conceptRepository;

    @InjectMocks
    private WorkshopCsvConceptUpdater updater;

    // ---- updateConceptValueByNewValue : PREF_LABEL ----

    @Test
    void updateConceptValueByNewValue_prefLabel_updatesExistingTranslation() {
        NodeReplaceValueByValue command = new NodeReplaceValueByValue();
        command.setIdConcept("C1");
        command.setSKOSProperty(SKOSProperty.PREF_LABEL);
        command.setNewValue("Nouveau Label");
        command.setIdLang("fr");

        when(preferredTermRepository.findByIdThesaurusAndIdConcept("TH1", "C1"))
                .thenReturn(Optional.of(PreferredTerm.builder().idThesaurus("TH1").idConcept("C1").idTerm("T1").build()));
        Term existingTerm = Term.builder().idTerm("T1").idThesaurus("TH1").lang("fr").lexicalValue("Ancien Label").build();
        when(termRepository.findByIdTermAndIdThesaurusAndLang("T1", "TH1", "fr"))
                .thenReturn(Optional.of(existingTerm));

        boolean result = updater.updateConceptValueByNewValue("TH1", command, 7);

        assertTrue(result);
        verify(termRepository).save(existingTerm);
        assertEquals("Nouveau Label", existingTerm.getLexicalValue());
        assertEquals(7, existingTerm.getContributor());

        ArgumentCaptor<TermHistorique> historiqueCaptor = ArgumentCaptor.forClass(TermHistorique.class);
        verify(termHistoriqueRepository).save(historiqueCaptor.capture());
        TermHistorique historique = historiqueCaptor.getValue();
        assertEquals("UPDATE", historique.getAction());
        assertEquals("Nouveau Label", historique.getLexicalValue());
        assertEquals("T1", historique.getIdTerm());
        assertEquals("TH1", historique.getIdThesaurus());
        assertTrue(updater.getMessage().isEmpty());
    }

    @Test
    void updateConceptValueByNewValue_prefLabel_addsNewTranslation_whenLangNotYetPresent() {
        NodeReplaceValueByValue command = new NodeReplaceValueByValue();
        command.setIdConcept("C1");
        command.setSKOSProperty(SKOSProperty.PREF_LABEL);
        command.setNewValue("New Label");
        command.setIdLang("en");

        when(preferredTermRepository.findByIdThesaurusAndIdConcept("TH1", "C1"))
                .thenReturn(Optional.of(PreferredTerm.builder().idThesaurus("TH1").idConcept("C1").idTerm("T1").build()));
        when(termRepository.findByIdTermAndIdThesaurusAndLang("T1", "TH1", "en"))
                .thenReturn(Optional.empty());
        when(termRepository.save(any(Term.class))).thenAnswer(invocation -> invocation.getArgument(0));

        boolean result = updater.updateConceptValueByNewValue("TH1", command, 7);

        assertTrue(result);
        ArgumentCaptor<Term> termCaptor = ArgumentCaptor.forClass(Term.class);
        verify(termRepository).save(termCaptor.capture());
        Term saved = termCaptor.getValue();
        assertEquals("T1", saved.getIdTerm());
        assertEquals("New Label", saved.getLexicalValue());
        assertEquals("en", saved.getLang());
        assertEquals("TH1", saved.getIdThesaurus());

        ArgumentCaptor<TermHistorique> historiqueCaptor = ArgumentCaptor.forClass(TermHistorique.class);
        verify(termHistoriqueRepository).save(historiqueCaptor.capture());
        assertEquals("New", historiqueCaptor.getValue().getAction());
        assertEquals("New Label", historiqueCaptor.getValue().getLexicalValue());
    }

    @Test
    void updateConceptValueByNewValue_prefLabel_missingConceptId_recordsErrorWithoutTouchingRepositories() {
        NodeReplaceValueByValue command = new NodeReplaceValueByValue();
        command.setIdConcept("");
        command.setSKOSProperty(SKOSProperty.PREF_LABEL);
        command.setNewValue("New Label");
        command.setIdLang("fr");

        boolean result = updater.updateConceptValueByNewValue("TH1", command, 7);

        assertTrue(result);
        verify(preferredTermRepository, never()).findByIdThesaurusAndIdConcept(any(), any());
        verify(termRepository, never()).save(any());
        assertTrue(updater.getMessage().contains("concept sans identifiant"));
    }

    // ---- updateConceptValueByNewValue : ALT_LABEL ----

    @Test
    void updateConceptValueByNewValue_altLabel_renamesExistingSynonym() {
        NodeReplaceValueByValue command = new NodeReplaceValueByValue();
        command.setIdConcept("C1");
        command.setSKOSProperty(SKOSProperty.ALT_LABEL);
        command.setOldValue("AncienSyn");
        command.setNewValue("NouveauSyn");
        command.setIdLang("fr");

        when(preferredTermRepository.findByIdThesaurusAndIdConcept("TH1", "C1"))
                .thenReturn(Optional.of(PreferredTerm.builder().idThesaurus("TH1").idConcept("C1").idTerm("T1").build()));
        NonPreferredTerm existingSynonym = NonPreferredTerm.builder()
                .idTerm("T1").lexicalValue("AncienSyn").lang("fr").idThesaurus("TH1").hiden(false).build();
        when(nonPreferredTermRepository.findByIdTermAndLexicalValueAndLangAndIdThesaurus("T1", "AncienSyn", "fr", "TH1"))
                .thenReturn(Optional.of(existingSynonym));

        boolean result = updater.updateConceptValueByNewValue("TH1", command, 7);

        assertTrue(result);
        verify(nonPreferredTermRepository).save(existingSynonym);
        assertEquals("NouveauSyn", existingSynonym.getLexicalValue());

        ArgumentCaptor<NonPreferredTermHistorique> historiqueCaptor = ArgumentCaptor.forClass(NonPreferredTermHistorique.class);
        verify(nonPreferredTermHistoriqueRepository).save(historiqueCaptor.capture());
        assertEquals("update", historiqueCaptor.getValue().getAction());
        assertEquals("NouveauSyn", historiqueCaptor.getValue().getLexicalValue());
    }

    @Test
    void updateConceptValueByNewValue_altLabel_addsNewSynonym_whenNoOldValueProvided() {
        NodeReplaceValueByValue command = new NodeReplaceValueByValue();
        command.setIdConcept("C1");
        command.setSKOSProperty(SKOSProperty.ALT_LABEL);
        command.setOldValue("");
        command.setNewValue("NouveauSyn");
        command.setIdLang("fr");

        when(preferredTermRepository.findByIdThesaurusAndIdConcept("TH1", "C1"))
                .thenReturn(Optional.of(PreferredTerm.builder().idThesaurus("TH1").idConcept("C1").idTerm("T1").build()));
        when(nonPreferredTermRepository.isAltLabelExist("NouveauSyn", "TH1", "fr")).thenReturn(false);

        boolean result = updater.updateConceptValueByNewValue("TH1", command, 7);

        assertTrue(result);
        ArgumentCaptor<NonPreferredTerm> captor = ArgumentCaptor.forClass(NonPreferredTerm.class);
        verify(nonPreferredTermRepository).save(captor.capture());
        NonPreferredTerm saved = captor.getValue();
        assertEquals("T1", saved.getIdTerm());
        assertEquals("NouveauSyn", saved.getLexicalValue());
        assertEquals("fr", saved.getLang());
        assertFalse(saved.isHiden());

        verify(nonPreferredTermHistoriqueRepository).save(any(NonPreferredTermHistorique.class));
    }

    // ---- updateConceptValueByNewValue : BROADER ----

    @Test
    void updateConceptValueByNewValue_broader_replacesParentRelation() {
        NodeReplaceValueByValue command = new NodeReplaceValueByValue();
        command.setIdConcept("C1");
        command.setSKOSProperty(SKOSProperty.BROADER);
        command.setOldValue("Bold");
        command.setNewValue("Bnew");

        boolean result = updater.updateConceptValueByNewValue("TH1", command, 7);

        assertTrue(result);
        verify(hierarchicalRelationshipRepository).deleteAllByIdThesaurusAndIdConcept1AndIdConcept2AndRole(
                "TH1", "C1", "Bold", "BT");
        verify(hierarchicalRelationshipRepository).deleteAllByIdThesaurusAndIdConcept1AndIdConcept2AndRole(
                "TH1", "Bold", "C1", "NT");

        ArgumentCaptor<HierarchicalRelationship> relationCaptor = ArgumentCaptor.forClass(HierarchicalRelationship.class);
        verify(hierarchicalRelationshipRepository, times(2)).save(relationCaptor.capture());
        List<HierarchicalRelationship> savedRelations = relationCaptor.getAllValues();
        assertEquals("C1", savedRelations.get(0).getIdConcept1());
        assertEquals("Bnew", savedRelations.get(0).getIdConcept2());
        assertEquals("BT", savedRelations.get(0).getRole());
        assertEquals("Bnew", savedRelations.get(1).getIdConcept1());
        assertEquals("C1", savedRelations.get(1).getIdConcept2());
        assertEquals("NT", savedRelations.get(1).getRole());

        ArgumentCaptor<HierarchicalRelationshipHistorique> historiqueCaptor =
                ArgumentCaptor.forClass(HierarchicalRelationshipHistorique.class);
        verify(hierarchicalRelationshipHistoriqueRepository, times(2)).save(historiqueCaptor.capture());
        assertEquals("DEL", historiqueCaptor.getAllValues().get(0).getAction());
        assertEquals("ADD", historiqueCaptor.getAllValues().get(1).getAction());

        verify(conceptRepository).updateTopConcept("C1", "TH1", false);
    }

    // ---- updateConcept ----

    @Test
    void updateConcept_withOnlyEmptyCollections_returnsTrueWithoutAnyWrite() {
        ThesaurusCsvConceptObject conceptObject = new ThesaurusCsvConceptObject();
        conceptObject.setIdConcept("C1");

        when(preferredTermRepository.findByIdThesaurusAndIdConcept("TH1", "C1"))
                .thenReturn(Optional.of(PreferredTerm.builder().idThesaurus("TH1").idConcept("C1").idTerm("T1").build()));

        boolean result = updater.updateConcept("TH1", conceptObject, 7);

        assertTrue(result);
        verify(termRepository, never()).save(any());
        verify(nonPreferredTermRepository, never()).save(any());
        verify(noteRepository, never()).save(any());
        verify(alignementRepository, never()).save(any());
        verify(gpsRepository, never()).save(any());
        verify(imagesRepository, never()).save(any());
        verify(externalResourcesRepository, never()).save(any());
        verify(hierarchicalRelationshipRepository, never()).save(any());
    }

    @Test
    void updateConcept_addsPreferredLabelTranslation_forNewLanguage() {
        ThesaurusCsvConceptObject conceptObject = new ThesaurusCsvConceptObject();
        conceptObject.setIdConcept("C1");
        ThesaurusCsvConceptLabel newLabel = new ThesaurusCsvConceptLabel();
        newLabel.setLabel("Label EN");
        newLabel.setLang("en");
        conceptObject.getPrefLabels().add(newLabel);

        when(preferredTermRepository.findByIdThesaurusAndIdConcept("TH1", "C1"))
                .thenReturn(Optional.of(PreferredTerm.builder().idThesaurus("TH1").idConcept("C1").idTerm("T1").build()));
        when(termRepository.findByIdTermAndIdThesaurusAndLang("T1", "TH1", "en"))
                .thenReturn(Optional.empty());
        when(termRepository.save(any(Term.class))).thenAnswer(invocation -> invocation.getArgument(0));

        boolean result = updater.updateConcept("TH1", conceptObject, 7);

        assertTrue(result);
        ArgumentCaptor<Term> termCaptor = ArgumentCaptor.forClass(Term.class);
        verify(termRepository).save(termCaptor.capture());
        Term saved = termCaptor.getValue();
        assertEquals("T1", saved.getIdTerm());
        assertEquals("Label EN", saved.getLexicalValue());
        assertEquals("en", saved.getLang());
        assertEquals("TH1", saved.getIdThesaurus());
        assertEquals("", saved.getSource());
        assertEquals(7, saved.getContributor());

        ArgumentCaptor<TermHistorique> historiqueCaptor = ArgumentCaptor.forClass(TermHistorique.class);
        verify(termHistoriqueRepository).save(historiqueCaptor.capture());
        assertEquals("New", historiqueCaptor.getValue().getAction());
        assertEquals("Label EN", historiqueCaptor.getValue().getLexicalValue());

        verify(nonPreferredTermRepository, never()).save(any());
        verify(noteRepository, never()).save(any());
    }
}
