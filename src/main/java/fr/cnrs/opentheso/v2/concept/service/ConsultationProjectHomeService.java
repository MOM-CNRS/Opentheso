package fr.cnrs.opentheso.v2.concept.service;

import fr.cnrs.opentheso.entites.LanguageIso639;
import fr.cnrs.opentheso.entites.ProjectDescription;
import fr.cnrs.opentheso.repositories.LanguageRepository;
import fr.cnrs.opentheso.repositories.ProjectDescriptionRepository;
import fr.cnrs.opentheso.v2.concept.model.ConsultationProjectLangOption;
import fr.cnrs.opentheso.v2.concept.model.ConsultationProjectThesaurusItem;
import fr.cnrs.opentheso.v2.concept.model.ConsultationThesaurusOption;
import fr.cnrs.opentheso.v2.shared.repository.ThesaurusHomeQueryRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ConsultationProjectHomeService {

    private final ProjectDescriptionRepository projectDescriptionRepository;
    private final LanguageRepository languageRepository;
    private final ThesaurusHomeQueryRepository thesaurusHomeQueryRepository;
    private final ConsultationCatalogService consultationCatalogService;

    @Value("${settings.workLanguage:fr}")
    private String defaultWorkLanguage;

    @Transactional(readOnly = true)
    public List<ConsultationProjectThesaurusItem> listThesauriWithCounts(
            Integer userId,
            boolean superAdmin,
            int projectId,
            String lang
    ) {
        List<ConsultationThesaurusOption> options = consultationCatalogService.listThesauri(
                userId, superAdmin, projectId, lang);
        return options.stream()
                .map(option -> new ConsultationProjectThesaurusItem(
                        option.id(),
                        option.title(),
                        thesaurusHomeQueryRepository.countValidConcepts(option.id())
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ConsultationProjectLangOption> listDescriptionLanguages(int projectId) {
        return languageRepository.findLanguagesByProject(String.valueOf(projectId)).stream()
                .map(this::toLangOption)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ConsultationProjectLangOption> listAllLanguages() {
        return languageRepository.findAllOrderByCodePays().stream()
                .map(this::toLangOption)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<ProjectDescription> findDescription(int projectId, String lang) {
        if (StringUtils.isBlank(lang)) {
            return Optional.empty();
        }
        return projectDescriptionRepository.findByIdGroupAndLang(String.valueOf(projectId), lang);
    }

    @Transactional(readOnly = true)
    public Optional<ProjectDescription> resolveDescription(int projectId, String preferredLang) {
        String lang = StringUtils.isNotBlank(preferredLang) ? preferredLang : defaultWorkLanguage;
        Optional<ProjectDescription> preferred = findDescription(projectId, lang);
        if (preferred.isPresent()) {
            return preferred;
        }
        List<ConsultationProjectLangOption> langs = listDescriptionLanguages(projectId);
        if (langs.isEmpty()) {
            return Optional.empty();
        }
        return findDescription(projectId, langs.get(0).iso6391());
    }

    @Transactional
    public ProjectDescription saveDescription(int projectId, String lang, String html) {
        ProjectDescription entity = projectDescriptionRepository
                .findByIdGroupAndLang(String.valueOf(projectId), lang)
                .orElseGet(() -> {
                    ProjectDescription created = new ProjectDescription();
                    created.setIdGroup(String.valueOf(projectId));
                    created.setLang(lang);
                    return created;
                });
        entity.setLang(lang);
        entity.setDescription(html);
        return projectDescriptionRepository.save(entity);
    }

    @Transactional
    public void deleteDescription(ProjectDescription description) {
        if (description == null || description.getId() == null) {
            return;
        }
        projectDescriptionRepository.delete(description);
    }

    public String resolveCountryCode(String iso6391, List<ConsultationProjectLangOption> allLangs) {
        if (StringUtils.isBlank(iso6391) || allLangs == null) {
            return null;
        }
        return allLangs.stream()
                .filter(lang -> iso6391.equalsIgnoreCase(lang.iso6391()))
                .map(ConsultationProjectLangOption::countryCode)
                .filter(StringUtils::isNotBlank)
                .findFirst()
                .orElse(null);
    }

    private ConsultationProjectLangOption toLangOption(LanguageIso639 language) {
        return new ConsultationProjectLangOption(
                language.getIso6391(),
                language.getFrenchName(),
                language.getEnglishName(),
                language.getCodePays()
        );
    }
}
