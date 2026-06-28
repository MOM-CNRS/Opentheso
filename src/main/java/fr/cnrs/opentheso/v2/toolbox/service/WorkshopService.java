package fr.cnrs.opentheso.v2.toolbox.service;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Service
public class WorkshopService {

    public String resolveThesaurusId(String contextThesaurusId, String selectedThesaurusId) {
        if (StringUtils.isNotBlank(contextThesaurusId)) {
            return contextThesaurusId;
        }
        return StringUtils.trimToEmpty(selectedThesaurusId);
    }

    public String resolveThesaurusTitle(
            String contextTitle,
            String contextThesaurusId,
            String selectedThesaurusName,
            String selectedThesaurusId
    ) {
        if (StringUtils.isNotBlank(contextTitle)) {
            return contextTitle;
        }
        if (StringUtils.isNotBlank(selectedThesaurusName)) {
            return selectedThesaurusName;
        }
        String thesaurusId = resolveThesaurusId(contextThesaurusId, selectedThesaurusId);
        return StringUtils.isNotBlank(thesaurusId) ? thesaurusId : "";
    }
}
