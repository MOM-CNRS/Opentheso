package fr.cnrs.opentheso.services;

import fr.cnrs.opentheso.concept.history.ConceptChangeLog;
import fr.cnrs.opentheso.entites.NonPreferredTerm;
import fr.cnrs.opentheso.models.terms.NodeEM;
import fr.cnrs.opentheso.models.terms.Term;
import fr.cnrs.opentheso.repositories.NonPreferredTermRepository;
import fr.cnrs.opentheso.ws.openapi.exception.LabelAlreadyExistsException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


@Slf4j
@Service
@RequiredArgsConstructor
public class NonPreferredTermService {

    private final NonPreferredTermRepository nonPreferredTermRepository;

    /** Journal unique des modifications (concept_change_log), qui remplace les anciennes tables *_historique. */
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private ConceptChangeLog changeLog;


    @Transactional
    public boolean addNonPreferredTerm(Term term, int idUser) {

        log.debug("Enregistrement du nouveau synonyme");
        if(nonPreferredTermRepository.isAltLabelExist(term.getLexicalValue().trim(),
                term.getIdThesaurus(), term.getLang())){
            return false;
        }
        nonPreferredTermRepository.save(NonPreferredTerm.builder()
                .idTerm(term.getIdTerm())
                .lexicalValue(term.getLexicalValue())
                .lang(term.getLang())
                .idThesaurus(term.getIdThesaurus())
                .source(term.getSource())
                .status(term.getStatus())
                .hiden(term.isHidden())
                .created(new Date())
                .modified(new Date())
                .build());

        saveTrace(term.getIdTerm(), term.getLexicalValue(), term.getIdTerm(), term.getLang(), idUser, term.isHidden(), "ADD");
        return true;
    }

    public void deleteNonPreferredTerm(String idTerm, String idLang, String lexicalValue, String idTheso, int idUser) {

        log.debug("Suppression du synonyme du term " + idTerm);
        nonPreferredTermRepository.deleteByIdThesaurusAndIdTermAndLexicalValueAndLang(idTheso, idTerm, lexicalValue, idLang);

        saveTrace(idTerm, lexicalValue, idTheso, idLang, idUser, false, "delete");
    }

    public List<String> getNonPreferredTermValue(String idThesaurus, String idTerm, String lang) {
        var nonPreferredTerms = nonPreferredTermRepository.findAllByIdThesaurusAndIdTermAndLangAndHidenNot(idThesaurus, idTerm, lang, true);
        if (CollectionUtils.isNotEmpty(nonPreferredTerms)) {
            return nonPreferredTerms.stream().map(NonPreferredTerm::getLexicalValue).toList();
        } else {
            return List.of();
        }
    }

    public boolean updateNonPreferredTerm(String oldValue, String newValue, String idTerm, String idLang, String idTheso, boolean isHidden, int idUser) {

        log.debug("Début du mise à jour du synonyme " + idTerm);
        var nonPreferredTerm = nonPreferredTermRepository.findByIdTermAndLexicalValueAndLangAndIdThesaurus(idTerm, oldValue, idLang, idTheso);

        if (nonPreferredTerm.isPresent()) {
            log.debug("Mise à jour du synonyme");
            nonPreferredTerm.get().setLexicalValue(newValue);
            nonPreferredTerm.get().setHiden(isHidden);
            nonPreferredTerm.get().setModified(new Date());
            nonPreferredTermRepository.save(nonPreferredTerm.get());

            saveTrace(idTerm, newValue, idTheso, idLang, idUser, isHidden, "update");
            return true;
        }
        return false;
    }


    public boolean updateStatusNonPreferredTerm(String idTerm, String value, String idLang, String idTheso, boolean isHidden, int idUser) {
        log.debug("Début du mise à jour du status du synonyme " + idTerm);
        var nonPreferredTerm = nonPreferredTermRepository.findByIdTermAndLexicalValueAndLangAndIdThesaurus(idTerm, value, idLang, idTheso);

        if (nonPreferredTerm.isPresent()) {
            log.debug("Mise à jour du status synonyme");
            nonPreferredTerm.get().setHiden(isHidden);
            nonPreferredTerm.get().setModified(new Date());
            nonPreferredTermRepository.save(nonPreferredTerm.get());

            saveTrace(idTerm, value, idTheso, idLang, idUser, isHidden, "update");
            return true;
        }
        return false;
    }

    private void saveTrace(String idTerm, String lexicalValue, String idThesaurus, String idLang, int idUser, boolean isHidden, String action) {
        log.debug("Enregistrement du trace de l'action du mise à jour");
        String normalized = ConceptChangeLog.normalizeActionPublic(action);
        boolean delete = "DELETE".equals(normalized);
        journalForTerm(idTerm, new ConceptChangeLog.Change(idThesaurus, null, ConceptChangeLog.SYNONYM, idTerm, idLang,
                normalized, delete ? lexicalValue : null, delete ? null : lexicalValue,
                isHidden ? "hidden" : null, idUser, "legacy"));
    }

    public List<NodeEM> getAllNonPreferredTerms(String idConcept, String idThesaurus) {
        try {
            return nonPreferredTermRepository.findAllNodeEMByConcept(idConcept, idThesaurus);
        } catch (Exception e) {
            log.error("Error while getting NonPreferredTerms for concept: " + idConcept, e);
            return new ArrayList<>();
        }
    }

    public List<NodeEM> getNonPreferredTerms(String idConcept, String idThesaurus, String idLang) {
        try {
            return nonPreferredTermRepository.findNodeEMByConceptAndLang(idConcept, idThesaurus, idLang);
        } catch (Exception e) {
            log.error("Error while getting NonPreferredTerm of Term: " + idConcept, e);
            return new ArrayList<>();
        }
    }

    @Transactional
    public void deleteEMByIdTermAndLangAndLexical(String idTerm, String idThesaurus, String idLang, String lexicalValue) {

        log.debug("Suppression du terme id {} (langue : {} et thésaurus id {} et lexicalValue {})", idTerm, idLang, idThesaurus, lexicalValue);
        nonPreferredTermRepository.deleteByIdThesaurusAndIdTermAndLangAndLexicalValue(idThesaurus, idTerm, idLang, lexicalValue);
    }

    @Transactional
    public void deleteEMByIdTermAndLang(String idTerm, String idThesaurus, String idLang) {

        log.debug("Suppression du terme id {} (langue : {} et thésaurus id {})", idTerm, idLang, idThesaurus);
        nonPreferredTermRepository.deleteByIdThesaurusAndIdTermAndLang(idThesaurus, idTerm, idLang);
    }

    private void journal(ConceptChangeLog.Change change) {
        if (changeLog != null) {
            changeLog.record(change);
        }
    }

    private void journalForTerm(String idTerm, ConceptChangeLog.Change change) {
        if (changeLog != null) {
            changeLog.recordForTerm(idTerm, change);
        }
    }
}
