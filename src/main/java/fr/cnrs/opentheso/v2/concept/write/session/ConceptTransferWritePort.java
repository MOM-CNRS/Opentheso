package fr.cnrs.opentheso.v2.concept.write.session;

import fr.cnrs.opentheso.v2.concept.write.model.ConceptWriteThesaurusOption;
import fr.cnrs.opentheso.v2.concept.write.model.MutationResult;
import fr.cnrs.opentheso.v2.concept.write.model.command.MoveConceptToThesaurusCommand;

import java.util.List;

public interface ConceptTransferWritePort {

    MutationResult moveConceptToThesaurus(MoveConceptToThesaurusCommand command);

    List<ConceptWriteThesaurusOption> listAdminThesauri(int userId, boolean superAdmin, String currentThesaurusId, String lang);
}
