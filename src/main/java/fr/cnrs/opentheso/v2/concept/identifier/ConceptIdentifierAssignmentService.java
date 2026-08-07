package fr.cnrs.opentheso.v2.concept.identifier;

import fr.cnrs.opentheso.repositories.PreferencesRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
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
            String detail = StringUtils.defaultIfBlank(
                    conceptHandleWriteService.lastErrorMessage(),
                    "échec de communication avec le serveur Handle");
            throw new IllegalStateException("La création du Handle a échoué : " + detail);
        }
        try {
            conceptArkWriteService.assignIdentifiersOnCreation(thesaurusId, conceptId, lang);
        } catch (IllegalStateException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            String detail = StringUtils.defaultIfBlank(exception.getMessage(), exception.getClass().getSimpleName());
            throw new IllegalStateException("La création de l'identifiant ARK a échoué : " + detail, exception);
        }
    }
}
