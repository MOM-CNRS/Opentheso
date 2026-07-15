package fr.cnrs.opentheso.v2.concept.write.session;

import fr.cnrs.opentheso.v2.concept.write.model.MutationResult;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteArkCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteHandleCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.GenerateArkCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.GenerateHandleCommand;

public interface ConceptIdentifierWritePort {

    MutationResult generateArk(GenerateArkCommand command);

    MutationResult deleteArk(DeleteArkCommand command);

    MutationResult generateHandle(GenerateHandleCommand command);

    MutationResult deleteHandle(DeleteHandleCommand command);
}
