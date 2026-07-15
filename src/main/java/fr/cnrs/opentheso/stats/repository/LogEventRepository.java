package fr.cnrs.opentheso.stats.repository;

import fr.cnrs.opentheso.stats.entity.LogEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository d'accès à la table stat_log_event.
 * Les requêtes de statistiques agrégées (top concepts, trafic par période...)
 * sont à ajouter ici au fur et à mesure des besoins des tableaux de bord,
 * via des méthodes @Query dédiées.
 */
@Repository
public interface LogEventRepository extends JpaRepository<LogEvent, Long> {
}
