package fr.cnrs.opentheso.v2.shared.auth;

import fr.cnrs.opentheso.v2.rights.AuthTarget;
import fr.cnrs.opentheso.v2.rights.Permission;
import fr.cnrs.opentheso.v2.rights.RightsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ThesaurusWriteAuthorizationService {

    private final RightsService rightsService;

    public boolean canUserWrite(int userId, String thesaurusId) {
        return rightsService.can(userId, Permission.WRITE_THESAURUS, AuthTarget.thesaurus(thesaurusId));
    }
}
