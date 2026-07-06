package fr.cnrs.opentheso.v2.concept.write.service;

import fr.cnrs.opentheso.v2.concept.write.model.MutationResult;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddConceptImageCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddExternalResourceCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteConceptImageCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteExternalResourceCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.ReplaceGpsCoordinatesCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.UpdateConceptImageCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.UpdateExternalResourceCommand;
import fr.cnrs.opentheso.v2.concept.write.session.ConceptMediaWritePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConceptMediaMutationService {

    private final ConceptMediaWritePort conceptMediaWritePort;

    @Transactional
    public MutationResult replaceGpsCoordinates(ReplaceGpsCoordinatesCommand command) {
        return conceptMediaWritePort.replaceGpsCoordinates(command);
    }

    @Transactional
    public MutationResult addImage(AddConceptImageCommand command) {
        return conceptMediaWritePort.addImage(command);
    }

    @Transactional
    public MutationResult updateImage(UpdateConceptImageCommand command) {
        return conceptMediaWritePort.updateImage(command);
    }

    @Transactional
    public MutationResult deleteImage(DeleteConceptImageCommand command) {
        return conceptMediaWritePort.deleteImage(command);
    }

    @Transactional
    public MutationResult addExternalResource(AddExternalResourceCommand command) {
        return conceptMediaWritePort.addExternalResource(command);
    }

    @Transactional
    public MutationResult updateExternalResource(UpdateExternalResourceCommand command) {
        return conceptMediaWritePort.updateExternalResource(command);
    }

    @Transactional
    public MutationResult deleteExternalResource(DeleteExternalResourceCommand command) {
        return conceptMediaWritePort.deleteExternalResource(command);
    }
}
