package fr.cnrs.opentheso.v2.user.ui;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.cnrs.opentheso.v2.shared.session.AuthenticatedUserSource;
import fr.cnrs.opentheso.v2.user.api.dto.ConceptBlockLayoutDto;
import fr.cnrs.opentheso.v2.user.model.ConceptBlockLayout;
import fr.cnrs.opentheso.v2.user.service.ConceptBlockLayoutService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Component("v2ConceptBlockLayoutBean")
@Scope(value = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
@RequiredArgsConstructor
public class ConceptBlockLayoutViewBean {

    private final AuthenticatedUserSource authenticatedUserSource;
    private final ConceptBlockLayoutService conceptBlockLayoutService;
    private final ObjectMapper objectMapper;

    public boolean isEditable() {
        return authenticatedUserSource.isLoggedIn();
    }

    public String getLayoutJson() {
        Integer userId = authenticatedUserSource.getUserId().orElse(null);
        ConceptBlockLayout layout = userId == null
                ? ConceptBlockLayout.defaults()
                : conceptBlockLayoutService.getLayout(userId);
        try {
            return objectMapper.writeValueAsString(new ConceptBlockLayoutDto(
                    layout.order(),
                    new ArrayList<>(layout.collapsed())
            ));
        } catch (JsonProcessingException e) {
            return "{\"order\":[],\"collapsed\":[]}";
        }
    }
}
