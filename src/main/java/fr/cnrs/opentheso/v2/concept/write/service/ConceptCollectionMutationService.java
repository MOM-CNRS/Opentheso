package fr.cnrs.opentheso.v2.concept.write.service;

import fr.cnrs.opentheso.v2.concept.write.model.MutationResult;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddConceptToCollectionCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.RemoveConceptFromCollectionCommand;
import fr.cnrs.opentheso.v2.concept.write.persistence.ConceptCollectionWritePersistence;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConceptCollectionMutationService {

    private final ConceptCollectionWritePersistence conceptCollectionWritePersistence;

    @Transactional
    public MutationResult addToCollection(AddConceptToCollectionCommand command) {
        return conceptCollectionWritePersistence.addToCollection(command);
    }

    @Transactional
    public MutationResult removeFromCollection(RemoveConceptFromCollectionCommand command) {
        return conceptCollectionWritePersistence.removeFromCollection(command);
    }
}
