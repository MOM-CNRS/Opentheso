package fr.cnrs.opentheso.v2.collection.read;

import fr.cnrs.opentheso.v2.concept.mapper.ConceptMapper;
import fr.cnrs.opentheso.v2.concept.model.FacetMemberItem;
import fr.cnrs.opentheso.v2.concept.model.GroupDetailOverview;
import fr.cnrs.opentheso.v2.concept.model.GroupTranslationItem;
import fr.cnrs.opentheso.v2.shared.repository.ConceptQueryRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CollectionReadService {

    private final ConceptQueryRepository conceptQueryRepository;

    @Transactional(readOnly = true)
    public Optional<GroupDetailOverview> loadDetail(String thesaurusId, String groupId, String lang) {
        if (StringUtils.isAnyBlank(thesaurusId, groupId)) {
            return Optional.empty();
        }
        return conceptQueryRepository.findGroupHeader(groupId, thesaurusId, lang)
                .map(header -> {
                    var type = conceptQueryRepository.findGroupType(ConceptMapper.stringAt(header, 5));
                    return new GroupDetailOverview(
                            ConceptMapper.stringAt(header, 0),
                            ConceptMapper.stringAt(header, 1),
                            lang,
                            type.map(row -> ConceptMapper.stringAt(row, 0)).orElse(""),
                            type.map(row -> ConceptMapper.stringAt(row, 1)).orElse(""),
                            conceptQueryRepository.countConceptsInGroup(thesaurusId, groupId),
                            ConceptMapper.stringAt(header, 2),
                            ConceptMapper.stringAt(header, 3),
                            ConceptMapper.stringAt(header, 4),
                            conceptQueryRepository.findGroupTranslations(groupId, thesaurusId, lang).stream()
                                    .map(row -> new GroupTranslationItem(
                                            ConceptMapper.stringAt(row, 0),
                                            ConceptMapper.stringAt(row, 1)
                                    ))
                                    .toList(),
                            conceptQueryRepository.findNotesByIdentifier(groupId, thesaurusId, lang).stream()
                                    .map(ConceptMapper::toNote)
                                    .toList(),
                            loadMembers(thesaurusId, groupId, lang)
                    );
                });
    }

    private List<FacetMemberItem> loadMembers(String thesaurusId, String groupId, String lang) {
        return conceptQueryRepository.findConceptsOfGroup(groupId, thesaurusId, lang).stream()
                .map(row -> new FacetMemberItem(
                        ConceptMapper.stringAt(row, 0),
                        ConceptMapper.stringAt(row, 2)
                ))
                .toList();
    }
}
