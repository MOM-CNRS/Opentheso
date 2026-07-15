package fr.cnrs.opentheso.v2.concept.write.session;

import fr.cnrs.opentheso.v2.concept.write.model.MutationResult;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddConceptToCollectionCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.RemoveConceptFromCollectionCommand;

public interface ConceptCollectionWritePort {

    MutationResult addToCollection(AddConceptToCollectionCommand command);

    MutationResult removeFromCollection(RemoveConceptFromCollectionCommand command);
}
