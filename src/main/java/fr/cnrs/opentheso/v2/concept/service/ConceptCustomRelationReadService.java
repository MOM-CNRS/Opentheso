package fr.cnrs.opentheso.v2.concept.service;

import fr.cnrs.opentheso.v2.concept.mapper.ConceptMapper;
import fr.cnrs.opentheso.v2.concept.model.ConceptCustomRelationItem;
import fr.cnrs.opentheso.v2.shared.repository.ConceptQueryRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ConceptCustomRelationReadService {

    private final ConceptQueryRepository conceptQueryRepository;

    @Transactional(readOnly = true)
    public List<ConceptCustomRelationItem> loadCustomRelations(
            String thesaurusId,
            String conceptId,
            String lang
    ) {
        if (StringUtils.isAnyBlank(thesaurusId, conceptId, lang)) {
            return Collections.emptyList();
        }
        return conceptQueryRepository.findCustomRelations(conceptId, thesaurusId, lang, lang).stream()
                .map(row -> new ConceptCustomRelationItem(
                        ConceptMapper.stringAt(row, 0),
                        ConceptMapper.stringAt(row, 1),
                        ConceptMapper.stringAt(row, 2),
                        ConceptMapper.stringAt(row, 3),
                        parseBoolean(row, 4)
                ))
                .toList();
    }

    private static boolean parseBoolean(Object[] row, int index) {
        Object value = row[index];
        if (value instanceof Boolean bool) {
            return bool;
        }
        return "true".equalsIgnoreCase(String.valueOf(value)) || "t".equalsIgnoreCase(String.valueOf(value));
    }
}
