package fr.cnrs.opentheso.v2.concept.write.persistence;

import fr.cnrs.opentheso.legacybridge.LegacyConceptWriteSupport;
import fr.cnrs.opentheso.v2.concept.write.model.MutationResult;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddBroaderRelationCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddChildConceptCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddCustomRelationCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddNarrowerRelationCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddRelatedRelationCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddReplacedByCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddSynonymCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddTopConceptCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddTranslationCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.ApplyNarrowerRelationToBranchCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.ApproveConceptCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteBroaderRelationCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteConceptCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteCustomRelationCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteNarrowerRelationCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteNoteCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteRelatedRelationCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteReplacedByCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteSynonymCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteTranslationCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeprecateConceptCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.RenamePreferredLabelCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.UpdateNarrowerRelationTypeCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.UpdateSynonymCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.UpdateTranslationCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.UpsertNoteCommand;
import fr.cnrs.opentheso.v2.concept.write.session.ConceptWritePort;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Native {@link ConceptWritePort} implementation used when native persistence is enabled.
 */
@Component
@Primary
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "opentheso.concept-write", name = "native-persistence-enabled", havingValue = "true")
public class V2NativeConceptWriteSupport implements ConceptWritePort {

    private final ConceptNoteNativeWriteService conceptNoteNativeWriteService;
    private final ConceptLexicalNativeWriteService conceptLexicalNativeWriteService;
    private final ConceptLifecycleNativeWriteService conceptLifecycleNativeWriteService;
    private final ConceptRelationNativeWriteService conceptRelationNativeWriteService;
    private final ConceptStructureNativeWriteService conceptStructureNativeWriteService;

    @Override
    public MutationResult renamePreferredLabel(RenamePreferredLabelCommand command) {
        return conceptLifecycleNativeWriteService.renamePreferredLabel(command);
    }

    @Override
    public MutationResult addChildConcept(AddChildConceptCommand command) {
        return conceptStructureNativeWriteService.addChildConcept(command);
    }

    @Override
    public MutationResult addTopConcept(AddTopConceptCommand command) {
        return conceptStructureNativeWriteService.addTopConcept(command);
    }

    @Override
    public MutationResult deleteConcept(DeleteConceptCommand command) {
        return conceptStructureNativeWriteService.deleteConcept(command);
    }

    @Override
    public MutationResult deprecateConcept(DeprecateConceptCommand command) {
        return conceptLifecycleNativeWriteService.deprecateConcept(command);
    }

    @Override
    public MutationResult approveConcept(ApproveConceptCommand command) {
        return conceptLifecycleNativeWriteService.approveConcept(command);
    }

    @Override
    public MutationResult addReplacedBy(AddReplacedByCommand command) {
        return conceptLifecycleNativeWriteService.addReplacedBy(command);
    }

    @Override
    public MutationResult deleteReplacedBy(DeleteReplacedByCommand command) {
        return conceptLifecycleNativeWriteService.deleteReplacedBy(command);
    }

    @Override
    public MutationResult addBroaderRelation(AddBroaderRelationCommand command) {
        return conceptRelationNativeWriteService.addBroaderRelation(command);
    }

    @Override
    public MutationResult addNarrowerRelation(AddNarrowerRelationCommand command) {
        return conceptRelationNativeWriteService.addNarrowerRelation(command);
    }

    @Override
    public MutationResult deleteBroaderRelation(DeleteBroaderRelationCommand command) {
        return conceptRelationNativeWriteService.deleteBroaderRelation(command);
    }

    @Override
    public MutationResult deleteNarrowerRelation(DeleteNarrowerRelationCommand command) {
        return conceptRelationNativeWriteService.deleteNarrowerRelation(command);
    }

    @Override
    public MutationResult updateNarrowerRelationType(UpdateNarrowerRelationTypeCommand command) {
        return conceptRelationNativeWriteService.updateNarrowerRelationType(command);
    }

    @Override
    public MutationResult applyNarrowerRelationToBranch(ApplyNarrowerRelationToBranchCommand command) {
        return conceptRelationNativeWriteService.applyNarrowerRelationToBranch(command);
    }

    @Override
    public MutationResult addRelatedRelation(AddRelatedRelationCommand command) {
        return conceptRelationNativeWriteService.addRelatedRelation(command);
    }

    @Override
    public MutationResult deleteRelatedRelation(DeleteRelatedRelationCommand command) {
        return conceptRelationNativeWriteService.deleteRelatedRelation(command);
    }

    @Override
    public MutationResult addCustomRelation(AddCustomRelationCommand command) {
        return conceptRelationNativeWriteService.addCustomRelation(command);
    }

    @Override
    public MutationResult deleteCustomRelation(DeleteCustomRelationCommand command) {
        return conceptRelationNativeWriteService.deleteCustomRelation(command);
    }

    @Override
    public MutationResult addSynonym(AddSynonymCommand command) {
        return conceptLexicalNativeWriteService.addSynonym(command);
    }

    @Override
    public MutationResult updateSynonym(UpdateSynonymCommand command) {
        return conceptLexicalNativeWriteService.updateSynonym(command);
    }

    @Override
    public MutationResult deleteSynonym(DeleteSynonymCommand command) {
        return conceptLexicalNativeWriteService.deleteSynonym(command);
    }

    @Override
    public MutationResult addTranslation(AddTranslationCommand command) {
        return conceptLexicalNativeWriteService.addTranslation(command);
    }

    @Override
    public MutationResult updateTranslation(UpdateTranslationCommand command) {
        return conceptLexicalNativeWriteService.updateTranslation(command);
    }

    @Override
    public MutationResult deleteTranslation(DeleteTranslationCommand command) {
        return conceptLexicalNativeWriteService.deleteTranslation(command);
    }

    @Override
    public MutationResult upsertNote(UpsertNoteCommand command) {
        return conceptNoteNativeWriteService.upsertNote(command);
    }

    @Override
    public MutationResult deleteNote(DeleteNoteCommand command) {
        return conceptNoteNativeWriteService.deleteNote(command);
    }
}
