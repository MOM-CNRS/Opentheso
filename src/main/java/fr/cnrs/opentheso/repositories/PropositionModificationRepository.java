package fr.cnrs.opentheso.repositories;

import fr.cnrs.opentheso.entites.PropositionModification;
import fr.cnrs.opentheso.models.PropositionProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


public interface PropositionModificationRepository extends JpaRepository<PropositionModification, Integer> {

    List<PropositionModification> findAllByIdThesoAndStatus(String idTheso, String status);

    @Modifying
    @Transactional
    void deleteAllByIdTheso(String idThesaurus);

    @Modifying
    @Transactional
    void deleteAllByIdConceptAndIdTheso(String idConcept, String idThesaurus);

    @Modifying
    @Transactional
    @Query("UPDATE PropositionModification pm SET pm.idTheso = :newThesaurus WHERE pm.idTheso = :oldThesaurus")
    void updateThesaurusId(@Param("newThesaurus") String newThesaurus, @Param("oldThesaurus") String oldThesaurus);

    @Modifying
    @Transactional
    @Query("UPDATE PropositionModification pm SET pm.status = :newStatus WHERE pm.id = :idProposition")
    void updateStatus(@Param("idProposition") Integer idProposition, @Param("newStatus") String newStatus);

    @Query(value = """
        SELECT DISTINCT 
            pro.id,
            pro.id_concept AS idConcept,
            pro.lang,
            pro.id_theso AS idTheso,
            pro.status,
            pro.date,
            pro.nom,
            pro.email,
            pro.commentaire,
            pro.approuve_par AS approuvePar,
            pro.approuve_date AS approuveDate,
            pro.admin_comment AS adminComment,
            term.lexical_value AS lexicalValue
        FROM proposition_modification pro
                JOIN preferred_term pre ON pro.id_concept = pre.id_concept AND pro.id_theso = pre.id_thesaurus
                JOIN term ON pre.id_term = term.id_term AND pre.id_thesaurus = term.id_thesaurus
        WHERE 
            pro.email = :email
            AND pro.id_concept = :idConcept
            AND pro.lang = :lang
            AND pro.status IN ('ENVOYER', 'LU')
        LIMIT 1
    """, nativeQuery = true)
    PropositionProjection findPropositionByEmailConceptLang(
            @Param("email") String email,
            @Param("idConcept") String idConcept,
            @Param("lang") String lang
    );

    @Query(value = """
        SELECT pro.id, pro.id_concept AS idConcept, pro.lang, pro.id_theso AS idTheso, pro.status, pro.date, pro.nom,
            pro.email, pro.commentaire, pro.approuve_par AS approuvePar, pro.approuve_date AS approuveDate,
            pro.admin_comment AS adminComment, t.lexical_value AS lexicalValue, l.code_pays AS codePays
        FROM proposition_modification pro 
                    LEFT JOIN preferred_term pre ON pro.id_concept = pre.id_concept AND pro.id_theso = pre.id_thesaurus
                    LEFT JOIN term t ON pre.id_term = t.id_term AND pro.lang = t.lang AND pro.id_theso = t.id_thesaurus
                    LEFT JOIN languages_iso639 l ON l.iso639_1 = t.lang
        WHERE t.id_thesaurus LIKE :idTheso
        AND pro.status = :status
        ORDER BY pro.id DESC
    """, nativeQuery = true)
    List<PropositionProjection> findAllPropositionsByStatusAndTheso(@Param("status") String status, @Param("idTheso") String idTheso);

}