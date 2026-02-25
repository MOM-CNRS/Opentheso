UPDATE concept c
SET top_concept = false
WHERE c.top_concept = true
  AND EXISTS (
    SELECT 1
    FROM hierarchical_relationship hr
    WHERE hr.id_thesaurus = c.id_thesaurus
      AND (
        (hr.id_concept1 = c.id_concept AND hr.role = 'BT')
            OR
        (hr.id_concept2 = c.id_concept AND hr.role = 'NT')
        )
);