package fr.cnrs.opentheso.v2.concept.ui;

import fr.cnrs.opentheso.entites.ProjectDescription;
import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.concept.model.ConsultationProjectLangOption;
import fr.cnrs.opentheso.v2.concept.model.ConsultationProjectThesaurusItem;
import fr.cnrs.opentheso.v2.concept.service.ConsultationProjectHomeService;
import fr.cnrs.opentheso.v2.concept.support.ConceptFlagSupport;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import fr.cnrs.opentheso.v2.shared.ui.V2LocaleBean;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

@Getter
@Setter
@SessionScoped
@Named("v2ConsultationProjectHomeBean")
@Scope(value = "session", proxyMode = ScopedProxyMode.TARGET_CLASS)
@RequiredArgsConstructor
public class ConsultationProjectHomeBean implements Serializable {

    private final transient ConsultationProjectHomeService consultationProjectHomeService;
    private final transient UserSession userSession;
    private final transient V2LocaleBean v2LocaleBean;

    private int projectId = -1;
    private List<ConsultationProjectThesaurusItem> thesauri = Collections.emptyList();
    private List<ConsultationProjectLangOption> descriptionLangs = Collections.emptyList();
    private List<ConsultationProjectLangOption> allLangs = Collections.emptyList();

    private String selectedDescriptionLang;
    private String descriptionHtml;
    private Integer descriptionEntityId;

    private boolean editing;
    private String editLang;
    private String editHtml;

    public void clear() {
        projectId = -1;
        thesauri = Collections.emptyList();
        descriptionLangs = Collections.emptyList();
        selectedDescriptionLang = null;
        descriptionHtml = null;
        descriptionEntityId = null;
        editing = false;
        editLang = null;
        editHtml = null;
    }

    public void load(int selectedProjectId) {
        clear();
        if (selectedProjectId < 0) {
            return;
        }
        projectId = selectedProjectId;
        thesauri = consultationProjectHomeService.listThesauriWithCounts(
                userSession.isLoggedIn() ? userSession.getCurrentUserId() : null,
                userSession.isSuperAdmin(),
                selectedProjectId,
                v2LocaleBean.getIdLangue()
        );
        descriptionLangs = consultationProjectHomeService.listDescriptionLanguages(selectedProjectId);
        applyDescription(v2LocaleBean.getIdLangue());
    }

    public boolean isThesauriVisible() {
        return thesauri != null && !thesauri.isEmpty();
    }

    public boolean isDescriptionVisible() {
        return descriptionLangs != null && !descriptionLangs.isEmpty();
    }

    public boolean isEditAllowed() {
        return userSession.isLoggedIn() && userSession.isSuperAdmin() && !editing;
    }

    public void onDescriptionLangChange() {
        applyDescription(selectedDescriptionLang);
    }

    public void startEdit() {
        if (!userSession.isSuperAdmin()) {
            return;
        }
        if (allLangs.isEmpty()) {
            allLangs = consultationProjectHomeService.listAllLanguages();
        }
        editing = true;
        editLang = StringUtils.isNotBlank(selectedDescriptionLang)
                ? selectedDescriptionLang
                : v2LocaleBean.getIdLangue();
        consultationProjectHomeService.findDescription(projectId, editLang)
                .ifPresentOrElse(
                        desc -> {
                            descriptionEntityId = desc.getId();
                            editHtml = desc.getDescription();
                        },
                        () -> {
                            descriptionEntityId = null;
                            editHtml = "";
                        }
                );
    }

    public void onEditLangChange() {
        consultationProjectHomeService.findDescription(projectId, editLang)
                .ifPresentOrElse(
                        desc -> {
                            descriptionEntityId = desc.getId();
                            editHtml = desc.getDescription();
                        },
                        () -> {
                            descriptionEntityId = null;
                            editHtml = "";
                        }
                );
    }

    public void cancelEdit() {
        editing = false;
        editHtml = null;
        editLang = null;
        applyDescription(selectedDescriptionLang);
    }

    public void saveDescription() {
        if (!userSession.isSuperAdmin()) {
            return;
        }
        if (StringUtils.isBlank(editHtml)) {
            MessageUtils.showWarnMessage("Veuillez saisir une description !");
            return;
        }
        if (descriptionEntityId != null
                && editHtml.equalsIgnoreCase(StringUtils.defaultString(descriptionHtml))) {
            MessageUtils.showWarnMessage("Veuillez proposer une présentation différente de l'ancienne !");
            return;
        }
        ProjectDescription saved = consultationProjectHomeService.saveDescription(projectId, editLang, editHtml);
        descriptionEntityId = saved.getId();
        selectedDescriptionLang = saved.getLang();
        descriptionHtml = saved.getDescription();
        descriptionLangs = consultationProjectHomeService.listDescriptionLanguages(projectId);
        editing = false;
        MessageUtils.showInformationMessage("Description ajoutée avec succès");
    }

    public void deleteDescription() {
        if (!userSession.isSuperAdmin() || descriptionEntityId == null) {
            return;
        }
        consultationProjectHomeService.findDescription(projectId, selectedDescriptionLang)
                .ifPresent(consultationProjectHomeService::deleteDescription);
        descriptionLangs = consultationProjectHomeService.listDescriptionLanguages(projectId);
        if (descriptionLangs.isEmpty()) {
            descriptionHtml = null;
            descriptionEntityId = null;
            selectedDescriptionLang = null;
        } else {
            applyDescription(descriptionLangs.get(0).iso6391());
        }
        MessageUtils.showWarnMessage("Description supprimée avec succès !");
    }

    public String flagUrlForSelectedDescription() {
        return flagUrl(selectedDescriptionLang);
    }

    public String flagUrlForEditLang() {
        return flagUrl(editLang);
    }

    private String flagUrl(String iso6391) {
        List<ConsultationProjectLangOption> source = allLangs.isEmpty() ? descriptionLangs : allLangs;
        if (source.isEmpty() && allLangs.isEmpty()) {
            allLangs = consultationProjectHomeService.listAllLanguages();
            source = allLangs;
        }
        String countryCode = consultationProjectHomeService.resolveCountryCode(iso6391, source);
        return ConceptFlagSupport.resolveFlagImageUrl(countryCode);
    }

    private void applyDescription(String preferredLang) {
        consultationProjectHomeService.resolveDescription(projectId, preferredLang)
                .ifPresentOrElse(
                        desc -> {
                            descriptionEntityId = desc.getId();
                            selectedDescriptionLang = desc.getLang();
                            descriptionHtml = desc.getDescription();
                        },
                        () -> {
                            descriptionEntityId = null;
                            selectedDescriptionLang = preferredLang;
                            descriptionHtml = null;
                        }
                );
    }
}
