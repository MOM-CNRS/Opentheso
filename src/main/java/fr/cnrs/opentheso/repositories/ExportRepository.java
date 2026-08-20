package fr.cnrs.opentheso.repositories;

import fr.cnrs.opentheso.entites.Concept;
import fr.cnrs.opentheso.models.SkosConceptProjection;
import fr.cnrs.opentheso.models.SkosFacetProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Collection;
import java.util.List;


public interface ExportRepository extends JpaRepository<Concept, Integer> {

    @Query(value = """
        SELECT * FROM opentheso_get_facettes(:idTheso, :baseUrl)
        AS (id_facet VARCHAR, lexicalValue VARCHAR, created timestamp with time zone,
             modified timestamp with time zone, lang VARCHAR, id_concept_parent VARCHAR,
             uri_value VARCHAR, definition text, example text, editorialNote text,
             changeNote text, secopeNote text, note text, historyNote text)
        """, nativeQuery = true)
    List<SkosFacetProjection> getAllFacettes(@Param("idTheso") String idTheso, @Param("baseUrl") String baseUrl);

    @Query(value = """
        SELECT * FROM opentheso_get_concepts(:idTheso, :baseUrl)
        AS x(URI text, TYPE varchar, LOCAL_URI text, IDENTIFIER varchar, ARK_ID varchar, 
             prefLab varchar, altLab varchar, altLab_hiden varchar, definition text, example text, 
             editorialNote text, changeNote text, secopeNote text, note text, historyNote text, 
             notation varchar, narrower text, broader text, related text, exactMatch text, 
             closeMatch text, broadMatch text, relatedMatch text, narrowMatch text, gpsData text, 
             membre text, created timestamp with time zone, modified timestamp with time zone, 
             img text, creator text, contributor text, replaces text, replaced_by text, 
             facets text, externalResources text)
        """, nativeQuery = true)
    List<SkosConceptProjection> getAllConcepts(@Param("idTheso") String idTheso, @Param("baseUrl") String baseUrl);

    @Query(value = """
        SELECT * FROM opentheso_get_concepts(:idTheso, :baseUrl)
        AS x(URI text, TYPE varchar, LOCAL_URI text, IDENTIFIER varchar, ARK_ID varchar, 
             prefLab varchar, altLab varchar, altLab_hiden varchar, definition text, example text, 
             editorialNote text, changeNote text, secopeNote text, note text, historyNote text, 
             notation varchar, narrower text, broader text, related text, exactMatch text, 
             closeMatch text, broadMatch text, relatedMatch text, narrowMatch text, gpsData text, 
             membre text, created timestamp with time zone, modified timestamp with time zone, 
             img text, creator text, contributor text, replaces text, replaced_by text, 
             facets text, externalResources text)
        WHERE x.IDENTIFIER IN (:ids)
        """, nativeQuery = true)
    List<SkosConceptProjection> getConceptsByIds(@Param("idTheso") String idTheso,
                                                 @Param("baseUrl") String baseUrl,
                                                 @Param("ids") Collection<String> ids);

    @Query(value = """
        SELECT * FROM opentheso_get_concepts_by_group(:idTheso, :baseUrl, :idGroup)
        AS x(URI text, TYPE varchar, LOCAL_URI text, IDENTIFIER varchar, ARK_ID varchar, 
             prefLab varchar, altLab varchar, altLab_hiden varchar, definition text, example text, 
             editorialNote text, changeNote text, secopeNote text, note text, historyNote text, 
             notation varchar, narrower text, broader text, related text, exactMatch text, 
             closeMatch text, broadMatch text, relatedMatch text, narrowMatch text, gpsData text, 
             membre text, created timestamp with time zone, modified timestamp with time zone, 
             img text, creator text, contributor text, replaces text, replaced_by text, 
             facets text, externalResources text)
        """, nativeQuery = true)
    List<SkosConceptProjection> getAllConceptsByGroup(@Param("idTheso") String idTheso,
                                                      @Param("baseUrl") String baseUrl,
                                                      @Param("idGroup") String idGroup);


}
