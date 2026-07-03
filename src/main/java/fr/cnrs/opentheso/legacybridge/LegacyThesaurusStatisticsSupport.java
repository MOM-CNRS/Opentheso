package fr.cnrs.opentheso.legacybridge;

import fr.cnrs.opentheso.models.candidats.DomaineDto;
import fr.cnrs.opentheso.models.statistiques.ConceptStatisticData;
import fr.cnrs.opentheso.models.statistiques.GenericStatistiqueData;
import fr.cnrs.opentheso.models.thesaurus.NodeLangTheso;
import fr.cnrs.opentheso.services.ConceptService;
import fr.cnrs.opentheso.services.ThesaurusService;
import fr.cnrs.opentheso.services.statistiques.StatistiqueService;
import fr.cnrs.opentheso.services.statistiques.StatistiquesRapportCSV;
import fr.cnrs.opentheso.v2.toolbox.session.ThesaurusStatisticsLegacySupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

@Component
@RequiredArgsConstructor
public class LegacyThesaurusStatisticsSupport implements ThesaurusStatisticsLegacySupport {

    private final StatistiqueService statistiqueService;
    private final ThesaurusService thesaurusService;
    private final ConceptService conceptService;

    @Override
    public List<NodeLangTheso> loadUsedLanguages(String thesaurusId, String workLang) {
        return thesaurusService.getAllUsedLanguagesOfThesaurusNode(thesaurusId, workLang);
    }

    @Override
    public List<DomaineDto> loadCollections(String thesaurusId, String workLang) {
        return statistiqueService.getListGroupes(thesaurusId, workLang);
    }

    @Override
    public List<GenericStatistiqueData> loadCollectionStatistics(String thesaurusId, String language) {
        return statistiqueService.searchAllCollectionsByThesaurus(thesaurusId, language);
    }

    @Override
    public Date loadLastModification(String thesaurusId) {
        return conceptService.getLastModification(thesaurusId);
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
        return statistiqueService.searchAllConceptsByThesaurus(
                thesaurusId,
                language,
                startDate,
                endDate,
                collectionId,
                resultLimit
        );
    }

    @Override
    public byte[] exportGenericReport(List<GenericStatistiqueData> rows) {
        var report = new StatistiquesRapportCSV();
        report.createGenericStatistiquesRapport(rows);
        return report.getOutput().toByteArray();
    }

    @Override
    public byte[] exportConceptReport(List<ConceptStatisticData> rows) {
        var report = new StatistiquesRapportCSV();
        report.createConceptsStatistiquesRapport(rows);
        return report.getOutput().toByteArray();
    }
}
