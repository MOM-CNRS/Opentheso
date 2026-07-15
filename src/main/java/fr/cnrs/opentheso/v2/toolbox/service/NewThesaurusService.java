package fr.cnrs.opentheso.v2.toolbox.service;

import fr.cnrs.opentheso.entites.UserGroupThesaurus;
import fr.cnrs.opentheso.repositories.ThesaurusDcTermRepository;
import fr.cnrs.opentheso.v2.shared.repository.EditionQueryRepository;
import fr.cnrs.opentheso.v2.shared.repository.ProjectAdminQueryRepository;
import fr.cnrs.opentheso.v2.toolbox.exception.InvalidToolboxDataException;
import fr.cnrs.opentheso.v2.toolbox.mapper.ToolboxMapper;
import fr.cnrs.opentheso.v2.toolbox.model.NewThesaurusFormOptions;
import fr.cnrs.opentheso.v2.toolbox.model.NewThesaurusRequest;
import fr.cnrs.opentheso.v2.toolbox.persistence.ToolboxPreferencePersistence;
import fr.cnrs.opentheso.v2.toolbox.persistence.ToolboxThesaurusPersistence;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fr.cnrs.opentheso.entites.ThesaurusDcTerm;
import fr.cnrs.opentheso.models.concept.DCMIResource;
import fr.cnrs.opentheso.models.nodes.DcElement;

import java.text.SimpleDateFormat;
import java.util.Date;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewThesaurusService {

    private final EditionQueryRepository editionQueryRepository;
    private final ProjectAdminQueryRepository projectAdminQueryRepository;
    private final ToolboxThesaurusPersistence toolboxThesaurusPersistence;
    private final ToolboxPreferencePersistence toolboxPreferencePersistence;
    private final ThesaurusDcTermRepository thesaurusDcTermRepository;

    @Transactional(readOnly = true)
    public NewThesaurusFormOptions loadFormOptions(int userId, boolean superAdmin) {
        var languages = editionQueryRepository.findAllLanguages().stream()
                .map(ToolboxMapper::toLanguageOption)
                .toList();
        var projects = superAdmin
                ? projectAdminQueryRepository.findAllProjects().stream().map(ToolboxMapper::toProjectOption).toList()
                : editionQueryRepository.findAdminProjectsForUser(userId).stream()
                        .map(ToolboxMapper::toProjectOption)
                        .toList();
        return new NewThesaurusFormOptions(languages, projects, superAdmin);
    }

    @Transactional
    public String create(NewThesaurusRequest request, String creatorName) {
        validate(request);

        String thesaurusId = toolboxThesaurusPersistence.createThesaurusId();
        if (thesaurusId == null) {
            throw new InvalidToolboxDataException("Erreur pendant la création");
        }

        var thesaurus = new fr.cnrs.opentheso.models.thesaurus.Thesaurus();
        thesaurus.setCreator(creatorName);
        thesaurus.setContributor(creatorName);
        thesaurus.setId_thesaurus(thesaurusId);
        thesaurus.setTitle(request.title());
        thesaurus.setLanguage(request.language());
        toolboxThesaurusPersistence.addTranslation(thesaurus);

        if (request.projectId() != null) {
            toolboxThesaurusPersistence.linkToProject(
                    UserGroupThesaurus.builder()
                            .idThesaurus(thesaurusId)
                            .idGroup(request.projectId())
                            .build()
            );
        }

        toolboxPreferencePersistence.initPreferences(thesaurusId, request.language());
        createAndSaveDcTerm(thesaurusId, DCMIResource.CREATOR, creatorName, "", "string");
        createAndSaveDcTerm(thesaurusId, DCMIResource.TITLE, request.title(), request.language(), "string");
        createAndSaveDcTerm(thesaurusId, DCMIResource.LANGUAGE, request.language(), "", "string");
        createAndSaveDcTerm(
                thesaurusId,
                DCMIResource.CREATED,
                new SimpleDateFormat("yyyy-MM-dd").format(new Date()),
                "",
                "date"
        );

        log.info("Thésaurus {} créé par {}", thesaurusId, creatorName);
        return thesaurusId;
    }

    private void validate(NewThesaurusRequest request) {
        if (StringUtils.isBlank(request.title())) {
            throw new InvalidToolboxDataException("Le label est obligatoire");
        }
        if (StringUtils.isBlank(request.language())) {
            throw new InvalidToolboxDataException("La langue est obligatoire");
        }
    }

    private void createAndSaveDcTerm(String thesaurusId, String name, String value, String language, String type) {
        DcElement dcElement = new DcElement(name, value, language, type);
        try {
            thesaurusDcTermRepository.save(
                    ThesaurusDcTerm.builder()
                            .idThesaurus(thesaurusId)
                            .name(dcElement.getName())
                            .value(dcElement.getValue())
                            .language(dcElement.getLanguage())
                            .dataType(dcElement.getType())
                            .build()
            );
        } catch (DataIntegrityViolationException e) {
            log.debug("DC Term déjà existant, insertion ignorée : {} {} {}", thesaurusId, name, value);
        }
    }
}
