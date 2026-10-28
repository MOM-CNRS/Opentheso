package fr.cnrs.opentheso.toolbox.ui;

import fr.cnrs.opentheso.toolbox.exception.InvalidToolboxDataException;
import fr.cnrs.opentheso.toolbox.model.NewThesaurusRequest;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class NewThesaurusEditor implements Serializable {

    private String title = "";
    private String persistentNameThesaurus = "";
    private String selectedLanguage;
    private final List<String> possibleLanguages = new ArrayList<>();
    private String selectedProjectId = "";
    private boolean privateThesaurus;
    /** Maître par défaut : source de synchronisation. */
    private boolean master = true;
    private String organization = "";
    /** Domaine thématique, enregistré comme dcterms:subject. */
    private String subject = "";
    /** Nom traduit par code de langue, hors langue du nom principal. */
    private final Map<String, String> titlesByLanguage = new LinkedHashMap<>();

    public static NewThesaurusEditor empty() {
        return new NewThesaurusEditor();
    }

    public void setSelectedLanguage(String selectedLanguage) {
        String previous = this.selectedLanguage;
        this.selectedLanguage = StringUtils.trimToNull(selectedLanguage);
        if (StringUtils.isNotBlank(previous)
                && !previous.equalsIgnoreCase(StringUtils.defaultString(this.selectedLanguage))) {
            includePossible(previous);
        }
        excludePrincipal();
    }

    public void includePossible(String code) {
        String normalized = StringUtils.trimToEmpty(code);
        if (normalized.isEmpty()
                || normalized.equalsIgnoreCase(StringUtils.defaultString(selectedLanguage))
                || isPossible(normalized)) {
            return;
        }
        possibleLanguages.add(normalized);
    }

    public void togglePossible(String code) {
        String normalized = StringUtils.trimToEmpty(code);
        if (normalized.isEmpty() || normalized.equalsIgnoreCase(StringUtils.defaultString(selectedLanguage))) {
            return;
        }
        if (!possibleLanguages.removeIf(lang -> lang.equalsIgnoreCase(normalized))) {
            possibleLanguages.add(normalized);
        }
    }

    private void excludePrincipal() {
        possibleLanguages.removeIf(lang -> lang.equalsIgnoreCase(StringUtils.defaultString(selectedLanguage)));
    }

    public void replacePossible(List<String> codes) {
        possibleLanguages.clear();
        if (codes == null) {
            return;
        }
        codes.forEach(this::includePossible);
    }

    public boolean isPossible(String code) {
        String normalized = StringUtils.trimToEmpty(code);
        return possibleLanguages.stream().anyMatch(lang -> lang.equalsIgnoreCase(normalized));
    }

    public String getTitlesEncoded() {
        StringBuilder encoded = new StringBuilder();
        titlesByLanguage.forEach((code, value) -> {
            if (encoded.length() > 0) {
                encoded.append('\n');
            }
            encoded.append(code).append('=').append(value.replace('\n', ' ').replace('\r', ' '));
        });
        return encoded.toString();
    }

    public void setTitlesEncoded(String encoded) {
        titlesByLanguage.clear();
        if (StringUtils.isBlank(encoded)) {
            return;
        }
        for (String line : encoded.split("\\R")) {
            int split = line.indexOf('=');
            if (split <= 0) {
                continue;
            }
            String code = line.substring(0, split).trim();
            String value = line.substring(split + 1).trim();
            if (code.isEmpty()
                    || value.isEmpty()
                    || code.equalsIgnoreCase(StringUtils.defaultString(selectedLanguage))) {
                continue;
            }
            titlesByLanguage.put(code, value);
        }
    }

    private Map<String, String> titlesForRequest() {
        Map<String, String> titles = new LinkedHashMap<>();
        for (String code : possibleLanguages) {
            titlesByLanguage.forEach((key, value) -> {
                if (key.equalsIgnoreCase(code) && StringUtils.isNotBlank(value)) {
                    titles.put(code, value.trim());
                }
            });
        }
        return Map.copyOf(titles);
    }

    public NewThesaurusRequest toRequest() {
        Integer projectId = null;
        if (StringUtils.isNotBlank(selectedProjectId)) {
            try {
                projectId = Integer.parseInt(selectedProjectId.trim());
            } catch (NumberFormatException e) {
                throw new InvalidToolboxDataException("Identifiant de projet invalide : " + selectedProjectId);
            }
        }
        excludePrincipal();
        return new NewThesaurusRequest(
                StringUtils.trimToEmpty(title),
                StringUtils.trimToEmpty(persistentNameThesaurus),
                selectedLanguage,
                projectId,
                privateThesaurus,
                StringUtils.trimToEmpty(organization),
                List.copyOf(possibleLanguages),
                master,
                StringUtils.trimToEmpty(subject),
                titlesForRequest()
        );
    }
}
