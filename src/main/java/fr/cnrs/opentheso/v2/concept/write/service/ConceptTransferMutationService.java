package fr.cnrs.opentheso.v2.concept.write.service;

import fr.cnrs.opentheso.v2.concept.write.model.ConceptWriteThesaurusOption;
import fr.cnrs.opentheso.v2.concept.write.model.MutationResult;
import fr.cnrs.opentheso.v2.concept.write.model.command.MoveConceptToThesaurusCommand;
import fr.cnrs.opentheso.v2.concept.write.persistence.ConceptTransferWritePersistence;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ConceptTransferMutationService {

    private final ConceptTransferWritePersistence conceptTransferWritePersistence;

    @Transactional
    public MutationResult moveConceptToThesaurus(MoveConceptToThesaurusCommand command) {
        return conceptTransferWritePersistence.moveConceptToThesaurus(command);
    }

    @Transactional(readOnly = true)
    public List<ConceptWriteThesaurusOption> listAdminThesauri(
            int userId,
            boolean superAdmin,
            String currentThesaurusId,
            String lang
    ) {
        return conceptTransferWritePersistence.listAdminThesauri(userId, superAdmin, currentThesaurusId, lang);
    }
}
