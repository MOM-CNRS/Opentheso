package fr.cnrs.opentheso.v2.toolbox.persistence;

import fr.cnrs.opentheso.models.candidats.DomaineDto;
import fr.cnrs.opentheso.models.statistiques.ConceptStatisticData;
import fr.cnrs.opentheso.models.statistiques.GenericStatistiqueData;
import fr.cnrs.opentheso.models.thesaurus.NodeLangTheso;
import fr.cnrs.opentheso.v2.toolbox.session.ThesaurusStatisticsLegacySupport;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

@Primary
@Component
@RequiredArgsConstructor
public class V2NativeThesaurusStatisticsSupport implements ThesaurusStatisticsLegacySupport {

    private final ToolboxStatisticsPersistence toolboxStatisticsPersistence;

    @Override
    public List<NodeLangTheso> loadUsedLanguages(String thesaurusId, String workLang) {
        return toolboxStatisticsPersistence.loadUsedLanguages(thesaurusId, workLang);
    }

    @Override
    public List<DomaineDto> loadCollections(String thesaurusId, String workLang) {
        return toolboxStatisticsPersistence.loadCollections(thesaurusId, workLang);
    }

    @Override
    public List<GenericStatistiqueData> loadCollectionStatistics(String thesaurusId, String language) {
        return toolboxStatisticsPersistence.loadCollectionStatistics(thesaurusId, language);
    }

    @Override
    public Date loadLastModification(String thesaurusId) {
        return toolboxStatisticsPersistence.loadLastModification(thesaurusId);
    }

    @Override
    public List<ConceptStatisticData> loadConceptStatistics(
            String thesaurusId,
            String language,
            Date startDate,
            Date endDate,
            String collectionId,
            String resultLimit
    ) {
        return toolboxStatisticsPersistence.loadConceptStatistics(
                thesaurusId, language, startDate, endDate, collectionId, resultLimit);
    }

    @Override
    public byte[] exportGenericReport(List<GenericStatistiqueData> rows) {
        return toolboxStatisticsPersistence.exportGenericReport(rows);
    }

    @Override
    public byte[] exportConceptReport(List<ConceptStatisticData> rows) {
        return toolboxStatisticsPersistence.exportConceptReport(rows);
    }
}
