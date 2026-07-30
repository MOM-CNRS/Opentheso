package fr.cnrs.opentheso.v2.shared.session;

import fr.cnrs.opentheso.v2.rights.RightsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SessionUserService {

    private final RightsService rightsService;

    public SessionUser load(int userId) {
        return rightsService.capabilities(userId);
    }

    public void invalidate(int userId) {
        rightsService.invalidate(userId);
    }
}
