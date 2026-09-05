package fr.cnrs.opentheso.v2.toolbox.persistence;

import fr.cnrs.opentheso.entites.Thesaurus;
import fr.cnrs.opentheso.entites.ThesaurusLabel;
import fr.cnrs.opentheso.entites.UserGroupThesaurus;
import fr.cnrs.opentheso.models.thesaurus.NodeLangTheso;
import fr.cnrs.opentheso.repositories.ThesaurusLabelRepository;
import fr.cnrs.opentheso.repositories.ThesaurusRepository;
import fr.cnrs.opentheso.repositories.UserGroupThesaurusRepository;
import fr.cnrs.opentheso.v2.shared.time.V2Dates;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ToolboxThesaurusPersistence {

    private final ThesaurusRepository thesaurusRepository;
    private final ThesaurusLabelRepository thesaurusLabelRepository;
    private final UserGroupThesaurusRepository userGroupThesaurusRepository;

    public String createThesaurusId() {
        var thesaurusSeq = thesaurusRepository.getNextThesaurusSequenceValue();
        var idThesaurus = "th" + thesaurusSeq;
        while (exists(idThesaurus)) {
            idThesaurus = "th" + ++thesaurusSeq;
        }

        thesaurusRepository.save(Thesaurus.builder()
                .id((int) thesaurusSeq)
                .idThesaurus(idThesaurus)
                .idArk("")
                .isPrivate(true)
                .created(new Date())
                .modified(new Date())
                .build());
        return idThesaurus;
    }

    public boolean exists(String thesaurusId) {
        return thesaurusRepository.findById(thesaurusId).isPresent();
    }

    public void setVisibility(String thesaurusId, boolean privateThesaurus) {
        thesaurusRepository.updateVisibility(thesaurusId, privateThesaurus);
    }

    public void addTranslation(fr.cnrs.opentheso.models.thesaurus.Thesaurus thesaurus) {
        var normalized = normalize(thesaurus);
        thesaurusLabelRepository.save(ThesaurusLabel.builder()
                .idThesaurus(normalized.getId_thesaurus())
                .lang(normalized.getLanguage().trim())
                .title(normalized.getTitle())
                .contributor(normalized.getContributor())
                .coverage(normalized.getCoverage())
                .creator(normalized.getCreator())
                .description(normalized.getDescription())
                .format(normalized.getFormat())
                .publisher(normalized.getPublisher())
                .relation(normalized.getRelation())
                .rights(normalized.getRights())
                .source(normalized.getSource())
                .subject(normalized.getSubject())
                .type(normalized.getType())
                .created(V2Dates.nowDateTime())
                .modified(V2Dates.nowDateTime())
                .build());
    }

    public boolean updateTranslation(fr.cnrs.opentheso.models.thesaurus.Thesaurus thesaurus) {
        var normalized = normalize(thesaurus);
        var thesaurusLabel = thesaurusLabelRepository.findByIdThesaurusAndLang(
                normalized.getId_thesaurus(), normalized.getLanguage());
        if (thesaurusLabel.isEmpty()) {
            return false;
        }
        var entity = thesaurusLabel.get();
        entity.setContributor(normalized.getContributor());
        entity.setCoverage(normalized.getCoverage());
        entity.setCreator(normalized.getCreator());
        entity.setModified(V2Dates.nowDateTime());
        entity.setDescription(normalized.getDescription());
        entity.setFormat(normalized.getFormat());
        entity.setPublisher(normalized.getPublisher());
        entity.setRelation(normalized.getRelation());
        entity.setRights(normalized.getRights());
        entity.setSource(normalized.getSource());
        entity.setSubject(normalized.getSubject());
        entity.setTitle(normalized.getTitle());
        entity.setType(normalized.getType());
        thesaurusLabelRepository.save(entity);
        return true;
    }

    public void deleteTranslation(String thesaurusId, String languageCode) {
        thesaurusLabelRepository.deleteByIdThesaurusAndLang(
                fr.cnrs.opentheso.utils.StringUtils.convertString(thesaurusId), languageCode);
    }

    public void linkToProject(UserGroupThesaurus link) {
        userGroupThesaurusRepository.save(link);
    }

    public List<NodeLangTheso> loadUsedLanguages(String thesaurusId, String workLang) {
        final var langue = org.apache.commons.lang3.StringUtils.isBlank(workLang) ? "fr" : workLang;
        var projections = thesaurusRepository.findAllUsedLanguagesOfThesaurus(thesaurusId);
        if (CollectionUtils.isEmpty(projections)) {
            return List.of();
        }
        return projections.stream()
                .map(element -> NodeLangTheso.builder()
                        .id(element.getId())
                        .code(element.getCode())
                        .codeFlag(element.getCodeFlag())
                        .labelTheso(element.getLabelTheso())
                        .value("fr".equalsIgnoreCase(langue) ? element.getFrenchName() : element.getEnglishName())
                        .build())
                .toList();
    }

    public String findArkId(String thesaurusId) {
        return thesaurusRepository.findById(thesaurusId)
                .map(Thesaurus::getIdArk)
                .orElse("");
    }

    public boolean updateArkId(String thesaurusId, String arkId) {
        var thesaurus = thesaurusRepository.findById(thesaurusId);
        if (thesaurus.isEmpty()) {
            return true;
        }
        thesaurus.get().setIdArk(arkId);
        thesaurusRepository.save(thesaurus.get());
        return false;
    }

    private fr.cnrs.opentheso.models.thesaurus.Thesaurus normalize(
            fr.cnrs.opentheso.models.thesaurus.Thesaurus thesaurus) {
        thesaurus.setContributor(fr.cnrs.opentheso.utils.StringUtils.convertString(thesaurus.getContributor()));
        thesaurus.setCoverage(fr.cnrs.opentheso.utils.StringUtils.convertString(thesaurus.getCoverage()));
        thesaurus.setCreator(fr.cnrs.opentheso.utils.StringUtils.convertString(thesaurus.getCreator()));
        thesaurus.setDescription(fr.cnrs.opentheso.utils.StringUtils.convertString(thesaurus.getDescription()));
        thesaurus.setFormat(fr.cnrs.opentheso.utils.StringUtils.convertString(thesaurus.getFormat()));
        thesaurus.setPublisher(fr.cnrs.opentheso.utils.StringUtils.convertString(thesaurus.getPublisher()));
        thesaurus.setRelation(fr.cnrs.opentheso.utils.StringUtils.convertString(thesaurus.getRelation()));
        thesaurus.setRights(fr.cnrs.opentheso.utils.StringUtils.convertString(thesaurus.getRights()));
        thesaurus.setSource(fr.cnrs.opentheso.utils.StringUtils.convertString(thesaurus.getSource()));
        thesaurus.setSubject(fr.cnrs.opentheso.utils.StringUtils.convertString(thesaurus.getSubject()));
        thesaurus.setTitle(fr.cnrs.opentheso.utils.StringUtils.convertString(thesaurus.getTitle()));
        thesaurus.setType(fr.cnrs.opentheso.utils.StringUtils.convertString(thesaurus.getType()));
        return thesaurus;
    }
}
