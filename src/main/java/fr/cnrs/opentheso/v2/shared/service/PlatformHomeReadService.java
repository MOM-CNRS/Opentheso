package fr.cnrs.opentheso.v2.shared.service;

import fr.cnrs.opentheso.v2.shared.repository.PlatformHomeQueryRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlatformHomeReadService {

    private final PlatformHomeQueryRepository platformHomeQueryRepository;

    @Value("${settings.workLanguage:fr}")
    private String defaultWorkLanguage;

    @Value("${info.application.version:}")
    private String applicationVersion;

    @Transactional(readOnly = true)
    public String loadHomePageHtml(String lang) {
        String resolvedLang = StringUtils.isBlank(lang) ? defaultWorkLanguage : lang.toLowerCase();
        return platformHomeQueryRepository.findHomePageHtml(resolvedLang)
                .orElseGet(() -> platformHomeQueryRepository.findHomePageHtml(defaultWorkLanguage).orElse(""));
    }

    @Transactional(readOnly = true)
    public String getGoogleAnalyticsCode() {
        return platformHomeQueryRepository.findGoogleAnalyticsCode().orElse("");
    }

    public String getApplicationVersion() {
        return applicationVersion;
    }
}
