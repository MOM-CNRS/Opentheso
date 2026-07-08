package fr.cnrs.opentheso.v2.shared.auth;

import fr.cnrs.opentheso.v2.project.policy.ProjectAccessPolicy;
import fr.cnrs.opentheso.v2.shared.repository.ThesaurusSettingsQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ThesaurusWriteAuthorizationService {

    private final ThesaurusSettingsQueryRepository thesaurusSettingsQueryRepository;

    public boolean canUserWrite(int userId, String thesaurusId) {
        return thesaurusSettingsQueryRepository.findEffectiveRoleOnThesaurus(userId, thesaurusId)
                .map(role -> role <= ProjectAccessPolicy.ROLE_ADMIN)
                .orElse(false);
    }
}
