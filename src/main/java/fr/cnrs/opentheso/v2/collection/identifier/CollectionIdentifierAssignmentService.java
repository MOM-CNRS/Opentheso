package fr.cnrs.opentheso.v2.collection.identifier;

import fr.cnrs.opentheso.repositories.PreferencesRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CollectionIdentifierAssignmentService {

    private final PreferencesRepository preferencesRepository;
    private final CollectionArkWriteService collectionArkWriteService;
    private final CollectionHandleWriteService collectionHandleWriteService;

    public void assignOnCreation(String thesaurusId, String collectionId, String label) {
        var preferences = preferencesRepository.findByIdThesaurus(thesaurusId).orElse(null);
        if (preferences == null) {
            return;
        }
        if (preferences.isUseArk()) {
            collectionArkWriteService.assignArkOnCreation(thesaurusId, collectionId, label);
        }
        if (preferences.isUseHandle() && !preferences.isGenerateHandle()
                && !collectionHandleWriteService.assignHandleOnCreation(collectionId, thesaurusId)) {
            // Aligné sur le legacy : un échec Handle ne doit pas annuler la création de la collection.
            log.warn("La création du Handle a échoué pour la collection {} (thésaurus {})",
                    collectionId, thesaurusId);
        }
    }
}
