package fr.cnrs.opentheso.v2.shared.repository;

import fr.cnrs.opentheso.v2.shared.repository.projection.UserCredentialRow;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class UserAuthQueryRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public Optional<UserCredentialRow> findByUsername(String username) {
        return mapCredential(entityManager.createNativeQuery("""
                SELECT id_user, username, mail, password
                FROM users
                WHERE LOWER(username) = LOWER(:value)
                LIMIT 1
                """)
                .setParameter(NativeQueryParams.VALUE, username)
                .getResultList());
    }

    public Optional<UserCredentialRow> findByMail(String mail) {
        return mapCredential(entityManager.createNativeQuery("""
                SELECT id_user, username, mail, password
                FROM users
                WHERE LOWER(mail) = LOWER(:value)
                LIMIT 1
                """)
                .setParameter(NativeQueryParams.VALUE, mail)
                .getResultList());
    }

    @SuppressWarnings("unchecked")
    private Optional<UserCredentialRow> mapCredential(List<?> rows) {
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        Object[] row = (Object[]) rows.get(0);
        return Optional.of(new UserCredentialRow(
                ((Number) row[0]).intValue(),
                (String) row[1],
                (String) row[2],
                (String) row[3]
        ));
    }
}
