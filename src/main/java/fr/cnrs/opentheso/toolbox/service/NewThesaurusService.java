package fr.cnrs.opentheso.toolbox.service;

import fr.cnrs.opentheso.entites.ThesaurusDcTerm;
import fr.cnrs.opentheso.entites.UserGroupThesaurus;
import fr.cnrs.opentheso.models.concept.DCMIResource;
import fr.cnrs.opentheso.models.nodes.DcElement;
import fr.cnrs.opentheso.repositories.ThesaurusDcTermRepository;
import fr.cnrs.opentheso.project.policy.ProjectAccessPolicy;
import fr.cnrs.opentheso.shared.repository.EditionQueryRepository;
import fr.cnrs.opentheso.shared.repository.ProjectAdminQueryRepository;
import fr.cnrs.opentheso.shared.repository.ProjectMembershipRepository;
import fr.cnrs.opentheso.shared.time.Dates;
import fr.cnrs.opentheso.toolbox.exception.InvalidToolboxDataException;
import fr.cnrs.opentheso.toolbox.mapper.ToolboxMapper;
import fr.cnrs.opentheso.toolbox.model.NewThesaurusFormOptions;
import fr.cnrs.opentheso.toolbox.model.NewThesaurusRequest;
import fr.cnrs.opentheso.toolbox.persistence.ToolboxPreferencePersistence;
import fr.cnrs.opentheso.toolbox.persistence.ToolboxThesaurusPersistence;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewThesaurusService {

    private static final String DCMI_TYPE_STRING = "string";

    private final EditionQueryRepository editionQueryRepository;
    private final ProjectAdminQueryRepository projectAdminQueryRepository;
    private final ProjectMembershipRepository projectMembershipRepository;
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
        return create(request, creatorName, null);
    }

    @Transactional
    public String create(NewThesaurusRequest request, String creatorName, Integer creatorUserId) {
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
        thesaurus.setPublisher(StringUtils.defaultString(request.organization()));
        thesaurus.setSubject(StringUtils.trimToEmpty(request.subject()));
        toolboxThesaurusPersistence.addTranslation(thesaurus);
        for (String language : possibleLanguages(request)) {
            if (language.equalsIgnoreCase(request.language())) {
                continue;
            }
            var extra = new fr.cnrs.opentheso.models.thesaurus.Thesaurus();
            extra.setCreator(creatorName);
            extra.setContributor(creatorName);
            extra.setId_thesaurus(thesaurusId);
            extra.setTitle(titleFor(request, language));
            extra.setLanguage(language);
            extra.setPublisher(StringUtils.defaultString(request.organization()));
            extra.setSubject(StringUtils.trimToEmpty(request.subject()));
            toolboxThesaurusPersistence.addTranslation(extra);
        }

        if (request.projectId() != null) {
            toolboxThesaurusPersistence.linkToProject(
                    UserGroupThesaurus.builder()
                            .idThesaurus(thesaurusId)
                            .idGroup(request.projectId())
                            .build()
            );
            ensureCreatorMembership(creatorUserId, request.projectId(), thesaurusId);
        }

        toolboxPreferencePersistence.initPreferences(thesaurusId, request.language(), request.master());
        toolboxPreferencePersistence.updatePreferredName(thesaurusId, request.persistentNameThesaurus());
        toolboxThesaurusPersistence.setVisibility(thesaurusId, request.privateThesaurus());
        createAndSaveDcTerm(thesaurusId, DCMIResource.CREATOR, creatorName, "", DCMI_TYPE_STRING);
        createAndSaveDcTerm(thesaurusId, DCMIResource.TITLE, request.title(), request.language(), DCMI_TYPE_STRING);
        createAndSaveDcTerm(thesaurusId, DCMIResource.LANGUAGE, request.language(), "", DCMI_TYPE_STRING);
        if (StringUtils.isNotBlank(request.organization())) {
            createAndSaveDcTerm(
                    thesaurusId,
                    DCMIResource.PUBLISHER,
                    request.organization(),
                    request.language(),
                    DCMI_TYPE_STRING
            );
        }
        if (StringUtils.isNotBlank(request.subject())) {
            createAndSaveDcTerm(
                    thesaurusId,
                    DCMIResource.SUBJECT,
                    request.subject().trim(),
                    request.language(),
                    DCMI_TYPE_STRING
            );
        }
        createAndSaveDcTerm(
                thesaurusId,
                DCMIResource.CREATED,
                DateTimeFormatter.ISO_LOCAL_DATE.format(Dates.nowDate()),
                "",
                "date"
        );

        log.info("Thésaurus {} créé par {}", thesaurusId, creatorName);
        return thesaurusId;
    }

    /**
     * Sans rôle projet, le thésaurus n'apparaît pas dans « Mes thésaurus »
     * (cas typique du super-admin). On pose alors un rôle admin limité.
     */
    private void ensureCreatorMembership(Integer creatorUserId, int projectId, String thesaurusId) {
        if (creatorUserId == null || creatorUserId < 0) {
            return;
        }
        if (projectAdminQueryRepository.findCallerRoleOnProject(creatorUserId, projectId).isPresent()) {
            return;
        }
        if (projectMembershipRepository.hasLimitedRoleOnThesaurus(creatorUserId, thesaurusId)) {
            return;
        }
        projectMembershipRepository.assignLimitedRole(
                creatorUserId,
                ProjectAccessPolicy.ROLE_ADMIN,
                projectId,
                thesaurusId
        );
    }

    private String titleFor(NewThesaurusRequest request, String language) {
        if (request.titles() != null && StringUtils.isNotBlank(language)) {
            for (var entry : request.titles().entrySet()) {
                if (entry.getKey() != null
                        && entry.getKey().equalsIgnoreCase(language)
                        && StringUtils.isNotBlank(entry.getValue())) {
                    return entry.getValue().trim();
                }
            }
        }
        return request.title();
    }

    private List<String> possibleLanguages(NewThesaurusRequest request) {
        LinkedHashSet<String> languages = new LinkedHashSet<>();
        if (request.languages() != null) {
            for (String language : request.languages()) {
                if (StringUtils.isNotBlank(language)) {
                    languages.add(language.trim());
                }
            }
        }
        if (languages.stream().noneMatch(language -> language.equalsIgnoreCase(request.language()))) {
            languages.add(request.language().trim());
        }
        return new ArrayList<>(languages);
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
