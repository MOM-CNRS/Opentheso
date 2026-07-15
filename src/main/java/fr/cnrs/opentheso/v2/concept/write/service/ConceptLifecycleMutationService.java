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
import fr.cnrs.opentheso.v2.concept.write.persistence.ConceptLifecycleNativeWriteService;
import fr.cnrs.opentheso.v2.concept.write.persistence.ConceptStructureNativeWriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConceptLifecycleMutationService {

    private final ConceptLifecycleNativeWriteService conceptLifecycleNativeWriteService;
    private final ConceptStructureNativeWriteService conceptStructureNativeWriteService;

    @Transactional
    public MutationResult renamePreferredLabel(RenamePreferredLabelCommand command) {
        return conceptLifecycleNativeWriteService.renamePreferredLabel(command);
    }

    @Transactional
    public MutationResult addChildConcept(AddChildConceptCommand command) {
        return conceptStructureNativeWriteService.addChildConcept(command);
    }

    @Transactional
    public MutationResult addTopConcept(AddTopConceptCommand command) {
        return conceptStructureNativeWriteService.addTopConcept(command);
    }

    @Transactional
    public MutationResult deleteConcept(DeleteConceptCommand command) {
        return conceptStructureNativeWriteService.deleteConcept(command);
    }

    @Transactional
    public MutationResult deprecateConcept(DeprecateConceptCommand command) {
        return conceptLifecycleNativeWriteService.deprecateConcept(command);
    }

    @Transactional
    public MutationResult approveConcept(ApproveConceptCommand command) {
        return conceptLifecycleNativeWriteService.approveConcept(command);
    }

    @Transactional
    public MutationResult addReplacedBy(AddReplacedByCommand command) {
        return conceptLifecycleNativeWriteService.addReplacedBy(command);
    }

    @Transactional
    public MutationResult deleteReplacedBy(DeleteReplacedByCommand command) {
        return conceptLifecycleNativeWriteService.deleteReplacedBy(command);
    }
}
