package fr.cnrs.opentheso.v2.publicapi.group.service;

import fr.cnrs.opentheso.repositories.ConceptGroupLabelRepository;
import fr.cnrs.opentheso.repositories.ConceptGroupRepository;
import fr.cnrs.opentheso.repositories.RelationGroupRepository;
import fr.cnrs.opentheso.v2.concept.io.rdf.ConceptSkosRdfExportEngine;
import fr.cnrs.opentheso.v2.publicapi.group.api.dto.GroupBranchTreeEntryResponse;
import fr.cnrs.opentheso.v2.publicapi.group.api.dto.GroupSummaryResponse;
import fr.cnrs.opentheso.v2.setting.service.ThesaurusWorkLanguageService;
import fr.cnrs.opentheso.v2.shared.io.SkosRdfFormatSupport;
import fr.cnrs.opentheso.v2.shared.io.SkosRdfFormatSupport.ExportResult;
import fr.cnrs.opentheso.v2.toolbox.edition.model.ThesaurusEditionExportOptions;
import fr.cnrs.opentheso.v2.toolbox.edition.persistence.ThesaurusSkosDocumentBuilder;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GroupPublicExportService {

    private static final String PARENT_RELATION = "sub";
    private static final int MAX_PATH_DEPTH = 50;

    private final ThesaurusSkosDocumentBuilder thesaurusSkosDocumentBuilder;
    private final ConceptSkosRdfExportEngine conceptSkosRdfExportEngine;
    private final RelationGroupRepository relationGroupRepository;
    private final ConceptGroupLabelRepository conceptGroupLabelRepository;
    private final ConceptGroupRepository conceptGroupRepository;
    private final ThesaurusWorkLanguageService thesaurusWorkLanguageService;

    public ExportResult exportGroup(String thesaurusId, String groupId, String formatCode) throws IOException {
        try {
            var document = thesaurusSkosDocumentBuilder.buildDocumentByGroup(thesaurusId, groupId, false);
            return serialize(document, thesaurusId + "_" + groupId, formatCode);
        } catch (IOException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("Export SKOS du groupe impossible", ex);
        }
    }

    public ExportResult exportBranch(String thesaurusId, List<String> groupIds, String formatCode) throws IOException {
        try {
            var options = new ThesaurusEditionExportOptions(true, groupIds, false);
            var document = thesaurusSkosDocumentBuilder.buildDocument(thesaurusId, options);
            return serialize(document, thesaurusId + "_branch", formatCode);
        } catch (IOException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("Export SKOS de la branche de groupes impossible", ex);
        }
    }

    public List<GroupBranchTreeEntryResponse> branchTree(String thesaurusId, List<String> groupIds, String lang) {
        String workLang = resolveLang(thesaurusId, lang);
        return groupIds.stream()
                .map(groupId -> new GroupBranchTreeEntryResponse(
                        groupId,
                        labelOf(thesaurusId, groupId, workLang),
                        pathToRoot(thesaurusId, groupId, workLang)
                ))
                .toList();
    }

    public List<GroupSummaryResponse> listGroups(String thesaurusId) {
        return conceptGroupRepository.findAllByIdThesaurus(thesaurusId).stream()
                .map(group -> toSummary(thesaurusId, group.getIdGroup()))
                .toList();
    }

    public List<GroupSummaryResponse> listSubGroups(String thesaurusId, String groupId) {
        return relationGroupRepository.findChildGroupIds(thesaurusId, groupId).stream()
                .map(subGroupId -> toSummary(thesaurusId, subGroupId))
                .toList();
    }

    private GroupSummaryResponse toSummary(String thesaurusId, String groupId) {
        var labels = conceptGroupLabelRepository.findAllByIdThesaurusAndIdGroup(thesaurusId, groupId).stream()
                .map(label -> new GroupSummaryResponse.Translation(label.getLang(), label.getLexicalValue()))
                .toList();
        return new GroupSummaryResponse(groupId, labels);
    }

    private String resolveLang(String thesaurusId, String lang) {
        return StringUtils.isNotBlank(lang) ? lang : thesaurusWorkLanguageService.resolveForThesaurus(thesaurusId);
    }

    private List<GroupBranchTreeEntryResponse.PathStep> pathToRoot(String thesaurusId, String groupId, String lang) {
        List<GroupBranchTreeEntryResponse.PathStep> path = new ArrayList<>();
        String currentId = groupId;
        int depth = 0;
        while (StringUtils.isNotBlank(currentId) && depth < MAX_PATH_DEPTH) {
            var parentRelation = relationGroupRepository
                    .findByIdThesaurusAndIdGroup2AndRelation(thesaurusId, currentId, PARENT_RELATION)
                    .orElse(null);
            if (parentRelation == null) {
                break;
            }
            String parentId = parentRelation.getIdGroup1();
            path.add(new GroupBranchTreeEntryResponse.PathStep(parentId, labelOf(thesaurusId, parentId, lang)));
            currentId = parentId;
            depth++;
        }
        return path;
    }

    private String labelOf(String thesaurusId, String groupId, String lang) {
        return conceptGroupLabelRepository.findAllByIdThesaurusAndIdGroupAndLang(thesaurusId, groupId, lang).stream()
                .findFirst()
                .map(label -> label.getLexicalValue())
                .orElse(groupId);
    }

    private ExportResult serialize(fr.cnrs.opentheso.models.skosapi.SKOSXmlDocument document, String baseName, String formatCode) throws IOException {
        var format = SkosRdfFormatSupport.resolveExportFormat(formatCode);
        byte[] content = conceptSkosRdfExportEngine.serializeSkos(document, format.rdfFormat());
        return new ExportResult(content, baseName + format.extension(), "application/xml");
    }
}
