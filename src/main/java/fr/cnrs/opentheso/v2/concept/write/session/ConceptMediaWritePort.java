package fr.cnrs.opentheso.v2.concept.write.session;

import fr.cnrs.opentheso.v2.concept.write.model.MutationResult;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddConceptImageCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddExternalResourceCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteConceptImageCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteExternalResourceCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.ReplaceGpsCoordinatesCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.UpdateConceptImageCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.UpdateExternalResourceCommand;

public interface ConceptMediaWritePort {

    MutationResult replaceGpsCoordinates(ReplaceGpsCoordinatesCommand command);

    MutationResult addImage(AddConceptImageCommand command);

    MutationResult updateImage(UpdateConceptImageCommand command);

    MutationResult deleteImage(DeleteConceptImageCommand command);

    MutationResult addExternalResource(AddExternalResourceCommand command);

    MutationResult updateExternalResource(UpdateExternalResourceCommand command);

    MutationResult deleteExternalResource(DeleteExternalResourceCommand command);
}
