package fr.cnrs.opentheso.v2.concept.write.session;

import fr.cnrs.opentheso.v2.concept.write.model.MutationResult;
import fr.cnrs.opentheso.v2.concept.write.model.command.UpdateConceptTypeCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.UpdateNotationCommand;

public interface ConceptAttributeWritePort {

    MutationResult updateNotation(UpdateNotationCommand command);

    MutationResult updateConceptType(UpdateConceptTypeCommand command);
}
