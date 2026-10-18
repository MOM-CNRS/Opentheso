package fr.cnrs.opentheso.concept.ui;

import fr.cnrs.opentheso.shared.ui.LocaleBean;
import fr.cnrs.opentheso.toolbox.model.EditionStatistics;
import fr.cnrs.opentheso.toolbox.service.EditionThesaurusService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.inject.Named;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.primefaces.PrimeFaces;

import java.io.Serializable;

@Named("thesaurusHomeStatsBean")
@ApplicationScoped
@RequiredArgsConstructor
public class ThesaurusHomeStatsBean implements Serializable {

    private final transient EditionThesaurusService editionThesaurusService;
    private final transient LocaleBean localeBean;

    public void showInfos(String thesaurusId) {
        if (StringUtils.isBlank(thesaurusId)) {
            return;
        }
        EditionStatistics stats = editionThesaurusService.loadStatistics(thesaurusId.trim());
        FacesMessage message = new FacesMessage(
                FacesMessage.SEVERITY_INFO,
                localeBean.getMsg("info"),
                localeBean.getMsg("candidat.total_concepts") + " = " + stats.conceptCount() + "\n"
                        + localeBean.getMsg("candidat.titre") + " = " + stats.candidateCount() + "\n"
                        + localeBean.getMsg("search.deprecated") + " = " + stats.deprecatedCount()
        );
        PrimeFaces.current().dialog().showMessageDynamic(message);
    }
}
