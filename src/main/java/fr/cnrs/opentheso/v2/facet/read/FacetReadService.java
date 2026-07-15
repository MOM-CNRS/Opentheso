package fr.cnrs.opentheso.v2.facet.read;

import fr.cnrs.opentheso.v2.concept.mapper.ConceptMapper;
import fr.cnrs.opentheso.v2.concept.model.FacetDetailOverview;
import fr.cnrs.opentheso.v2.concept.model.FacetMemberItem;
import fr.cnrs.opentheso.v2.concept.model.GroupTranslationItem;
import fr.cnrs.opentheso.v2.shared.repository.ConceptQueryRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FacetReadService {

    private final ConceptQueryRepository conceptQueryRepository;

    @Transactional(readOnly = true)
    public Optional<FacetDetailOverview> loadDetail(String thesaurusId, String facetId, String lang) {
        if (StringUtils.isAnyBlank(thesaurusId, facetId)) {
            return Optional.empty();
        }
        return conceptQueryRepository.findFacetHeader(facetId, thesaurusId, lang)
                .map(header -> {
                    String parentId = ConceptMapper.stringAt(header, 1);
                    String parentLabel = conceptQueryRepository.findConceptHeader(parentId, thesaurusId, lang)
                            .map(row -> row.prefLabel())
                            .filter(StringUtils::isNotBlank)
                            .orElse(parentId);
                    return new FacetDetailOverview(
                            ConceptMapper.stringAt(header, 0),
                            ConceptMapper.stringAt(header, 2),
                            lang,
                            parentId,
                            parentLabel,
                            conceptQueryRepository.findFacetMembers(facetId, thesaurusId, lang).stream()
                                    .map(row -> new FacetMemberItem(row.conceptId(), row.label()))
                                    .toList(),
                            conceptQueryRepository.findFacetTranslations(facetId, thesaurusId, lang).stream()
                                    .map(row -> new GroupTranslationItem(
                                            ConceptMapper.stringAt(row, 0),
                                            ConceptMapper.stringAt(row, 1)
                                    ))
                                    .toList(),
                            conceptQueryRepository.findNotesByIdentifier(facetId, thesaurusId, lang).stream()
                                    .map(ConceptMapper::toNote)
                                    .toList()
                    );
                });
    }
}
