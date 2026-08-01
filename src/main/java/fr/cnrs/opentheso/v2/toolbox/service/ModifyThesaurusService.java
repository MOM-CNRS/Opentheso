package fr.cnrs.opentheso.v2.toolbox.service;

import fr.cnrs.opentheso.entites.ThesaurusDcTerm;
import fr.cnrs.opentheso.models.concept.DCMIResource;
import fr.cnrs.opentheso.models.thesaurus.Thesaurus;
import fr.cnrs.opentheso.repositories.ThesaurusDcTermRepository;
import fr.cnrs.opentheso.v2.toolbox.persistence.ThesaurusLifecyclePersistence;
import fr.cnrs.opentheso.v2.toolbox.persistence.ToolboxPreferencePersistence;
import fr.cnrs.opentheso.v2.toolbox.persistence.ToolboxThesaurusArkPersistence;
import fr.cnrs.opentheso.v2.toolbox.persistence.ToolboxThesaurusPersistence;
import fr.cnrs.opentheso.v2.shared.repository.EditionQueryRepository;
import fr.cnrs.opentheso.v2.shared.repository.ThesaurusSettingsQueryRepository;
import fr.cnrs.opentheso.v2.shared.repository.projection.EditionCollectionRow;
import fr.cnrs.opentheso.v2.toolbox.exception.InvalidToolboxDataException;
import fr.cnrs.opentheso.v2.toolbox.mapper.ToolboxMapper;
import fr.cnrs.opentheso.v2.toolbox.model.EditionCollectionNode;
import fr.cnrs.opentheso.v2.toolbox.model.EditionMetadata;
import fr.cnrs.opentheso.v2.toolbox.model.EditionThesaurusDetails;
import fr.cnrs.opentheso.v2.toolbox.model.EditionThesaurusLanguage;
import fr.cnrs.opentheso.v2.toolbox.model.LanguageOption;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ModifyThesaurusService {

    private final EditionQueryRepository editionQueryRepository;
    private final ThesaurusSettingsQueryRepository thesaurusSettingsQueryRepository;
    private final ToolboxPreferencePersistence toolboxPreferencePersistence;
    private final ToolboxThesaurusPersistence toolboxThesaurusPersistence;
    private final ToolboxThesaurusArkPersistence toolboxThesaurusArkPersistence;
    private final ThesaurusLifecyclePersistence thesaurusLifecyclePersistence;
    private final ThesaurusDcTermRepository thesaurusDcTermRepository;

    @Value("${settings.workLanguage:fr}")
    private String workLanguage;

    @Transactional(readOnly = true)
    public EditionThesaurusDetails loadDetails(String thesaurusId) {
        return editionQueryRepository.findThesaurusDetails(thesaurusId, workLanguage)
                .map(ToolboxMapper::toThesaurusDetails)
                .orElseThrow(() -> new InvalidToolboxDataException("Thésaurus introuvable"));
    }

    @Transactional(readOnly = true)
    public List<EditionThesaurusLanguage> loadLanguages(String thesaurusId) {
        String sourceLang = toolboxPreferencePersistence.getWorkLanguage(thesaurusId);
        String lang = StringUtils.isBlank(sourceLang) ? workLanguage : sourceLang;
        return thesaurusSettingsQueryRepository.findUsedLanguages(thesaurusId, lang).stream()
                .map(ToolboxMapper::toEditionLanguage)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LanguageOption> loadAllLanguages() {
        return editionQueryRepository.findAllLanguages().stream()
                .map(ToolboxMapper::toLanguageOption)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EditionMetadata> loadMetadata(String thesaurusId) {
        return thesaurusDcTermRepository.findAllByIdThesaurus(thesaurusId).stream()
                .map(term -> {
                    EditionMetadata metadata = new EditionMetadata();
                    metadata.setId(term.getId().intValue());
                    metadata.setName(term.getName());
                    metadata.setValue(term.getValue());
                    metadata.setLanguage(term.getLanguage());
                    metadata.setType(term.getDataType());
                    return metadata;
                })
                .toList();
    }

    public List<String> loadDcmiResources() {
        return new DCMIResource().getAllResources();
    }

    public List<String> loadDcmiTypes() {
        return new DCMIResource().getAllTypes();
    }

    @Transactional(readOnly = true)
    public TreeNode<EditionCollectionNode> loadCollectionTree(String thesaurusId) {
        String sourceLang = toolboxPreferencePersistence.getWorkLanguage(thesaurusId);
        String lang = StringUtils.isBlank(sourceLang) ? workLanguage : sourceLang;
        List<EditionCollectionRow> rows = editionQueryRepository.findCollections(thesaurusId, lang);
        return buildCollectionTree(rows);
    }

    @Transactional
    public void changeSourceLanguage(String thesaurusId, String languageCode) {
        if (StringUtils.isBlank(languageCode)) {
            throw new InvalidToolboxDataException("La langue source est obligatoire");
        }
        if (!toolboxPreferencePersistence.setWorkLanguage(languageCode, thesaurusId)) {
            throw new InvalidToolboxDataException("Erreur pendant la modification de la langue source");
        }
    }

    @Transactional
    public void changeVisibility(String thesaurusId, boolean privateThesaurus) {
        toolboxThesaurusPersistence.setVisibility(thesaurusId, privateThesaurus);
    }

    @Transactional(readOnly = true)
    public boolean isMasterThesaurus(String thesaurusId) {
        return toolboxPreferencePersistence.isMaster(thesaurusId);
    }

    @Transactional
    public void updateMasterRole(String thesaurusId, boolean master) {
        if (StringUtils.isBlank(thesaurusId)) {
            throw new InvalidToolboxDataException("Thésaurus introuvable");
        }
        toolboxPreferencePersistence.updateMasterRole(thesaurusId, master);
    }

    @Transactional
    public String generateArkId(String thesaurusId) {
        return toolboxThesaurusArkPersistence.generateArkIdForThesaurus(thesaurusId);
    }

    @Transactional
    public String changeThesaurusId(String currentId, String newId) {
        if (StringUtils.isBlank(newId)) {
            throw new InvalidToolboxDataException("Le nouvel identifiant est obligatoire");
        }
        if (!thesaurusLifecyclePersistence.changeThesaurusId(currentId, newId)) {
            throw new InvalidToolboxDataException("Erreur de changement d'identifiant, vérifiez que l'identifiant est unique");
        }
        return newId;
    }

    @Transactional
    public void addLanguage(String thesaurusId, String title, String languageCode, String creatorName) {
        if (StringUtils.isBlank(title)) {
            throw new InvalidToolboxDataException("Le label est obligatoire");
        }
        if (StringUtils.isBlank(languageCode)) {
            throw new InvalidToolboxDataException("La langue est obligatoire");
        }
        Thesaurus thesaurus = new Thesaurus();
        thesaurus.setCreator(creatorName);
        thesaurus.setContributor(creatorName);
        thesaurus.setId_thesaurus(thesaurusId);
        thesaurus.setTitle(title);
        thesaurus.setLanguage(languageCode);
        toolboxThesaurusPersistence.addTranslation(thesaurus);
    }

    @Transactional
    public void updateLanguage(String thesaurusId, String languageCode, String title, String creatorName) {
        if (StringUtils.isBlank(title)) {
            throw new InvalidToolboxDataException("Le label est obligatoire");
        }
        Thesaurus thesaurus = new Thesaurus();
        thesaurus.setCreator(creatorName);
        thesaurus.setContributor(creatorName);
        thesaurus.setId_thesaurus(thesaurusId);
        thesaurus.setTitle(title);
        thesaurus.setLanguage(languageCode);
        if (!toolboxThesaurusPersistence.updateTranslation(thesaurus)) {
            throw new InvalidToolboxDataException("Erreur pendant la modification");
        }
    }

    @Transactional
    public void deleteLanguage(String thesaurusId, String languageCode) {
        if (StringUtils.isBlank(languageCode)) {
            throw new InvalidToolboxDataException("Pas de langue sélectionnée");
        }
        toolboxThesaurusPersistence.deleteTranslation(thesaurusId, languageCode);
    }

    @Transactional
    public void saveMetadata(String thesaurusId, EditionMetadata metadata) {
        if (StringUtils.isBlank(metadata.getValue())) {
            throw new InvalidToolboxDataException("La valeur est obligatoire");
        }
        if (metadata.getId() == -1) {
            try {
                var saved = thesaurusDcTermRepository.save(ThesaurusDcTerm.builder()
                        .idThesaurus(thesaurusId)
                        .name(metadata.getName())
                        .value(metadata.getValue())
                        .language(metadata.getLanguage())
                        .dataType(metadata.getType())
                        .build());
                metadata.setId(saved.getId().intValue());
            } catch (DataIntegrityViolationException e) {
                log.debug("DC Term déjà existant, insertion ignorée : {} {} {}", thesaurusId, metadata.getName(), metadata.getValue());
            }
            return;
        }
        var existing = thesaurusDcTermRepository.findById(metadata.getId());
        if (existing.isEmpty()) {
            throw new InvalidToolboxDataException("Métadonnée introuvable");
        }
        var entity = existing.get();
        entity.setName(metadata.getName());
        entity.setLanguage(metadata.getLanguage());
        entity.setValue(metadata.getValue());
        entity.setDataType(metadata.getType());
        try {
            thesaurusDcTermRepository.save(entity);
        } catch (DataIntegrityViolationException e) {
            log.debug("DC Term déjà existant, mise à jour ignorée : {} {} {}", thesaurusId, metadata.getName(), metadata.getValue());
        }
    }

    @Transactional
    public void deleteMetadata(String thesaurusId, int metadataId) {
        thesaurusDcTermRepository.deleteDcElementThesaurus(metadataId, thesaurusId);
    }

    @Transactional
    public void updateCollectionsVisibility(String thesaurusId, List<String> groupIds, boolean isPrivate) {
        editionQueryRepository.updateCollectionsVisibility(thesaurusId, groupIds, isPrivate);
    }

    TreeNode<EditionCollectionNode> buildCollectionTree(List<EditionCollectionRow> rows) {
        Map<String, EditionCollectionNode> nodesById = new LinkedHashMap<>();
        Map<String, List<String>> childrenByParent = new HashMap<>();

        for (EditionCollectionRow row : rows) {
            EditionCollectionNode node = new EditionCollectionNode();
            node.setId(row.id());
            node.setLabel(row.label());
            node.setPrivateCollection(row.privateCollection());
            nodesById.put(row.id(), node);
            if (row.parentId() != null) {
                childrenByParent.computeIfAbsent(row.parentId(), key -> new ArrayList<>()).add(row.id());
            }
        }

        TreeNode<EditionCollectionNode> root = new DefaultTreeNode<>(null, null);
        Set<String> visited = new HashSet<>();
        for (EditionCollectionRow row : rows) {
            if (row.parentId() == null) {
                appendCollectionNode(root, row.id(), nodesById, childrenByParent, visited);
            }
        }
        return root;
    }

    private void appendCollectionNode(
            TreeNode<EditionCollectionNode> parent,
            String nodeId,
            Map<String, EditionCollectionNode> nodesById,
            Map<String, List<String>> childrenByParent,
            Set<String> visited
    ) {
        if (!visited.add(nodeId)) {
            log.warn("Cycle détecté dans l'arbre des collections au nœud {}", nodeId);
            return;
        }
        EditionCollectionNode data = nodesById.get(nodeId);
        if (data == null) {
            return;
        }
        TreeNode<EditionCollectionNode> treeNode = new DefaultTreeNode<>(data, parent);
        for (String childId : childrenByParent.getOrDefault(nodeId, List.of())) {
            appendCollectionNode(treeNode, childId, nodesById, childrenByParent, visited);
        }
    }
}
