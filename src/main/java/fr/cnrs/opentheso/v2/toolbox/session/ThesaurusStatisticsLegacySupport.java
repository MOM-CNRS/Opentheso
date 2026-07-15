package fr.cnrs.opentheso.v2.toolbox.session;

import fr.cnrs.opentheso.models.candidats.DomaineDto;
import fr.cnrs.opentheso.models.statistiques.ConceptStatisticData;
import fr.cnrs.opentheso.models.statistiques.GenericStatistiqueData;
import fr.cnrs.opentheso.models.thesaurus.NodeLangTheso;

import java.util.Date;
import java.util.List;

public interface ThesaurusStatisticsLegacySupport {

    List<NodeLangTheso> loadUsedLanguages(String thesaurusId, String workLang);

    List<DomaineDto> loadCollections(String thesaurusId, String workLang);

    List<GenericStatistiqueData> loadCollectionStatistics(String thesaurusId, String language);

    Date loadLastModification(String thesaurusId);

    List<ConceptStatisticData> loadConceptStatistics(
            String thesaurusId,
            String language,
            Date startDate,
            Date endDate,
            String collectionId,
            String resultLimit
    );

    byte[] exportGenericReport(List<GenericStatistiqueData> rows);

    byte[] exportConceptReport(List<ConceptStatisticData> rows);
}
