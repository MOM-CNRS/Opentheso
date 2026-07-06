package fr.cnrs.opentheso.v2.concept.write.session;

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

/**
 * Port d'écriture concept — implémenté par un adaptateur legacy ou une persistance v2 native.
 */
public interface ConceptWritePort {

    MutationResult renamePreferredLabel(RenamePreferredLabelCommand command);

    MutationResult addChildConcept(AddChildConceptCommand command);

    MutationResult addTopConcept(AddTopConceptCommand command);

    MutationResult deleteConcept(DeleteConceptCommand command);

    MutationResult addBroaderRelation(AddBroaderRelationCommand command);

    MutationResult addNarrowerRelation(AddNarrowerRelationCommand command);

    MutationResult deleteBroaderRelation(DeleteBroaderRelationCommand command);

    MutationResult deleteNarrowerRelation(DeleteNarrowerRelationCommand command);

    MutationResult updateNarrowerRelationType(UpdateNarrowerRelationTypeCommand command);

    MutationResult applyNarrowerRelationToBranch(ApplyNarrowerRelationToBranchCommand command);

    MutationResult addRelatedRelation(AddRelatedRelationCommand command);

    MutationResult deleteRelatedRelation(DeleteRelatedRelationCommand command);

    MutationResult addSynonym(AddSynonymCommand command);

    MutationResult updateSynonym(UpdateSynonymCommand command);

    MutationResult deleteSynonym(DeleteSynonymCommand command);

    MutationResult addTranslation(AddTranslationCommand command);

    MutationResult updateTranslation(UpdateTranslationCommand command);

    MutationResult deleteTranslation(DeleteTranslationCommand command);

    MutationResult upsertNote(UpsertNoteCommand command);

    MutationResult deleteNote(DeleteNoteCommand command);

    MutationResult deprecateConcept(DeprecateConceptCommand command);

    MutationResult approveConcept(ApproveConceptCommand command);

    MutationResult addReplacedBy(AddReplacedByCommand command);

    MutationResult deleteReplacedBy(DeleteReplacedByCommand command);

    MutationResult addCustomRelation(AddCustomRelationCommand command);

    MutationResult deleteCustomRelation(DeleteCustomRelationCommand command);
}
