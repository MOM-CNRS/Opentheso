package fr.cnrs.opentheso.v2.concept.write.service;

import fr.cnrs.opentheso.v2.concept.write.model.MutationResult;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddChildConceptCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddReplacedByCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddTopConceptCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.ApproveConceptCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteConceptCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteReplacedByCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeprecateConceptCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.RenamePreferredLabelCommand;
import fr.cnrs.opentheso.v2.concept.write.session.ConceptWritePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConceptLifecycleMutationService {

    private final ConceptWritePort conceptWritePort;

    @Transactional
    public MutationResult renamePreferredLabel(RenamePreferredLabelCommand command) {
        return conceptWritePort.renamePreferredLabel(command);
    }

    @Transactional
    public MutationResult addChildConcept(AddChildConceptCommand command) {
        return conceptWritePort.addChildConcept(command);
    }

    @Transactional
    public MutationResult addTopConcept(AddTopConceptCommand command) {
        return conceptWritePort.addTopConcept(command);
    }

    @Transactional
    public MutationResult deleteConcept(DeleteConceptCommand command) {
        return conceptWritePort.deleteConcept(command);
    }

    @Transactional
    public MutationResult deprecateConcept(DeprecateConceptCommand command) {
        return conceptWritePort.deprecateConcept(command);
    }

    @Transactional
    public MutationResult approveConcept(ApproveConceptCommand command) {
        return conceptWritePort.approveConcept(command);
    }

    @Transactional
    public MutationResult addReplacedBy(AddReplacedByCommand command) {
        return conceptWritePort.addReplacedBy(command);
    }

    @Transactional
    public MutationResult deleteReplacedBy(DeleteReplacedByCommand command) {
        return conceptWritePort.deleteReplacedBy(command);
    }
}
