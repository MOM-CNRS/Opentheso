package fr.cnrs.opentheso.v2.toolbox.service;

import fr.cnrs.opentheso.models.candidats.DomaineDto;
import fr.cnrs.opentheso.models.statistiques.ConceptStatisticData;
import fr.cnrs.opentheso.models.statistiques.GenericStatistiqueData;
import fr.cnrs.opentheso.models.thesaurus.NodeLangTheso;
import fr.cnrs.opentheso.v2.shared.repository.EditionQueryRepository;
import fr.cnrs.opentheso.v2.toolbox.model.EditionStatistics;
import fr.cnrs.opentheso.v2.toolbox.model.StatisticsSummary;
import fr.cnrs.opentheso.v2.toolbox.persistence.ToolboxStatisticsPersistence;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ThesaurusStatisticsService {

    private final ToolboxStatisticsPersistence toolboxStatisticsPersistence;
    private final EditionQueryRepository editionQueryRepository;

    @Transactional(readOnly = true)
    public List<NodeLangTheso> loadLanguages(String thesaurusId, String workLang) {
        return toolboxStatisticsPersistence.loadUsedLanguages(thesaurusId, workLang);
    }

    @Transactional(readOnly = true)
    public List<DomaineDto> loadCollections(String thesaurusId, String workLang) {
        return toolboxStatisticsPersistence.loadCollections(thesaurusId, workLang);
    }

    @Transactional(readOnly = true)
    public List<GenericStatistiqueData> loadCollectionStatistics(String thesaurusId, String language) {
        return toolboxStatisticsPersistence.loadCollectionStatistics(thesaurusId, language);
    }

    @Transactional(readOnly = true)
    public StatisticsSummary loadSummary(String thesaurusId) {
        int[] stats = editionQueryRepository.countAllConceptStats(thesaurusId);
        Date lastModification = toolboxStatisticsPersistence.loadLastModification(thesaurusId);
        return new StatisticsSummary(
                new EditionStatistics(stats[0], stats[1], stats[2]),
                lastModification
        );
    }

    @Transactional(readOnly = true)
    public List<ConceptStatisticData> loadConceptStatistics(
            String thesaurusId,
            String language,
            Date startDate,
            Date endDate,
            String collectionId,
            String resultLimit
    ) {
        return toolboxStatisticsPersistence.loadConceptStatistics(
                thesaurusId,
                language,
                startDate,
                endDate,
                collectionId,
                resultLimit
        );
    }

    public byte[] exportGenericReport(List<GenericStatistiqueData> rows) {
        return toolboxStatisticsPersistence.exportGenericReport(rows);
    }

    public byte[] exportConceptReport(List<ConceptStatisticData> rows) {
        return toolboxStatisticsPersistence.exportConceptReport(rows);
    }

    public ByteArrayInputStream toStream(byte[] content) {
        return new ByteArrayInputStream(content);
    }
}
