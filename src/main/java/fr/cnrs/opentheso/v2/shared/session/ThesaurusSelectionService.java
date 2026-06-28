package fr.cnrs.opentheso.v2.shared.session;

import fr.cnrs.opentheso.v2.shared.repository.ThesaurusSettingsQueryRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ThesaurusSelectionService {

    private final ThesaurusSettingsQueryRepository thesaurusSettingsQueryRepository;

    @Value("${settings.workLanguage:fr}")
    private String workLanguage;

    @Transactional(readOnly = true)
    public ThesaurusSelection resolve(String thesaurusId) {
        if (StringUtils.isBlank(thesaurusId)) {
            return null;
        }
        String title = thesaurusSettingsQueryRepository
                .findThesaurusTitle(thesaurusId.trim(), workLanguage)
                .orElse(thesaurusId.trim());
        return new ThesaurusSelection(thesaurusId.trim(), title);
    }
}
