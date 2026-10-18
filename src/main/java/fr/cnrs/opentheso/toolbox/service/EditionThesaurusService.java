package fr.cnrs.opentheso.toolbox.service;

import fr.cnrs.opentheso.shared.repository.EditionQueryRepository;
import fr.cnrs.opentheso.toolbox.exception.InvalidToolboxDataException;
import fr.cnrs.opentheso.toolbox.mapper.ToolboxMapper;
import fr.cnrs.opentheso.toolbox.model.EditionStatistics;
import fr.cnrs.opentheso.toolbox.model.EditionThesaurusSummary;
import fr.cnrs.opentheso.toolbox.persistence.ThesaurusLifecyclePersistence;
import fr.cnrs.opentheso.toolbox.persistence.ToolboxPreferencePersistence;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EditionThesaurusService {

    private final EditionQueryRepository editionQueryRepository;
    private final ThesaurusLifecyclePersistence thesaurusLifecyclePersistence;
    private final ToolboxPreferencePersistence toolboxPreferencePersistence;

    @Value("${settings.workLanguage:fr}")
    private String workLanguage;

    @Transactional(readOnly = true)
    public List<EditionThesaurusSummary> listAdminThesauri(int userId, boolean superAdmin) {
        return editionQueryRepository.findAdminThesauriForUser(userId, superAdmin, workLanguage).stream()
                .map(ToolboxMapper::toThesaurusSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public EditionStatistics loadStatistics(String thesaurusId) {
        int[] stats = editionQueryRepository.countAllConceptStats(thesaurusId);
        return new EditionStatistics(stats[0], stats[1], stats[2]);
    }

    @Transactional
    public void deleteThesaurus(String thesaurusId, boolean deletePerennialIdentifiers) {
        if (StringUtils.isBlank(thesaurusId)) {
            throw new InvalidToolboxDataException("Aucun thésaurus sélectionné");
        }
        if (deletePerennialIdentifiers) {
            thesaurusLifecyclePersistence.deleteAllHandleIds(
                    thesaurusId, toolboxPreferencePersistence.findPreferences(thesaurusId));
        }
        thesaurusLifecyclePersistence.deleteRights(thesaurusId);
        if (!thesaurusLifecyclePersistence.deleteThesaurus(thesaurusId)) {
            throw new InvalidToolboxDataException("Erreur pendant la suppression");
        }
        log.info("Thésaurus {} supprimé", thesaurusId);
    }
}
