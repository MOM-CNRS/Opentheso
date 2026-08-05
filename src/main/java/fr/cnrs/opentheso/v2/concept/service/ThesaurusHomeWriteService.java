package fr.cnrs.opentheso.v2.concept.service;

import fr.cnrs.opentheso.repositories.ThesaurusHomePageRepository;
import fr.cnrs.opentheso.utils.ToolsHelper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ThesaurusHomeWriteService {

    @Value("${settings.workLanguage:fr}")
    private String defaultWorkLanguage;

    private final ThesaurusHomePageRepository thesaurusHomePageRepository;

    @Transactional(readOnly = true)
    public String loadHtml(String thesaurusId, String languageCode) {
        if (StringUtils.isBlank(thesaurusId)) {
            return "";
        }
        String lang = StringUtils.defaultIfBlank(languageCode, defaultWorkLanguage);
        return thesaurusHomePageRepository.findByIdThesoAndLang(thesaurusId, lang)
                .map(page -> StringUtils.defaultString(page.getHtmlCode()))
                .orElse("");
    }

    @Transactional
    public boolean saveHtml(String thesaurusId, String languageCode, String html) {
        if (StringUtils.isBlank(thesaurusId)) {
            return false;
        }
        String lang = StringUtils.defaultIfBlank(languageCode, defaultWorkLanguage);
        String normalized = new ToolsHelper().normalizeHtml(html);
        return thesaurusHomePageRepository.upsertHtmlCode(thesaurusId, lang, normalized) > 0;
    }
}
