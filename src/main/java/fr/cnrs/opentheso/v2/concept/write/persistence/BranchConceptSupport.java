package fr.cnrs.opentheso.v2.concept.write.persistence;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class BranchConceptSupport {

    private final ConceptRelationWriteRepository conceptRelationWriteRepository;

    public List<String> collectBranchConceptIds(String thesaurusId, String rootConceptId) {
        if (StringUtils.isAnyBlank(thesaurusId, rootConceptId)) {
            return List.of();
        }
        List<String> conceptIds = new ArrayList<>();
        collectRecursive(thesaurusId, rootConceptId, conceptIds);
        return conceptIds;
    }

    private void collectRecursive(String thesaurusId, String conceptId, List<String> conceptIds) {
        if (conceptIds.contains(conceptId)) {
            return;
        }
        conceptIds.add(conceptId);
        for (String childConceptId : conceptRelationWriteRepository.listNarrowerChildConceptIds(
                conceptId, thesaurusId)) {
            collectRecursive(thesaurusId, childConceptId, conceptIds);
        }
    }
}
