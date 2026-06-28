package fr.cnrs.opentheso.v2.toolbox.service;

import fr.cnrs.opentheso.models.candidats.DomaineDto;
import fr.cnrs.opentheso.models.statistiques.ConceptStatisticData;
import fr.cnrs.opentheso.models.statistiques.GenericStatistiqueData;
import fr.cnrs.opentheso.services.ConceptService;
import fr.cnrs.opentheso.services.ThesaurusService;
import fr.cnrs.opentheso.services.statistiques.StatistiqueService;
import fr.cnrs.opentheso.services.statistiques.StatistiquesRapportCSV;
import fr.cnrs.opentheso.v2.shared.repository.EditionQueryRepository;
import fr.cnrs.opentheso.v2.toolbox.model.EditionStatistics;
import fr.cnrs.opentheso.v2.toolbox.model.StatisticsSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ThesaurusStatisticsService {

    private final StatistiqueService statistiqueService;
    private final ThesaurusService thesaurusService;
    private final ConceptService conceptService;
    private final EditionQueryRepository editionQueryRepository;

    @Transactional(readOnly = true)
    public List<fr.cnrs.opentheso.models.thesaurus.NodeLangTheso> loadLanguages(String thesaurusId, String workLang) {
        return thesaurusService.getAllUsedLanguagesOfThesaurusNode(thesaurusId, workLang);
    }

    @Transactional(readOnly = true)
    public List<DomaineDto> loadCollections(String thesaurusId, String workLang) {
        return statistiqueService.getListGroupes(thesaurusId, workLang);
    }

    @Transactional(readOnly = true)
    public List<GenericStatistiqueData> loadCollectionStatistics(String thesaurusId, String language) {
        return statistiqueService.searchAllCollectionsByThesaurus(thesaurusId, language);
    }

    @Transactional(readOnly = true)
    public StatisticsSummary loadSummary(String thesaurusId) {
        int[] stats = editionQueryRepository.countAllConceptStats(thesaurusId);
        Date lastModification = conceptService.getLastModification(thesaurusId);
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
        return statistiqueService.searchAllConceptsByThesaurus(
                thesaurusId,
                language,
                startDate,
                endDate,
                collectionId,
                resultLimit
        );
    }

    public byte[] exportGenericReport(List<GenericStatistiqueData> rows) {
        var report = new StatistiquesRapportCSV();
        report.createGenericStatistiquesRapport(rows);
        return report.getOutput().toByteArray();
    }

    public byte[] exportConceptReport(List<ConceptStatisticData> rows) {
        var report = new StatistiquesRapportCSV();
        report.createConceptsStatistiquesRapport(rows);
        return report.getOutput().toByteArray();
    }

    public ByteArrayInputStream toStream(byte[] content) {
        return new ByteArrayInputStream(content);
    }
}
