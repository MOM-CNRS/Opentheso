package fr.cnrs.opentheso.v2.toolbox.persistence;

import fr.cnrs.opentheso.models.group.NodeGroup;
import fr.cnrs.opentheso.repositories.ConceptGroupLabelRepository;
import fr.cnrs.opentheso.repositories.ConceptGroupRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ToolboxExportPersistence {

    private final ConceptGroupRepository conceptGroupRepository;
    private final ConceptGroupLabelRepository conceptGroupLabelRepository;
    private final ToolboxPreferencePersistence toolboxPreferencePersistence;

    public List<NodeGroup> loadConceptGroups(String thesaurusId) {
        String workLang = toolboxPreferencePersistence.getWorkLanguage(thesaurusId);
        var groups = conceptGroupRepository.findAllByIdThesaurus(thesaurusId).stream()
                .map(group -> {
                    var labels = conceptGroupLabelRepository.findAllByIdThesaurusAndIdGroupAndLang(
                            thesaurusId, group.getIdGroup(), workLang);
                    return NodeGroup.builder()
                            .groupPrivate(group.isPrivate())
                            .conceptGroup(group)
                            .lexicalValue(CollectionUtils.isNotEmpty(labels) ? labels.get(0).getLexicalValue() : group.getIdGroup())
                            .idLang(workLang)
                            .build();
                })
                .toList();
        var sorted = new java.util.ArrayList<>(groups);
        sorted.sort(Comparator.comparing(NodeGroup::getLexicalValue, String.CASE_INSENSITIVE_ORDER));
        return sorted;
    }
}
