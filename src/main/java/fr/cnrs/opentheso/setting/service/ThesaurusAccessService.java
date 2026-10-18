package fr.cnrs.opentheso.setting.service;

import fr.cnrs.opentheso.rights.AuthTarget;
import fr.cnrs.opentheso.rights.Permission;
import fr.cnrs.opentheso.rights.RightsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ThesaurusAccessService {

    private final RightsService rightsService;

    @Transactional(readOnly = true)
    public boolean canManageThesaurus(int userId, boolean superAdmin, String thesaurusId) {
        if (superAdmin) {
            return true;
        }
        return rightsService.can(userId, Permission.MANAGE_THESAURUS, AuthTarget.thesaurus(thesaurusId));
    }
}
