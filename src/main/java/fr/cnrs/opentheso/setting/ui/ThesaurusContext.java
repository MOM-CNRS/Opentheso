package fr.cnrs.opentheso.setting.ui;

import fr.cnrs.opentheso.setting.service.ThesaurusWorkLanguageService;
import fr.cnrs.opentheso.shared.session.ThesaurusSelection;
import fr.cnrs.opentheso.shared.session.ThesaurusSelectionService;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.component.UIViewRoot;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;

import java.io.Serializable;
import java.util.Map;

/**
 * Thésaurus courant.
 * <p>
 * La session ne garde que le dernier thésaurus choisi : c'est la valeur de départ d'une nouvelle page.
 * Chaque vue JSF (donc chaque onglet) épingle son propre thésaurus dans son état de vue : un autre onglet
 * qui change de thésaurus ne fait plus basculer celui-ci (arbre, fiche, actions).
 */
@Getter
@Setter
@SessionScoped
@Named("thesaurusContext")
@Scope(value = "session", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class ThesaurusContext implements Serializable {

    private final transient ThesaurusSelectionService thesaurusSelectionService;
    private final transient ThesaurusWorkLanguageService thesaurusWorkLanguageService;

    @Value("${settings.workLanguage:fr}")
    private String defaultWorkLanguage;

    private String idConceptFromUri;
    private String idGroupFromUri;
    private String idFacetFromUri;
    private String idThesoFromUri;

    /** Dernier thésaurus choisi dans la session (valeur de départ des nouvelles vues). */
    private String currentThesaurusId;
    private String currentThesaurusTitle;
    private String currentLanguage;

    /** Clé, dans l'état de la vue JSF, du thésaurus épinglé par l'onglet. */
    static final String VIEW_KEY = ThesaurusContext.class.getName() + ".tab";

    private boolean fromUrl;

    public ThesaurusContext(
            ThesaurusSelectionService thesaurusSelectionService,
            ThesaurusWorkLanguageService thesaurusWorkLanguageService
    ) {
        this.thesaurusSelectionService = thesaurusSelectionService;
        this.thesaurusWorkLanguageService = thesaurusWorkLanguageService;
    }

    /**
     * Applique {@code idt} depuis l'URL. Les ids concept/collection/facette restent disponibles
     * pour l'écran appelant (à consommer puis effacer).
     */
    public void syncFromViewParams() {
        if (StringUtils.isNotBlank(idThesoFromUri)) {
            fromUrl = true;
            applyThesaurus(idThesoFromUri.trim());
            idThesoFromUri = null;
            return;
        }
        fromUrl = false;
    }

    public String resolveThesaurusId() {
        String id = getCurrentThesaurusId();
        return StringUtils.isNotBlank(id) ? id : null;
    }

    public String resolveWorkLanguage() {
        String language = getCurrentLanguage();
        if (StringUtils.isNotBlank(language)) {
            return language;
        }
        return thesaurusWorkLanguageService.resolveForThesaurus(resolveThesaurusId());
    }

    public String getCurrentThesaurusId() {
        return tab().id();
    }

    public String getCurrentThesaurusTitle() {
        return tab().title();
    }

    public String getCurrentLanguage() {
        return tab().language();
    }

    /** Thésaurus de l'onglet : celui de sa vue JSF, épinglé à la première lecture ; hors JSF, celui de la session. */
    private TabSelection tab() {
        Map<String, Object> viewMap = viewMap();
        if (viewMap == null) {
            return sessionSelection();
        }
        Object pinned = viewMap.get(VIEW_KEY);
        if (pinned instanceof TabSelection selection) {
            return selection;
        }
        TabSelection selection = sessionSelection();
        if (selection.id() != null) {
            viewMap.put(VIEW_KEY, selection);
        }
        return selection;
    }

    private TabSelection sessionSelection() {
        return new TabSelection(currentThesaurusId, currentThesaurusTitle, currentLanguage);
    }

    /** Enregistre la sélection pour la session (nouvelles pages) et pour l'onglet courant. */
    private void store(String id, String title, String language) {
        currentThesaurusId = id;
        currentThesaurusTitle = title;
        currentLanguage = language;
        Map<String, Object> viewMap = viewMap();
        if (viewMap != null) {
            viewMap.put(VIEW_KEY, new TabSelection(id, title, language));
        }
    }

    private static Map<String, Object> viewMap() {
        FacesContext faces = FacesContext.getCurrentInstance();
        if (faces == null) {
            return null;
        }
        UIViewRoot root = faces.getViewRoot();
        return root == null ? null : root.getViewMap(true);
    }

    /** Thésaurus épinglé par une vue (sérialisable avec l'état de la vue). */
    record TabSelection(String id, String title, String language) implements Serializable {
    }

    public boolean matchesCurrentThesaurus(String thesaurusId) {
        return StringUtils.isNotBlank(thesaurusId)
                && thesaurusId.equalsIgnoreCase(resolveThesaurusId());
    }

    public void clearSelection() {
        store(null, null, null);
        fromUrl = false;
    }

    public void changeWorkLanguage(String language) {
        if (StringUtils.isBlank(language)) {
            return;
        }
        TabSelection tab = tab();
        store(tab.id(), tab.title(), language);
    }

    public void selectThesaurus(String thesaurusId) {
        if (StringUtils.isBlank(thesaurusId)) {
            clearSelection();
            return;
        }
        applyThesaurus(thesaurusId.trim());
    }

    /**
     * Sélection depuis le catalogue header (évite les requêtes title / source_lang).
     */
    public void selectThesaurus(String thesaurusId, String title, String workLanguage) {
        if (StringUtils.isBlank(thesaurusId)) {
            clearSelection();
            return;
        }
        String id = thesaurusId.trim();
        if (StringUtils.isNotBlank(title) && StringUtils.isNotBlank(workLanguage)) {
            store(id, title.trim(), workLanguage.trim());
            return;
        }
        applyThesaurus(id);
        TabSelection tab = tab();
        store(tab.id(),
                StringUtils.isNotBlank(title) ? title.trim() : tab.title(),
                StringUtils.isNotBlank(workLanguage) ? workLanguage.trim() : tab.language());
    }

    private void applyThesaurus(String thesaurusId) {
        ThesaurusSelection selection = thesaurusSelectionService.resolve(thesaurusId);
        if (selection == null) {
            return;
        }
        store(selection.thesaurusId(), selection.title(),
                thesaurusWorkLanguageService.resolveForThesaurus(selection.thesaurusId()));
    }
}
