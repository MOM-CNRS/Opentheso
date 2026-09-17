package fr.cnrs.opentheso.repositories;

import fr.cnrs.opentheso.entites.ImageExterne;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


public interface ImagesRepository extends JpaRepository<ImageExterne, Integer> {

    List<ImageExterne> findAllByIdConceptAndIdThesaurus(String idConcept, String idThesaurus);

    @Query("SELECT i.idConcept, i.externalUri FROM ImageExterne i WHERE i.idThesaurus = :idThesaurus AND i.idConcept IN :ids")
    List<Object[]> findConceptUriPairs(@Param("idThesaurus") String idThesaurus, @Param("ids") java.util.Set<String> ids);

    @Modifying
    @Transactional
    void deleteAllByIdThesaurusAndIdConcept(String idThesaurus, String idConcept);

    @Modifying
    @Transactional
    void deleteByIdThesaurusAndIdConceptAndExternalUri(String idThesaurus, String idConcept, String uri);

    @Modifying
    @Transactional
    void deleteByIdThesaurusAndIdConceptAndImageCopyrightIgnoreCase(
            String idThesaurus, String idConcept, String imageCopyright);

    @Modifying
    @Transactional
    void deleteByIdThesaurus(String idThesaurus);

    @Modifying
    @Transactional
    @Query("UPDATE ImageExterne t SET t.idThesaurus = :newIdThesaurus WHERE t.idThesaurus = :oldIdThesaurus")
    void updateThesaurusId(@Param("newIdThesaurus") String newIdThesaurus, @Param("oldIdThesaurus") String oldIdThesaurus);

    /**
     * Native positional {@code CALL}: Hibernate {@code @Procedure} uses named args
     * ({@code id_theso}, {@code identifier}, {@code images_str}) that do not match the
     * PostgreSQL signature ({@code id_thesaurus}, {@code id_concept}, {@code images}).
     */
    @Modifying
    @Transactional
    @Query(value = "CALL opentheso_add_external_images(:idTheso, :identifier, :idUser, CAST(:imagesStr AS text))",
            nativeQuery = true)
    void addExternalImages(@Param("idTheso") String idTheso,
                           @Param("identifier") String identifier,
                           @Param("idUser") int idUser,
                           @Param("imagesStr") String imagesStr);

}
