package fr.cnrs.opentheso.v2.concept.write.service;

import fr.cnrs.opentheso.v2.concept.write.model.MutationResult;
import fr.cnrs.opentheso.v2.concept.write.model.command.CopyBranchBetweenThesaurusCommand;
import fr.cnrs.opentheso.v2.concept.write.persistence.ConceptCopyBetweenThesaurusWritePersistence;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ConceptCopyBetweenThesaurusMutationService {

    private final ConceptCopyBetweenThesaurusWritePersistence conceptCopyBetweenThesaurusWritePersistence;

    public MutationResult validateIdsAvailable(String targetThesaurusId, List<String> conceptIds) {
        return conceptCopyBetweenThesaurusWritePersistence.validateIdsAvailable(targetThesaurusId, conceptIds);
    }

    public MutationResult copyBranch(CopyBranchBetweenThesaurusCommand command) {
        return conceptCopyBetweenThesaurusWritePersistence.copyBranch(command);
    }
}
