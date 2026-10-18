package fr.cnrs.opentheso.shared.session;

import fr.cnrs.opentheso.rights.RightsService;
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
