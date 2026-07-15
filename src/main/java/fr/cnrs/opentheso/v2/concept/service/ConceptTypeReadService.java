package fr.cnrs.opentheso.v2.concept.service;

import fr.cnrs.opentheso.v2.concept.mapper.ConceptMapper;
import fr.cnrs.opentheso.v2.shared.repository.ConceptQueryRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConceptTypeReadService {

    private final ConceptQueryRepository conceptQueryRepository;

    @Transactional(readOnly = true)
    public String resolveLabel(String code, String thesaurusId, String interfaceLang) {
        if (StringUtils.isAnyBlank(code, thesaurusId)) {
            return "";
        }
        return conceptQueryRepository.findConceptType(code, thesaurusId)
                .map(row -> "fr".equalsIgnoreCase(interfaceLang)
                        ? ConceptMapper.stringAt(row, 0)
                        : ConceptMapper.stringAt(row, 1))
                .filter(StringUtils::isNotBlank)
                .orElse("");
    }
}
