package fr.cnrs.opentheso.v2.concept.identifier;

import fr.cnrs.opentheso.repositories.PreferencesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ConceptIdentifierAssignmentService {

    private final PreferencesRepository preferencesRepository;
    private final ConceptArkWriteService conceptArkWriteService;
    private final ConceptHandleWriteService conceptHandleWriteService;

    public void assignIdentifiers(String thesaurusId, String conceptId, String lang) {
        var preferences = preferencesRepository.findByIdThesaurus(thesaurusId).orElse(null);
        if (preferences == null) {
            return;
        }
        if (preferences.isUseHandle() && !conceptHandleWriteService.assignHandleOnCreation(conceptId, thesaurusId)) {
            throw new IllegalStateException("La création du Handle a échoué");
        }
        conceptArkWriteService.assignIdentifiersOnCreation(thesaurusId, conceptId, lang);
    }
}
