package fr.cnrs.opentheso.v2.user.ui;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.v2.shared.repository.ConceptQueryRepository;
import fr.cnrs.opentheso.v2.shared.session.AuthenticatedUserSource;
import fr.cnrs.opentheso.v2.user.api.dto.TreeStatusPrefDto;
import fr.cnrs.opentheso.v2.user.model.TreeStatusIds;
import fr.cnrs.opentheso.v2.user.model.TreeStatusPref;
import fr.cnrs.opentheso.v2.user.service.TreeStatusPrefService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component("v2TreeStatusBean")
@Scope(value = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
@RequiredArgsConstructor
public class TreeStatusPrefViewBean {

    private final AuthenticatedUserSource authenticatedUserSource;
    private final TreeStatusPrefService treeStatusPrefService;
    private final ConceptQueryRepository conceptQueryRepository;
    private final ThesaurusContext thesaurusContext;
    private final ObjectMapper objectMapper;

    private TreeStatusPref pref;
    private Map<String, Integer> counts;

    public String getSelectedJson() {
        try {
            return objectMapper.writeValueAsString(new TreeStatusPrefDto(new ArrayList<>(pref().selected())));
        } catch (JsonProcessingException e) {
            return "{\"selected\":[]}";
        }
    }

    public boolean isSelected(String statusId) {
        return pref().contains(statusId);
    }

    public boolean selected(String statusId) {
        return isSelected(statusId);
    }

    public boolean isGroupOn(String group) {
        return selectedInGroup(group) > 0;
    }

    public boolean groupOn(String group) {
        return isGroupOn(group);
    }

    public boolean isGroupMixed(String group) {
        List<String> statuses = TreeStatusIds.GROUPS.getOrDefault(group, List.of());
        int on = selectedInGroup(group);
        return on > 0 && on < statuses.size();
    }

    public boolean groupMixed(String group) {
        return isGroupMixed(group);
    }

    public int count(String statusId) {
        return counts().getOrDefault(statusId, 0);
    }

    public int groupCount(String group) {
        int total = 0;
        for (String statusId : TreeStatusIds.GROUPS.getOrDefault(group, List.of())) {
            total += count(statusId);
        }
        return total;
    }

    private int selectedInGroup(String group) {
        int on = 0;
        for (String statusId : TreeStatusIds.GROUPS.getOrDefault(group, List.of())) {
            if (pref().contains(statusId)) {
                on++;
            }
        }
        return on;
    }

    private TreeStatusPref pref() {
        if (pref == null) {
            Integer userId = authenticatedUserSource.getUserId().orElse(null);
            pref = userId == null ? TreeStatusPref.defaults() : treeStatusPrefService.getPref(userId);
        }
        return pref;
    }

    private Map<String, Integer> counts() {
        if (counts == null) {
            String thesaurusId = thesaurusContext.getCurrentThesaurusId();
            counts = StringUtils.isBlank(thesaurusId)
                    ? new HashMap<>()
                    : new HashMap<>(conceptQueryRepository.countConceptsByUiStatus(thesaurusId));
        }
        return counts;
    }
}
