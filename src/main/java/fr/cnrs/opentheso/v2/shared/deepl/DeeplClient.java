package fr.cnrs.opentheso.v2.shared.deepl;

import com.deepl.api.Language;
import com.deepl.api.Translator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Client DeepL indépendant du legacy ({@code DeeplService}).
 */
@Slf4j
@Component
public class DeeplClient {

    public String translate(String authKey, String value, String fromLang, String toLang) {
        if (StringUtils.isAnyBlank(authKey, value, fromLang, toLang)) {
            return null;
        }
        try {
            return new Translator(authKey).translateText(value, fromLang, toLang).getText().trim();
        } catch (Exception ex) {
            log.error("DeepL translate failed", ex);
            return null;
        }
    }

    public List<Language> listSourceLanguages(String authKey) {
        if (StringUtils.isBlank(authKey)) {
            return Collections.emptyList();
        }
        try {
            return new Translator(authKey).getSourceLanguages();
        } catch (Exception ex) {
            log.error("DeepL getSourceLanguages failed", ex);
            return Collections.emptyList();
        }
    }

    public List<Language> listTargetLanguages(String authKey) {
        if (StringUtils.isBlank(authKey)) {
            return Collections.emptyList();
        }
        try {
            return new Translator(authKey).getTargetLanguages();
        } catch (Exception ex) {
            log.error("DeepL getTargetLanguages failed", ex);
            return Collections.emptyList();
        }
    }
}
