package fr.cnrs.opentheso.v2.shared.repository;

import fr.cnrs.opentheso.v2.shared.persistence.ProjectEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectRepository extends JpaRepository<ProjectEntity, Integer> {

    @Query("""
        SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END
        FROM V2Project p
        WHERE LOWER(p.label) = LOWER(:label)
    """)
    boolean existsByLabelIgnoreCase(@Param("label") String label);

    @Query("""
        SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END
        FROM V2Project p
        WHERE LOWER(p.label) = LOWER(:label) AND p.id <> :projectId
    """)
    boolean existsByLabelIgnoreCaseExcludingId(@Param("label") String label, @Param("projectId") int projectId);
}
