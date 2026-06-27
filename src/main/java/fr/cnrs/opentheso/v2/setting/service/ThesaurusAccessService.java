package fr.cnrs.opentheso.v2.setting.service;

import fr.cnrs.opentheso.v2.setting.policy.ThesaurusAccessPolicy;
import fr.cnrs.opentheso.v2.shared.repository.ThesaurusSettingsQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ThesaurusAccessService {

    private final ThesaurusSettingsQueryRepository thesaurusSettingsQueryRepository;

    @Transactional(readOnly = true)
    public boolean canManageThesaurus(int userId, boolean superAdmin, String thesaurusId) {
        if (superAdmin) {
            return true;
        }
        return thesaurusSettingsQueryRepository.findEffectiveRoleOnThesaurus(userId, thesaurusId)
                .map(roleId -> ThesaurusAccessPolicy.isThesaurusAdmin(false, roleId))
                .orElse(false);
    }
}
