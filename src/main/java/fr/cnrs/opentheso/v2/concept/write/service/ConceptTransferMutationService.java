package fr.cnrs.opentheso.v2.concept.write.service;

import fr.cnrs.opentheso.v2.concept.write.model.ConceptWriteThesaurusOption;
import fr.cnrs.opentheso.v2.concept.write.model.MutationResult;
import fr.cnrs.opentheso.v2.concept.write.model.command.MoveConceptToThesaurusCommand;
import fr.cnrs.opentheso.v2.concept.write.session.ConceptTransferWritePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ConceptTransferMutationService {

    private final ConceptTransferWritePort conceptTransferWritePort;

    @Transactional
    public MutationResult moveConceptToThesaurus(MoveConceptToThesaurusCommand command) {
        return conceptTransferWritePort.moveConceptToThesaurus(command);
    }

    @Transactional(readOnly = true)
    public List<ConceptWriteThesaurusOption> listAdminThesauri(
            int userId,
            boolean superAdmin,
            String currentThesaurusId,
            String lang
    ) {
        return conceptTransferWritePort.listAdminThesauri(userId, superAdmin, currentThesaurusId, lang);
    }
}
