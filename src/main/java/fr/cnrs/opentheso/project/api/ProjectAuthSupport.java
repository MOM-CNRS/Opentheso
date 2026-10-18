package fr.cnrs.opentheso.project.api;

import fr.cnrs.opentheso.user.api.AccountAuthSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProjectAuthSupport {

    private final AccountAuthSupport accountAuthSupport;

    public int resolveUserId(String xApiKey, String legacyApiKey) {
        return accountAuthSupport.resolveUserId(xApiKey, legacyApiKey);
    }
}
