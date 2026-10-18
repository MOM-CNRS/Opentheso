package fr.cnrs.opentheso.shared.auth;

import fr.cnrs.opentheso.rights.AuthTarget;
import fr.cnrs.opentheso.rights.Permission;
import fr.cnrs.opentheso.rights.RightsService;
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
