package fr.cnrs.opentheso.v2.collection.identifier;

import fr.cnrs.opentheso.repositories.PreferencesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
        if (preferences.isUseHandle() && !preferences.isGenerateHandle()) {
            if (!collectionHandleWriteService.assignHandleOnCreation(collectionId, thesaurusId)) {
                throw new IllegalStateException("La création du Handle a échoué");
            }
        }
    }
}
