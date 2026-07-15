package fr.cnrs.opentheso.v2.shared.repository;

import fr.cnrs.opentheso.v2.shared.repository.projection.UserCredentialRow;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class UserAuthQueryRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public Optional<UserCredentialRow> findByUsername(String username) {
        return findCredential("username", username);
    }

    public Optional<UserCredentialRow> findByMail(String mail) {
        return findCredential("mail", mail);
    }

    @SuppressWarnings("unchecked")
    private Optional<UserCredentialRow> findCredential(String column, String value) {
        String sql = """
                SELECT id_user, username, mail, password
                FROM users
                WHERE LOWER(%s) = LOWER(:value)
                LIMIT 1
                """.formatted(column);
        var rows = entityManager.createNativeQuery(sql)
                .setParameter("value", value)
                .getResultList();
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
