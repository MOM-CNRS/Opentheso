package fr.cnrs.opentheso.v2.user.ui;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.cnrs.opentheso.v2.shared.session.AuthenticatedUserSource;
import fr.cnrs.opentheso.v2.user.api.dto.TableColPrefDto;
import fr.cnrs.opentheso.v2.user.model.TableColPref;
import fr.cnrs.opentheso.v2.user.service.TableColPrefService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Component("v2TableColBean")
@Scope(value = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
@RequiredArgsConstructor
public class TableColPrefViewBean {

    private final AuthenticatedUserSource authenticatedUserSource;
    private final TableColPrefService tableColPrefService;
    private final ObjectMapper objectMapper;

    private TableColPref pref;

    public String getSelectedJson() {
        try {
            return objectMapper.writeValueAsString(new TableColPrefDto(new ArrayList<>(pref().selected())));
        } catch (JsonProcessingException e) {
            return "{\"selected\":[]}";
        }
    }

    public boolean isSelected(String colId) {
        return pref().contains(colId);
    }

    public boolean selected(String colId) {
        return isSelected(colId);
    }

    private TableColPref pref() {
        if (pref == null) {
            Integer userId = authenticatedUserSource.getUserId().orElse(null);
            pref = userId == null ? TableColPref.defaults() : tableColPrefService.getPref(userId);
        }
        return pref;
    }
}
