package fr.cnrs.opentheso.shared.service;

import fr.cnrs.opentheso.shared.model.LegalPageContent;
import fr.cnrs.opentheso.shared.repository.LegalPageQueryRepository;
import fr.cnrs.opentheso.utils.ToolsHelper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class LegalPageService {

    public static final String MENTIONS = "mentions";
    public static final String PRIVACY = "donnees";
    public static final String ACCESSIBILITY = "accessibilite";
    public static final int TITLE_MAX = 400;

    private static final Set<String> CODES = Set.of(MENTIONS, PRIVACY, ACCESSIBILITY);
    private static final Set<String> LANGS = Set.of("fr", "en", "de", "es", "ar");

    private final LegalPageQueryRepository legalPageQueryRepository;

    public boolean isKnown(String code) {
        return CODES.contains(normalizeCode(code));
    }

    public static String titleKey(String code) {
        return switch (normalizeCode(code)) {
            case PRIVACY -> "legal.privacy";
            case ACCESSIBILITY -> "legal.accessibility";
            default -> "legal.mentions";
        };
    }

    @Transactional(readOnly = true)
    public Optional<LegalPageContent> find(String code, String lang) {
        String page = normalizeCode(code);
        if (!CODES.contains(page)) {
            return Optional.empty();
        }
        return legalPageQueryRepository.find(page, normalizeLang(lang));
    }

    @Transactional(readOnly = true)
    public Map<String, String> findTitles(String lang) {
        return legalPageQueryRepository.findTitles(normalizeLang(lang));
    }

    @Transactional
    public boolean save(String code, String lang, String title, String html) {
        String page = normalizeCode(code);
        if (!CODES.contains(page)) {
            return false;
        }
        String cleanTitle = StringUtils.normalizeSpace(StringUtils.defaultString(title));
        if (cleanTitle.isEmpty() || cleanTitle.length() > TITLE_MAX) {
            return false;
        }
        legalPageQueryRepository.upsert(
                page,
                normalizeLang(lang),
                cleanTitle,
                new ToolsHelper().normalizeHtml(html));
        return true;
    }

    private static String normalizeCode(String code) {
        return StringUtils.trimToEmpty(code).toLowerCase(Locale.ROOT);
    }

    private static String normalizeLang(String lang) {
        String code = StringUtils.trimToEmpty(lang).toLowerCase(Locale.ROOT);
        return LANGS.contains(code) ? code : "fr";
    }
}
