package fr.cnrs.opentheso.bean.session;

import fr.cnrs.opentheso.bean.menu.theso.SelectedTheso;
import fr.cnrs.opentheso.bean.rightbody.viewconcept.ConceptView;
import fr.cnrs.opentheso.utils.MessageUtils;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Named;
import lombok.Data;
import org.primefaces.PrimeFaces;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@Named
@SessionScoped
public class SessionManager implements Serializable {

    // Map tabId -> état de l'onglet
    private Map<String, TabState> tabStates = new HashMap<>();

    // Appelé au chargement ou à l'initialisation de l'onglet
    public void registerTab() {
        String tabId = FacesUtils.getRequestParam("tabId");

        SelectedTheso selectedTheso = SpringUtils.getBean(SelectedTheso.class);
        ConceptView conceptView = SpringUtils.getBean(ConceptView.class);

        if (selectedTheso != null && conceptView != null &&
                selectedTheso.getCurrentIdTheso() != null &&
                conceptView.getNodeConcept() != null &&
                conceptView.getNodeConcept().getConcept() != null) {

            String thesoId = selectedTheso.getCurrentIdTheso();
            String conceptId = conceptView.getNodeConcept().getConcept().getIdConcept();

            tabStates.put(tabId, new TabState(thesoId, conceptId));
        }
    }

    // Appelé lorsque l'utilisateur revient sur l'onglet
    public void checkTabState() {
        String tabId = FacesUtils.getRequestParam("tabId");

        TabState state = tabStates.get(tabId);
        if (state == null) return;

        SelectedTheso selectedTheso = SpringUtils.getBean(SelectedTheso.class);
        ConceptView conceptView = SpringUtils.getBean(ConceptView.class);

        if (selectedTheso == null || conceptView == null ||
                selectedTheso.getCurrentIdTheso() == null ||
                conceptView.getNodeConcept() == null ||
                conceptView.getNodeConcept().getConcept() == null) {
            return;
        }

        String currentTheso = selectedTheso.getCurrentIdTheso();
        String currentConcept = conceptView.getNodeConcept().getConcept().getIdConcept();

        // Comparaison : si theso ou concept a changé
        if (!state.getThesoId().equals(currentTheso) ||
                !state.getConceptId().equals(currentConcept)) {

            MessageUtils.showWarnMessage(
                    "Attention !! un autre onglet utilise un autre thésaurus ou concept..."
            );

            PrimeFaces.current().executeScript("location.reload()");
        }
    }
}
