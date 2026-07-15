package fr.cnrs.opentheso.legacybridge;

import fr.cnrs.opentheso.bean.menu.theso.SelectedTheso;
import fr.cnrs.opentheso.bean.menu.users.CurrentUser;
import fr.cnrs.opentheso.bean.rightbody.viewconcept.ConceptView;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LegacyConceptSync {

    private final ConceptView conceptView;
    private final CurrentUser currentUser;
    private final SelectedTheso selectedTheso;

    public void syncConceptSelection(String thesaurusId, String conceptId, String language) {
        if (StringUtils.isAnyBlank(thesaurusId, conceptId, language)) {
            return;
        }
        selectedTheso.setCurrentIdTheso(thesaurusId.trim());
        selectedTheso.setCurrentLang(language.trim());
        conceptView.getConceptForTree(thesaurusId.trim(), conceptId.trim(), language.trim(), currentUser);
    }
}
