package fr.cnrs.opentheso.v2.concept.ui;

import fr.cnrs.opentheso.v2.shared.ui.V2LocaleBean;
import fr.cnrs.opentheso.v2.toolbox.model.EditionStatistics;
import fr.cnrs.opentheso.v2.toolbox.service.EditionThesaurusService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.inject.Named;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.primefaces.PrimeFaces;

import java.io.Serializable;

@Named("v2ThesaurusHomeStatsBean")
@ApplicationScoped
@RequiredArgsConstructor
public class ThesaurusHomeStatsBean implements Serializable {

    private final EditionThesaurusService editionThesaurusService;
    private final V2LocaleBean v2LocaleBean;

    public void showInfos(String thesaurusId) {
        if (StringUtils.isBlank(thesaurusId)) {
            return;
        }
        EditionStatistics stats = editionThesaurusService.loadStatistics(thesaurusId.trim());
        FacesMessage message = new FacesMessage(
                FacesMessage.SEVERITY_INFO,
                v2LocaleBean.getMsg("info"),
                v2LocaleBean.getMsg("candidat.total_concepts") + " = " + stats.conceptCount() + "\n"
                        + v2LocaleBean.getMsg("candidat.titre") + " = " + stats.candidateCount() + "\n"
                        + v2LocaleBean.getMsg("search.deprecated") + " = " + stats.deprecatedCount()
        );
        PrimeFaces.current().dialog().showMessageDynamic(message);
    }
}
