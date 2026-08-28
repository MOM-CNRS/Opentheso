package fr.cnrs.opentheso.v2.user.ui;

import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import fr.cnrs.opentheso.v2.shared.ui.V2LocaleBean;
import fr.cnrs.opentheso.v2.user.model.ConceptBlockIds;
import fr.cnrs.opentheso.v2.user.model.ConceptBlockLayout;
import fr.cnrs.opentheso.v2.user.service.ConceptBlockLayoutService;
import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@ViewScoped
@Named("v2ConceptBlockLayoutAccountBean")
@RequiredArgsConstructor
public class ConceptBlockLayoutAccountBean implements Serializable {

    private final UserSession userSession;
    private final V2LocaleBean localeBean;
    private final ConceptBlockLayoutService conceptBlockLayoutService;

    private List<ConceptBlockLayoutRow> rows = List.of();
    private boolean loaded;

    @PostConstruct
    public void init() {
        load();
    }

    public List<ConceptBlockLayoutRow> getRows() {
        if (!loaded) {
            load();
        }
        return rows;
    }

    public void load() {
        Integer userId = userSession.getCurrentUserId();
        try {
            if (userId == null) {
                applyLayout(ConceptBlockLayout.defaults());
            } else {
                applyLayout(conceptBlockLayoutService.getLayout(userId));
            }
        } catch (RuntimeException e) {
            log.warn("Impossible de charger la disposition des blocs concept: {}", e.getMessage());
            applyLayout(ConceptBlockLayout.defaults());
        }
        loaded = true;
    }

    public void moveUp(String blockId) {
        ensureLoaded();
        int index = indexOf(blockId);
        if (index <= 0) {
            return;
        }
        swap(index, index - 1);
        persist();
    }

    public void moveDown(String blockId) {
        ensureLoaded();
        int index = indexOf(blockId);
        if (index < 0 || index >= rows.size() - 1) {
            return;
        }
        swap(index, index + 1);
        persist();
    }

    public void toggleOpen(String blockId) {
        ensureLoaded();
        int index = indexOf(blockId);
        if (index < 0) {
            return;
        }
        ConceptBlockLayoutRow row = rows.get(index);
        row.setOpen(!row.isOpen());
        persist();
    }

    public void resetDefault() {
        Integer userId = userSession.getCurrentUserId();
        if (userId == null) {
            return;
        }
        applyLayout(conceptBlockLayoutService.resetLayout(userId));
        loaded = true;
        MessageUtils.showInformationMessage(localeBean.getMsg("v2.profile.blocks.resetDone"));
    }

    private void persist() {
        Integer userId = userSession.getCurrentUserId();
        if (userId == null) {
            return;
        }
        List<String> order = new ArrayList<>();
        List<String> collapsed = new ArrayList<>();
        for (ConceptBlockLayoutRow row : rows) {
            order.add(row.getId());
            if (!row.isOpen()) {
                collapsed.add(row.getId());
            }
        }
        applyLayout(conceptBlockLayoutService.saveLayout(userId, order, collapsed));
        loaded = true;
    }

    private void applyLayout(ConceptBlockLayout layout) {
        ConceptBlockLayout safe = layout == null ? ConceptBlockLayout.defaults() : layout;
        List<ConceptBlockLayoutRow> next = new ArrayList<>();
        for (String id : safe.order()) {
            next.add(new ConceptBlockLayoutRow(
                    id,
                    localeBean.getMsg("v2.block." + id),
                    !safe.collapsed().contains(id)
            ));
        }
        for (String id : ConceptBlockIds.DEFAULT_ORDER) {
            if (next.stream().noneMatch(row -> id.equals(row.getId()))) {
                next.add(new ConceptBlockLayoutRow(id, localeBean.getMsg("v2.block." + id), true));
            }
        }
        rows = next;
    }

    private void ensureLoaded() {
        if (!loaded) {
            load();
        }
        if (!(rows instanceof ArrayList)) {
            rows = new ArrayList<>(rows);
        }
    }

    private int indexOf(String blockId) {
        for (int i = 0; i < rows.size(); i++) {
            if (rows.get(i).getId().equals(blockId)) {
                return i;
            }
        }
        return -1;
    }

    private void swap(int left, int right) {
        ConceptBlockLayoutRow a = rows.get(left);
        rows.set(left, rows.get(right));
        rows.set(right, a);
    }
}
