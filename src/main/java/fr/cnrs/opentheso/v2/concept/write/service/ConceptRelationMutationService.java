package fr.cnrs.opentheso.v2.concept.write.service;

import fr.cnrs.opentheso.v2.concept.write.model.ConceptWriteNtRelationType;
import fr.cnrs.opentheso.v2.concept.write.model.MutationResult;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddBroaderRelationCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddCustomRelationCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddNarrowerRelationCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddRelatedRelationCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.ApplyNarrowerRelationToBranchCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteBroaderRelationCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteCustomRelationCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteNarrowerRelationCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteRelatedRelationCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.UpdateNarrowerRelationTypeCommand;
import fr.cnrs.opentheso.v2.concept.write.persistence.ConceptRelationNativeWriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ConceptRelationMutationService {

    private final ConceptRelationNativeWriteService conceptRelationNativeWriteService;
    private final ConceptWriteMetadataService conceptWriteMetadataService;

    @Transactional
    public MutationResult addBroaderRelation(AddBroaderRelationCommand command) {
        return conceptRelationNativeWriteService.addBroaderRelation(command);
    }

    @Transactional
    public MutationResult addNarrowerRelation(AddNarrowerRelationCommand command) {
        return conceptRelationNativeWriteService.addNarrowerRelation(command);
    }

    @Transactional
    public MutationResult deleteBroaderRelation(DeleteBroaderRelationCommand command) {
        return conceptRelationNativeWriteService.deleteBroaderRelation(command);
    }

    @Transactional
    public MutationResult deleteNarrowerRelation(DeleteNarrowerRelationCommand command) {
        return conceptRelationNativeWriteService.deleteNarrowerRelation(command);
    }

    @Transactional
    public MutationResult updateNarrowerRelationType(UpdateNarrowerRelationTypeCommand command) {
        return conceptRelationNativeWriteService.updateNarrowerRelationType(command);
    }

    @Transactional
    public MutationResult applyNarrowerRelationToBranch(ApplyNarrowerRelationToBranchCommand command) {
        return conceptRelationNativeWriteService.applyNarrowerRelationToBranch(command);
    }

    @Transactional
    public MutationResult addRelatedRelation(AddRelatedRelationCommand command) {
        return conceptRelationNativeWriteService.addRelatedRelation(command);
    }

    @Transactional
    public MutationResult deleteRelatedRelation(DeleteRelatedRelationCommand command) {
        return conceptRelationNativeWriteService.deleteRelatedRelation(command);
    }

    @Transactional
    public MutationResult addCustomRelation(AddCustomRelationCommand command) {
        return conceptRelationNativeWriteService.addCustomRelation(command);
    }

    @Transactional
    public MutationResult deleteCustomRelation(DeleteCustomRelationCommand command) {
        return conceptRelationNativeWriteService.deleteCustomRelation(command);
    }

    @Transactional(readOnly = true)
    public List<ConceptWriteNtRelationType> listNtRelationTypes() {
        return conceptWriteMetadataService.listNtRelationTypes();
    }
}
