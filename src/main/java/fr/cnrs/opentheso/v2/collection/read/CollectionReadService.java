package fr.cnrs.opentheso.v2.collection.read;

import fr.cnrs.opentheso.v2.concept.mapper.ConceptMapper;
import fr.cnrs.opentheso.v2.concept.model.FacetMemberItem;
import fr.cnrs.opentheso.v2.concept.model.GroupDetailOverview;
import fr.cnrs.opentheso.v2.concept.model.GroupTranslationItem;
import fr.cnrs.opentheso.v2.shared.repository.CollectionTreeQueryRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CollectionReadService {

    private static final int MAX_MEMBER_CONCEPTS = 4_000;

    private final CollectionTreeQueryRepository collectionTreeQueryRepository;

    @Transactional(readOnly = true)
    public Optional<GroupDetailOverview> loadDetail(String thesaurusId, String groupId, String lang) {
        if (StringUtils.isAnyBlank(thesaurusId, groupId)) {
            return Optional.empty();
        }
        return collectionTreeQueryRepository.findGroupHeader(groupId, thesaurusId, lang, true)
                .map(header -> {
                    String typeCode = ConceptMapper.stringAt(header, 5);
                    var type = collectionTreeQueryRepository.findGroupType(typeCode);
                    return new GroupDetailOverview(
                            ConceptMapper.stringAt(header, 0),
                            ConceptMapper.stringAt(header, 1),
                            lang,
                            typeCode,
                            type.map(row -> ConceptMapper.stringAt(row, 0)).orElse(""),
                            type.map(row -> ConceptMapper.stringAt(row, 1)).orElse(""),
                            collectionTreeQueryRepository.countMemberConcepts(thesaurusId, groupId),
                            ConceptMapper.stringAt(header, 2),
                            ConceptMapper.stringAt(header, 3),
                            ConceptMapper.stringAt(header, 4),
                            collectionTreeQueryRepository.findGroupTranslations(groupId, thesaurusId, lang).stream()
                                    .map(row -> new GroupTranslationItem(
                                            ConceptMapper.stringAt(row, 0),
                                            ConceptMapper.stringAt(row, 1)
                                    ))
                                    .toList(),
                            collectionTreeQueryRepository.findNotesByIdentifier(groupId, thesaurusId, lang).stream()
                                    .map(ConceptMapper::toNote)
                                    .toList(),
                            loadMembers(thesaurusId, groupId, lang)
                    );
                });
    }

    private List<FacetMemberItem> loadMembers(String thesaurusId, String groupId, String lang) {
        return collectionTreeQueryRepository
                .findMemberConcepts(groupId, thesaurusId, lang, MAX_MEMBER_CONCEPTS)
                .stream()
                .map(row -> new FacetMemberItem(
                        ConceptMapper.stringAt(row, 0),
                        ConceptMapper.stringAt(row, 1)
                ))
                .toList();
    }
}
