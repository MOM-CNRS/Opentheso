package fr.cnrs.opentheso.v2.concept.service;

import fr.cnrs.opentheso.v2.concept.model.ConsultationProjectOption;
import fr.cnrs.opentheso.v2.concept.model.ConsultationThesaurusOption;
import fr.cnrs.opentheso.v2.project.service.ProjectAdminService;
import fr.cnrs.opentheso.v2.shared.repository.AdminQueryRepository;
import fr.cnrs.opentheso.v2.shared.repository.ConsultationCatalogQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ConsultationCatalogService {

    private final ConsultationCatalogQueryRepository consultationCatalogQueryRepository;
    private final ProjectAdminService projectAdminService;
    private final AdminQueryRepository adminQueryRepository;

    @Value("${settings.workLanguage:fr}")
    private String defaultWorkLanguage;

    @Transactional(readOnly = true)
    public List<ConsultationProjectOption> listProjects(Integer userId, boolean superAdmin) {
        if (userId == null) {
            return consultationCatalogQueryRepository.findPublicProjects();
        }
        return projectAdminService.listAccessibleProjects(userId).stream()
                .map(project -> new ConsultationProjectOption(project.id(), project.name()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ConsultationThesaurusOption> listThesauri(Integer userId, boolean superAdmin, int projectId, String lang) {
        String resolvedLang = lang != null ? lang : defaultWorkLanguage;
        if (userId == null) {
            return consultationCatalogQueryRepository.findPublicThesauri(projectId, resolvedLang);
        }
        if (superAdmin && projectId < 0) {
            return adminQueryRepository.findAllThesauri(resolvedLang).stream()
                    .map(row -> new ConsultationThesaurusOption(
                            row.thesaurusId(),
                            row.title(),
                            resolvedLang
                    ))
                    .toList();
        }
        return consultationCatalogQueryRepository.findAccessibleThesauriForUser(userId, projectId, resolvedLang);
    }

    @Transactional(readOnly = true)
    public List<String> listSearchableThesaurusIds(Integer userId, boolean superAdmin, int projectId, String lang) {
        return listThesauri(userId, superAdmin, projectId, lang).stream()
                .map(ConsultationThesaurusOption::id)
                .toList();
    }
}
