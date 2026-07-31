package fr.cnrs.opentheso.v2.toolbox.ui;

import fr.cnrs.opentheso.v2.toolbox.exception.InvalidToolboxDataException;
import fr.cnrs.opentheso.v2.toolbox.model.NewThesaurusRequest;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;

@Getter
@Setter
public class NewThesaurusEditor implements Serializable {

    private String title = "";
    private String persistentNameThesaurus = "";
    private String selectedLanguage;
    private String selectedProjectId = "";

    public static NewThesaurusEditor empty() {
        return new NewThesaurusEditor();
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
        return new NewThesaurusRequest(
                StringUtils.trimToEmpty(title),
                StringUtils.trimToEmpty(persistentNameThesaurus),
                selectedLanguage,
                projectId
        );
    }
}
