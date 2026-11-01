-- Les concepts dépréciés étaient masqués par le filtre de statut enregistré
-- avec l'ancienne sélection par défaut (valide, inséré, candidat).
-- On les réactive une fois pour cette sélection, sans toucher un choix différent.

UPDATE user_tree_status_pref AS deprecated
SET selected = true
WHERE deprecated.status_id = 'deprecie'
  AND deprecated.selected = false
  AND NOT EXISTS (
      SELECT 1
      FROM user_tree_status_pref AS other
      WHERE other.id_user = deprecated.id_user
        AND (
            (other.status_id IN ('valide', 'insere', 'candidat') AND other.selected = false)
            OR (other.status_id = 'rejete' AND other.selected = true)
        )
  );
