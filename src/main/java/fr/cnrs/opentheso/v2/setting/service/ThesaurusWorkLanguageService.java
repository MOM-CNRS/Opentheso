package fr.cnrs.opentheso.v2.setting.service;

import fr.cnrs.opentheso.v2.shared.repository.ThesaurusSettingsQueryRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ThesaurusWorkLanguageService {

    private final ThesaurusSettingsQueryRepository thesaurusSettingsQueryRepository;

    @Value("${settings.workLanguage:fr}")
    private String defaultWorkLanguage;

    @Transactional(readOnly = true)
    public String resolveForThesaurus(String thesaurusId) {
        if (StringUtils.isBlank(thesaurusId)) {
            return defaultWorkLanguage;
        }
        return thesaurusSettingsQueryRepository.findSourceLanguage(thesaurusId.trim())
                .filter(StringUtils::isNotBlank)
                .orElse(defaultWorkLanguage);
    }

    public String getDefaultWorkLanguage() {
        return defaultWorkLanguage;
    }
}
