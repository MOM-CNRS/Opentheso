package fr.cnrs.opentheso.v2.shared.session;

import fr.cnrs.opentheso.v2.shared.auth.UserCapabilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SessionUserService {

    private final UserCapabilityService userCapabilityService;

    public SessionUser load(int userId) {
        return userCapabilityService.loadSessionUser(userId);
    }
}
