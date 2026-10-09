package fr.cnrs.opentheso.v2.concept.write.service;

import fr.cnrs.opentheso.v2.concept.write.model.ConceptWriteThesaurusOption;
import fr.cnrs.opentheso.v2.concept.write.model.MutationResult;
import fr.cnrs.opentheso.v2.concept.write.model.command.MoveConceptToThesaurusCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.MoveConceptsToThesaurusCommand;
import fr.cnrs.opentheso.v2.concept.write.persistence.BranchConceptSupport;
import fr.cnrs.opentheso.v2.concept.write.persistence.ConceptTransferWritePersistence;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ConceptTransferMutationService {

    private final ConceptTransferWritePersistence conceptTransferWritePersistence;
    private final BranchConceptSupport branchConceptSupport;

    @Transactional
    public MutationResult moveConceptToThesaurus(MoveConceptToThesaurusCommand command) {
        return conceptTransferWritePersistence.moveConceptToThesaurus(command);
    }

    @Transactional
    public MutationResult moveConceptsToThesaurus(MoveConceptsToThesaurusCommand command) {
        List<String> conceptIds = normalizeIds(command == null ? null : command.conceptIds());
        if (conceptIds.isEmpty()) {
            return MutationResult.validationError("Aucun concept à déplacer.");
        }
        if (command == null || StringUtils.isAnyBlank(command.sourceThesaurusId(), command.targetThesaurusId())) {
            return MutationResult.validationError("Choisissez un thésaurus de destination");
        }
        if (Strings.CI.equals(command.sourceThesaurusId(), command.targetThesaurusId())) {
            return MutationResult.validationError("Le thésaurus cible doit être différent du thésaurus source.");
        }
        List<String> heads = selectTransferHeads(command.sourceThesaurusId(), conceptIds);
        if (heads.isEmpty()) {
            return MutationResult.validationError("Aucun concept à déplacer.");
        }
        String parentId = StringUtils.trimToNull(command.parentConceptId());
        int moved = 0;
        for (String head : heads) {
            List<String> branch = branchConceptSupport.collectBranchConceptIds(
                    command.sourceThesaurusId(), head);
            if (branch.isEmpty()) {
                branch = List.of(head);
            }
            MutationResult result = conceptTransferWritePersistence.moveConceptToThesaurus(
                    new MoveConceptToThesaurusCommand(
                            command.sourceThesaurusId(),
                            command.targetThesaurusId(),
                            head,
                            branch,
                            command.lang(),
                            command.userId(),
                            command.contributorName(),
                            parentId
                    ));
            if (result == null || !result.success()) {
                return result != null ? result : MutationResult.validationError("Le déplacement a échoué");
            }
            moved += 1;
        }
        String s = moved > 1 ? "s" : "";
        return MutationResult.ok(moved + " concept" + s + " déplacé" + s);
    }

    private List<String> selectTransferHeads(String thesaurusId, List<String> conceptIds) {
        List<String> heads = new ArrayList<>();
        for (String conceptId : conceptIds) {
            boolean descendantOfSelection = false;
            for (String other : conceptIds) {
                if (other.equals(conceptId)) {
                    continue;
                }
                if (branchConceptSupport.collectBranchConceptIds(thesaurusId, other).contains(conceptId)) {
                    descendantOfSelection = true;
                    break;
                }
            }
            if (!descendantOfSelection) {
                heads.add(conceptId);
            }
        }
        return heads;
    }

    private static List<String> normalizeIds(List<String> raw) {
        if (raw == null) {
            return List.of();
        }
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        for (String part : raw) {
            if (StringUtils.isNotBlank(part)) {
                ids.add(part.trim());
            }
        }
        return List.copyOf(ids);
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
