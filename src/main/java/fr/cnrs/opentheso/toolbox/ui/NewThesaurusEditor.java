package fr.cnrs.opentheso.toolbox.ui;

import fr.cnrs.opentheso.toolbox.exception.InvalidToolboxDataException;
import fr.cnrs.opentheso.toolbox.model.NewThesaurusRequest;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class NewThesaurusEditor implements Serializable {

    private String title = "";
    private String persistentNameThesaurus = "";
    private String selectedLanguage;
    private final List<String> possibleLanguages = new ArrayList<>();
    private String selectedProjectId = "";
    private boolean privateThesaurus;
    private String organization = "";

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
                List.copyOf(possibleLanguages)
        );
    }
}
