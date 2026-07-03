package fr.cnrs.opentheso.legacybridge;

import fr.cnrs.opentheso.bean.menu.theso.RoleOnThesaurusBean;
import fr.cnrs.opentheso.bean.menu.theso.SelectedTheso;
import fr.cnrs.opentheso.v2.shared.session.ThesaurusLegacySync;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class LegacyThesaurusSync implements ThesaurusLegacySync {

    private final SelectedTheso selectedTheso;
    private final RoleOnThesaurusBean roleOnThesaurusBean;

    @Override
    public void applyThesaurusId(String thesaurusId) {
        applyThesaurusId(thesaurusId, null);
    }

    @Override
    public void applyThesaurusId(String thesaurusId, String language) {
        if (StringUtils.isBlank(thesaurusId)) {
            return;
        }
        String id = thesaurusId.trim();
        selectedTheso.setSelectedIdTheso(id);
        selectedTheso.setCurrentIdTheso(id);
        roleOnThesaurusBean.initNodePref(id);
        applyLanguage(language);
    }

    @Override
    public Optional<String> readSelectedThesaurusId() {
        return Optional.ofNullable(selectedTheso.getCurrentIdTheso()).filter(StringUtils::isNotBlank);
    }

    @Override
    public Optional<String> readSelectedLanguage() {
        return Optional.ofNullable(selectedTheso.getCurrentLang()).filter(StringUtils::isNotBlank);
    }

    @Override
    public void clearSelection() {
        selectedTheso.setSelectedIdTheso("");
        selectedTheso.setSelectedLang(null);
        selectedTheso.setProjectIdSelected("-1");
        selectedTheso.setSelectedProject();
        try {
            selectedTheso.setSelectedTheso();
        } catch (IOException e) {
            log.warn("Réinitialisation du thésaurus sélectionné impossible", e);
        }
    }

    private void applyLanguage(String language) {
        String resolved = StringUtils.firstNonBlank(
                language,
                selectedTheso.getSelectedLang(),
                selectedTheso.getCurrentLang(),
                selectedTheso.getWorkLanguage()
        );
        if (StringUtils.isBlank(resolved)) {
            return;
        }
        selectedTheso.setSelectedLang(resolved);
        if (StringUtils.isBlank(selectedTheso.getCurrentLang())) {
            selectedTheso.setCurrentLang(resolved);
        }
    }
}
