package fr.cnrs.opentheso.v2.shared.service;

import fr.cnrs.opentheso.utils.ToolsHelper;
import fr.cnrs.opentheso.v2.shared.repository.PlatformHomeQueryRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Écriture page d'accueil plateforme + Google Analytics.
 */
@Service
@RequiredArgsConstructor
public class PlatformHomeWriteService {

    private final PlatformHomeQueryRepository platformHomeQueryRepository;
    private final PlatformHomeReadService platformHomeReadService;

    @Value("${settings.workLanguage:fr}")
    private String defaultWorkLanguage;

    @Transactional(readOnly = true)
    public String loadHomePageHtml(String lang) {
        return platformHomeReadService.loadHomePageHtml(lang);
    }

    @Transactional
    public boolean saveHomePageHtml(String lang, String html) {
        String resolvedLang = StringUtils.isBlank(lang) ? defaultWorkLanguage : lang.toLowerCase();
        String normalized = new ToolsHelper().normalizeHtml(html);
        return platformHomeQueryRepository.upsertHomePageHtml(resolvedLang, normalized);
    }

    @Transactional(readOnly = true)
    public String loadGoogleAnalyticsCode() {
        return platformHomeQueryRepository.findGoogleAnalyticsCode().orElse("");
    }

    @Transactional
    public void saveGoogleAnalyticsCode(String code) {
        platformHomeQueryRepository.saveGoogleAnalyticsCode(StringUtils.defaultString(code));
    }
}
