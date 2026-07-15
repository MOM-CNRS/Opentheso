package fr.cnrs.opentheso.v2.toolbox.edition.persistence;

import fr.cnrs.opentheso.models.terms.ConceptPreferredTermLookup;
import fr.cnrs.opentheso.models.terms.Term;
import fr.cnrs.opentheso.repositories.TermRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ThesaurusEditionPdfTermLookup implements ConceptPreferredTermLookup {

    private final TermRepository termRepository;

    @Override
    public Term getThisTerm(String idConcept, String idThesaurus, String idLang) {
        if (!termRepository.existsTranslationForConcept(idConcept, idThesaurus, idLang)) {
            return null;
        }
        var result = termRepository.getPreferredTermWithConceptInfo(idConcept, idThesaurus, idLang);
        if (result instanceof Object[] row) {
            return Term.builder()
                    .idTerm((String) row[0])
                    .idConcept((String) row[1])
                    .lexicalValue((String) row[2])
                    .lang((String) row[3])
                    .idThesaurus((String) row[4])
                    .build();
        }
        return null;
    }
}
