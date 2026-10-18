package fr.cnrs.opentheso.user.service;

import fr.cnrs.opentheso.user.exception.UserNotFoundException;
import fr.cnrs.opentheso.shared.persistence.UserEntity;
import fr.cnrs.opentheso.shared.repository.UserProfileRepository;
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
