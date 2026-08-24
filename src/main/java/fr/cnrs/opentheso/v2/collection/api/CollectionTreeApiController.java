package fr.cnrs.opentheso.v2.collection.api;

import fr.cnrs.opentheso.v2.collection.model.CollectionDetail;
import fr.cnrs.opentheso.v2.collection.model.CollectionTreeNode;
import fr.cnrs.opentheso.v2.collection.read.CollectionTreeConsultationService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping({"/v2/api", "/v2-preview/api"})
public class CollectionTreeApiController {

    private final CollectionTreeConsultationService collectionTreeConsultationService;

    public CollectionTreeApiController(CollectionTreeConsultationService collectionTreeConsultationService) {
        this.collectionTreeConsultationService = collectionTreeConsultationService;
    }

    @GetMapping(value = "/collection-tree/roots", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<CollectionTreeNode> roots(
            @RequestParam(required = false) String thesaurusId,
            @RequestParam(required = false) String lang,
            @RequestParam(required = false, defaultValue = "false") boolean sortByNotation
    ) {
        if (StringUtils.isBlank(thesaurusId)) {
            return List.of();
        }
        return collectionTreeConsultationService.loadRoots(
                thesaurusId,
                StringUtils.firstNonBlank(lang, "fr"),
                sortByNotation
        );
    }

    @GetMapping(value = "/collection-tree/list", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<CollectionTreeNode> list(
            @RequestParam(required = false) String thesaurusId,
            @RequestParam(required = false) String lang,
            @RequestParam(required = false, defaultValue = "false") boolean sortByNotation
    ) {
        if (StringUtils.isBlank(thesaurusId)) {
            return List.of();
        }
        return collectionTreeConsultationService.loadAll(
                thesaurusId,
                StringUtils.firstNonBlank(lang, "fr"),
                sortByNotation
        );
    }

    @GetMapping(value = "/collection-tree/children", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<CollectionTreeNode> children(
            @RequestParam(required = false) String parentId,
            @RequestParam(required = false) String thesaurusId,
            @RequestParam(required = false) String lang,
            @RequestParam(required = false, defaultValue = "false") boolean sortByNotation
    ) {
        if (StringUtils.isAnyBlank(parentId, thesaurusId)) {
            return List.of();
        }
        return collectionTreeConsultationService.loadChildren(
                parentId,
                thesaurusId,
                StringUtils.firstNonBlank(lang, "fr"),
                sortByNotation
        );
    }

    @GetMapping(value = "/collection-tree/detail", produces = MediaType.APPLICATION_JSON_VALUE)
    public CollectionDetail detail(
            @RequestParam(required = false) String groupId,
            @RequestParam(required = false) String thesaurusId,
            @RequestParam(required = false) String lang
    ) {
        if (StringUtils.isAnyBlank(groupId, thesaurusId)) {
            return CollectionDetail.empty();
        }
        return collectionTreeConsultationService.loadDetail(
                thesaurusId,
                groupId,
                StringUtils.firstNonBlank(lang, "fr")
        );
    }
}
