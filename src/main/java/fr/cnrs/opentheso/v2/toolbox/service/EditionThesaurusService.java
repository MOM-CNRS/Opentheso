package fr.cnrs.opentheso.v2.toolbox.service;

import fr.cnrs.opentheso.v2.toolbox.session.EditionThesaurusLegacySupport;
import fr.cnrs.opentheso.v2.shared.repository.EditionQueryRepository;
import fr.cnrs.opentheso.v2.toolbox.exception.InvalidToolboxDataException;
import fr.cnrs.opentheso.v2.toolbox.mapper.ToolboxMapper;
import fr.cnrs.opentheso.v2.toolbox.model.EditionStatistics;
import fr.cnrs.opentheso.v2.toolbox.model.EditionThesaurusSummary;
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
    private final EditionThesaurusLegacySupport editionThesaurusLegacySupport;

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
            editionThesaurusLegacySupport.deleteAllHandleIds(thesaurusId);
        }
        editionThesaurusLegacySupport.deleteRights(thesaurusId);
        if (!editionThesaurusLegacySupport.deleteThesaurus(thesaurusId)) {
            throw new InvalidToolboxDataException("Erreur pendant la suppression");
        }
        log.info("Thésaurus {} supprimé", thesaurusId);
    }
}
