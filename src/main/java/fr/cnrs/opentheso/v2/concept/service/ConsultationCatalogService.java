package fr.cnrs.opentheso.v2.concept.service;

import fr.cnrs.opentheso.v2.concept.model.ConsultationProjectOption;
import fr.cnrs.opentheso.v2.concept.model.ConsultationThesaurusOption;
import fr.cnrs.opentheso.v2.concept.model.ThesaurusPickerRow;
import fr.cnrs.opentheso.v2.project.service.ProjectAdminService;
import fr.cnrs.opentheso.v2.shared.repository.AdminQueryRepository;
import fr.cnrs.opentheso.v2.shared.repository.ConsultationCatalogQueryRepository;
import fr.cnrs.opentheso.v2.shared.repository.ProjectAdminQueryRepository;
import fr.cnrs.opentheso.v2.shared.time.V2Dates;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ConsultationCatalogService {

    private final ConsultationCatalogQueryRepository consultationCatalogQueryRepository;
    private final ProjectAdminService projectAdminService;
    private final ProjectAdminQueryRepository projectAdminQueryRepository;
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
        return doListThesauri(userId, superAdmin, projectId, lang);
    }

    private List<ConsultationThesaurusOption> doListThesauri(Integer userId, boolean superAdmin, int projectId, String lang) {
        String resolvedLang = lang != null ? lang : defaultWorkLanguage;
        if (userId == null) {
            return consultationCatalogQueryRepository.findPublicThesauri(projectId, resolvedLang);
        }
        // Aligné legacy SelectedTheso.setSelectedProject / getThesaurusOfProject :
        // un projet précis → tous les thésaurus du projet (y compris privés).
        if (projectId >= 0) {
            return projectAdminQueryRepository.findThesauriOfProject(projectId, resolvedLang).stream()
                    .map(row -> new ConsultationThesaurusOption(row.id(), row.title(), resolvedLang))
                    .toList();
        }
        if (superAdmin) {
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
        return doListThesauri(userId, superAdmin, projectId, lang).stream()
                .map(ConsultationThesaurusOption::id)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ThesaurusPickerRow> listPickerThesauri(Integer userId, boolean superAdmin, String lang) {
        String resolvedLang = lang != null ? lang : defaultWorkLanguage;
        return consultationCatalogQueryRepository
                .findPickerThesaurusRows(userId, superAdmin, resolvedLang)
                .stream()
                .map(this::toPickerRow)
                .toList();
    }

    private ThesaurusPickerRow toPickerRow(Object[] row) {
        String access = row[4] != null ? String.valueOf(row[4]) : "public";
        boolean member = "member".equalsIgnoreCase(access);
        LocalDateTime createdAt = V2Dates.toLocalDateTime(row[3]);
        LocalDate created = createdAt == null ? null : createdAt.toLocalDate();
        long terms = row[5] instanceof Number number ? number.longValue() : 0L;
        String langsRaw = row[7] != null ? String.valueOf(row[7]) : "";
        List<String> languages = StringUtils.isBlank(langsRaw)
                ? List.of()
                : Arrays.stream(langsRaw.split(","))
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .toList();
        Integer roleId = row.length > 11 && row[11] instanceof Number number ? number.intValue() : null;
        String roleKey = roleKeyFromId(roleId);
        String roleLabel = member ? roleLabelFromKey(roleKey) : "";
        return new ThesaurusPickerRow(
                (String) row[0],
                row[1] != null ? String.valueOf(row[1]) : (String) row[0],
                access,
                roleLabel,
                roleKey,
                terms,
                created,
                row[6] != null ? String.valueOf(row[6]) : "",
                row[9] != null ? String.valueOf(row[9]) : "",
                row[8] != null ? String.valueOf(row[8]) : "",
                row[10] != null ? String.valueOf(row[10]) : "",
                languages,
                toBoolean(row[2])
        );
    }

    private static String roleKeyFromId(Integer roleId) {
        if (roleId == null) {
            return "";
        }
        return switch (roleId) {
            case 1 -> "superAdmin";
            case 2 -> "admin";
            case 3 -> "manager";
            case 4 -> "contributor";
            default -> "member";
        };
    }

    private static String roleLabelFromKey(String roleKey) {
        return switch (StringUtils.defaultString(roleKey)) {
            case "superAdmin" -> "Super-admin";
            case "admin" -> "Administrateur";
            case "manager" -> "Gestionnaire";
            case "contributor" -> "Contributeur";
            default -> "Membre";
        };
    }

    private static boolean toBoolean(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof Number number) {
            return number.intValue() != 0;
        }
        return false;
    }
}
