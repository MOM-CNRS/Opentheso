package fr.cnrs.opentheso.v2.shared.repository;

import fr.cnrs.opentheso.v2.shared.persistence.CorpusLinkEntity;
import fr.cnrs.opentheso.v2.shared.persistence.CorpusLinkId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface CorpusLinkJpaRepository extends JpaRepository<CorpusLinkEntity, CorpusLinkId> {

    List<CorpusLinkEntity> findAllByIdThesaurusOrderBySortAsc(String idThesaurus);

    Optional<CorpusLinkEntity> findByIdThesaurusAndCorpusName(String idThesaurus, String corpusName);

    @Modifying
    @Transactional
    void deleteByIdThesaurusAndCorpusName(String idThesaurus, String corpusName);

    @Modifying
    @Transactional
    @Query("UPDATE V2CorpusLink t SET t.corpusName = :corpusName WHERE t.idThesaurus = :idThesaurus AND t.corpusName = :oldCorpusName")
    void updateCorpusName(
            @Param("corpusName") String corpusName,
            @Param("oldCorpusName") String oldCorpusName,
            @Param("idThesaurus") String idThesaurus
    );
}
