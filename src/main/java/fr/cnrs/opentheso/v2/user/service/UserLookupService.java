package fr.cnrs.opentheso.v2.user.service;

import fr.cnrs.opentheso.v2.user.exception.UserNotFoundException;
import fr.cnrs.opentheso.v2.shared.persistence.UserEntity;
import fr.cnrs.opentheso.v2.shared.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserLookupService {

    private final UserProfileRepository userProfileRepository;

    @Transactional(readOnly = true)
    public UserEntity requireEntity(int userId) {
        return userProfileRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }
}
