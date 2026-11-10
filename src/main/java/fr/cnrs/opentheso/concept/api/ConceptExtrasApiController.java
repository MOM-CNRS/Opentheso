package fr.cnrs.opentheso.concept.api;

import fr.cnrs.opentheso.concept.model.ConceptCorpusLinkItem;
import fr.cnrs.opentheso.concept.policy.ThesaurusConsultationAccessPolicy;
import fr.cnrs.opentheso.concept.service.ConceptReadService;
import fr.cnrs.opentheso.concept.write.persistence.ConceptLexicalWriteRepository;
import fr.cnrs.opentheso.rights.Permission;
import fr.cnrs.opentheso.rights.RightsService;
import fr.cnrs.opentheso.shared.session.AuthenticatedUserSource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Totaux de fiche calculés hors du rendu JSF : la page s'affiche, puis
 * le navigateur interroge ces deux ressources en parallèle.
 */
@RestController
@RequestMapping("/api/concepts")
public class ConceptExtrasApiController {

    private final ConceptReadService conceptReadService;
    private final ConceptLexicalWriteRepository conceptLexicalWriteRepository;
    private final ThesaurusConsultationAccessPolicy consultationAccessPolicy;
    private final RightsService rightsService;
    private final AuthenticatedUserSource authenticatedUserSource;

    public ConceptExtrasApiController(
            ConceptReadService conceptReadService,
            ConceptLexicalWriteRepository conceptLexicalWriteRepository,
            ThesaurusConsultationAccessPolicy consultationAccessPolicy,
            RightsService rightsService,
            AuthenticatedUserSource authenticatedUserSource
    ) {
        this.conceptReadService = conceptReadService;
        this.conceptLexicalWriteRepository = conceptLexicalWriteRepository;
        this.consultationAccessPolicy = consultationAccessPolicy;
        this.rightsService = rightsService;
        this.authenticatedUserSource = authenticatedUserSource;
    }

    @GetMapping(value = "/branch-count", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BranchCountJson> branchCount(
            @RequestParam(required = false) String thesaurusId,
            @RequestParam(required = false) String conceptId
    ) {
        if (StringUtils.isAnyBlank(thesaurusId, conceptId)) {
            return ResponseEntity.badRequest().build();
        }
        if (!canRead(thesaurusId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        int count = conceptReadService.countBranchConcepts(thesaurusId.trim(), conceptId.trim());
        return ResponseEntity.ok(new BranchCountJson(count));
    }

    @GetMapping(value = "/corpus-links", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CorpusLinksJson> corpusLinks(
            @RequestParam(required = false) String thesaurusId,
            @RequestParam(required = false) String conceptId,
            @RequestParam(required = false) String lang
    ) {
        if (StringUtils.isAnyBlank(thesaurusId, conceptId)) {
            return ResponseEntity.badRequest().build();
        }
        if (!canRead(thesaurusId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        var context = conceptReadService.loadCorpusSearchContext(
                thesaurusId.trim(), conceptId.trim(), lang);
        if (context.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        List<CorpusLinkJson> links = conceptReadService.loadCorpusLinks(thesaurusId.trim(), context.get()).stream()
                .map(ConceptExtrasApiController::toJson)
                .toList();
        return ResponseEntity.ok(new CorpusLinksJson(links));
    }

    @GetMapping(value = "/label-duplicate", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<LabelDuplicateJson> labelDuplicate(
            @RequestParam(required = false) String thesaurusId,
            @RequestParam(required = false) String lang,
            @RequestParam(required = false) String value
    ) {
        if (StringUtils.isAnyBlank(thesaurusId, lang, value)) {
            return ResponseEntity.badRequest().build();
        }
        if (!canRead(thesaurusId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        String id = thesaurusId.trim();
        String language = lang.trim();
        String label = value.trim();
        boolean duplicate = conceptLexicalWriteRepository.existsPrefLabel(label, language, id)
                || conceptLexicalWriteRepository.existsAltLabel(label, language, id);
        return ResponseEntity.ok(new LabelDuplicateJson(duplicate));
    }

    private boolean canRead(String thesaurusId) {
        String id = thesaurusId.trim();
        if (consultationAccessPolicy.canConsult(id, false)) {
            return true;
        }
        Integer userId = authenticatedUserSource.getUserId().orElse(null);
        if (userId == null) {
            return false;
        }
        if (rightsService.can(userId, Permission.SUPER_ADMIN)) {
            return true;
        }
        return rightsService.roleOnThesaurus(userId, id).isPresent();
    }

    private static CorpusLinkJson toJson(ConceptCorpusLinkItem item) {
        return new CorpusLinkJson(
                item.corpusName(),
                item.uriLink(),
                item.noticeCount(),
                item.onlyUriLink()
        );
    }

    public record BranchCountJson(int count) {
    }

    public record LabelDuplicateJson(boolean duplicate) {
    }

    public record CorpusLinksJson(List<CorpusLinkJson> links) {
    }

    public record CorpusLinkJson(String name, String uri, int count, boolean onlyUri) {
    }
}
