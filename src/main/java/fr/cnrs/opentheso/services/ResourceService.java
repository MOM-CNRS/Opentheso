package fr.cnrs.opentheso.services;

import fr.cnrs.opentheso.bean.menu.users.CurrentUser;
import fr.cnrs.opentheso.models.FacetProjection;
import fr.cnrs.opentheso.models.NarrowerTreeProjection;
import fr.cnrs.opentheso.models.concept.ConceptIdLabel;
import fr.cnrs.opentheso.models.concept.ConceptImage;
import fr.cnrs.opentheso.models.concept.ConceptLabel;
import fr.cnrs.opentheso.models.concept.ConceptNote;
import fr.cnrs.opentheso.models.concept.ConceptRelation;
import fr.cnrs.opentheso.models.concept.NodeConceptGraph;
import fr.cnrs.opentheso.models.concept.NodeConceptTree;
import fr.cnrs.opentheso.models.concept.NodeFullConcept;
import fr.cnrs.opentheso.models.concept.ResourceGPS;
import fr.cnrs.opentheso.models.skosapi.SKOSProperty;
import fr.cnrs.opentheso.repositories.ResourceRepository;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


@Slf4j
@Service
@AllArgsConstructor
public class ResourceService {

    private final static String SEPARATOR = "##";
    private final static String SUB_SEPARATOR = "@@";

    private final ResourceRepository resourceRepository;
    private final CurrentUser currentUser;


    public List<String> getConceptsTT(String idTheso, String idBT) {
        log.debug("Recherche des top termes de '{}' avec broader '{}'", idTheso, idBT);
        try {
            return resourceRepository.findNarrowersIgnoreFacet(idTheso, idBT);
        } catch (Exception e) {
            log.error("Erreur lors de l'appel à opentheso_get_narrowers_ignorefacet({}, {})", idTheso, idBT, e);
            return Collections.emptyList();
        }
    }

    public NodeFullConcept getFullConcept(String idThesaurus, String idConcept, String idLang, int offset, int step, boolean isPrivate) {

        log.debug("Rechercher toutes les informations du concept {} avec la langue {}", idConcept, idLang);
        var conceptDetails = resourceRepository.getFullConcept(idThesaurus, idConcept, idLang, offset, step, isPrivate);

        if (conceptDetails.isEmpty()) {
            log.debug("Aucun concept n'est trouvé avec l'id {} dans le thésaurus id {}", idConcept, idThesaurus);
            return null;
        }

        NodeFullConcept concept = new NodeFullConcept();
        concept.setUri(conceptDetails.get().getUri());
        concept.setResourceType(SKOSProperty.CONCEPT);
        concept.setConceptType(conceptDetails.get().getConcepttype());
        concept.setResourceStatus(switch (conceptDetails.get().getResourcetype().toUpperCase()) {
            case "DEP" -> SKOSProperty.DEPRECATED;
            case "CA" -> SKOSProperty.CANDIDATE;
            default -> SKOSProperty.CONCEPT;
        });
        concept.setIdentifier(conceptDetails.get().getIdentifier());
        concept.setPermanentId(conceptDetails.get().getPermalinkid());
        concept.setNotation(conceptDetails.get().getNotation());
        concept.setCreated(conceptDetails.get().getCreated() != null
                ? String.valueOf(conceptDetails.get().getCreated())
                : null);
        concept.setModified(conceptDetails.get().getModified() != null
                ? String.valueOf(conceptDetails.get().getModified())
                : null);
        concept.setCreatorName(conceptDetails.get().getCreator());
        concept.setContributorName(getContributors(conceptDetails.get().getContributor()));

        // Labels
        concept.setPrefLabel(getLabel(conceptDetails.get().getPreflabel(), idLang));
        concept.setAltLabels(getLabels(conceptDetails.get().getAltlabel(), idLang));
        concept.setHiddenLabels(getLabels(conceptDetails.get().getHidenlabel(), idLang));
        concept.setPrefLabelsTraduction(getLabelsTraduction(conceptDetails.get().getPreflabelTrad()));
        concept.setAltLabelTraduction(getLabelsTraduction(conceptDetails.get().getAltlabelTrad()));
        concept.setHiddenLabelTraduction(getLabelsTraduction(conceptDetails.get().getHiddenlabelTrad()));

        // Notes
        concept.setDefinitions(getNotes(conceptDetails.get().getDefinition()));
        concept.setExamples(getNotes(conceptDetails.get().getExample()));
        concept.setEditorialNotes(getNotes(conceptDetails.get().getEditorialnote()));
        concept.setChangeNotes(getNotes(conceptDetails.get().getChangenote()));
        concept.setScopeNotes(getNotes(conceptDetails.get().getScopenote()));
        concept.setNotes(getNotes(conceptDetails.get().getNote()));
        concept.setHistoryNotes(getNotes(conceptDetails.get().getHistorynote()));

        // Relations
        concept.setBroaders(getRelations(conceptDetails.get().getBroader()));
        concept.setNarrowers(getRelations(conceptDetails.get().getNarrower()));
        concept.setRelateds(getRelations(conceptDetails.get().getRelated()));

        // Alignments
        concept.setExactMatchs(getFromIdLabel(conceptDetails.get().getExactmatch()));
        concept.setCloseMatchs(getFromIdLabel(conceptDetails.get().getClosematch()));
        concept.setBroadMatchs(getFromIdLabel(conceptDetails.get().getBroadmatch()));
        concept.setRelatedMatchs(getFromIdLabel(conceptDetails.get().getRelatedmatch()));
        concept.setNarrowMatchs(getFromIdLabel(conceptDetails.get().getNarrowmatch()));

        concept.setExternalResources(getFromIdLabel(conceptDetails.get().getExternalresources()));
        concept.setGps(getGps(conceptDetails.get().getGpsdata()));
        concept.setMembres(getFromIdLabel(conceptDetails.get().getMembre()));
        concept.setImages(getImages(conceptDetails.get().getImages()));
        concept.setReplaces(getFromIdLabel(conceptDetails.get().getReplaces()));
        concept.setReplacedBy(getFromIdLabel(conceptDetails.get().getReplacedBy()));
        concept.setFacets(getFromIdLabel(conceptDetails.get().getFacets()));

        return concept;
    }

    private List<ResourceGPS> getGps(String textBrut) {
        List<ResourceGPS> resourceGPSs = new ArrayList<>();
        if (StringUtils.isNotEmpty(textBrut)) {
            String[] tabs = textBrut.split(SEPARATOR);
            for (String tab : tabs) {
                try {
                    String[] element = tab.split(SUB_SEPARATOR);
                    ResourceGPS resourceGPS = new ResourceGPS();
                    resourceGPS.setLatitude(Double.valueOf(element[0]));
                    resourceGPS.setLongitude(Double.valueOf(element[1]));
                    resourceGPS.setPosition(Integer.parseInt(element[2]));
                    resourceGPSs.add(resourceGPS);
                } catch (Exception e) {
                }
            }
        }
        if (CollectionUtils.isNotEmpty(resourceGPSs)) {
            return resourceGPSs;
        }
        return null;
    }

    private List<ConceptImage> getImages(String textBrut) {
        List<ConceptImage> conceptImages = new ArrayList<>();
        if (StringUtils.isNotEmpty(textBrut)) {
            String[] tabs = textBrut.split(SEPARATOR);
            for (String tab : tabs) {
                try {
                    String[] element = tab.split(SUB_SEPARATOR);
                    ConceptImage conceptImage = new ConceptImage();
                    if (StringUtils.isEmpty(element[0]) || "null".equalsIgnoreCase(element[0])) {
                        conceptImage.setImageName("");
                    } else {
                        conceptImage.setImageName(element[0]);
                    }
                    conceptImage.setCopyRight(element[1]);
                    conceptImage.setUri(element[2]);
                    if(element.length > 3)
                        conceptImage.setCreator(element[3]);
                    conceptImages.add(conceptImage);
                } catch (Exception e) {
                }
            }
        }
        if (CollectionUtils.isNotEmpty(conceptImages)) {
            return conceptImages;
        }
        return null;
    }

    private List<ConceptLabel> getLabelsTraduction(String labelBrut) {
        List<ConceptLabel> conceptLabels = new ArrayList<>();

        if (StringUtils.isNotEmpty(labelBrut)) {
            String[] tabs = labelBrut.split(SEPARATOR);

            for (String tab : tabs) {
                try {
                    String[] element = tab.split(SUB_SEPARATOR);
                    ConceptLabel conceptLabel = new ConceptLabel();
                    conceptLabel.setIdTerm(element[0]);
                    conceptLabel.setLabel(element[1]);
                    conceptLabel.setIdLang(element[2]);
                    conceptLabel.setId(Integer.parseInt(element[3]));
                    if(element.length > 4 )
                        conceptLabel.setCodeFlag(element[4]);
                    conceptLabels.add(conceptLabel);
                } catch (Exception e) {
                }
            }
        }
        if (CollectionUtils.isNotEmpty(conceptLabels)) {
     //       Collections.sort(conceptLabels);
            return conceptLabels;
        }
        return null;
    }

    private List<String> getContributors(String textBrut) {
        List<String> contributors = new ArrayList<>();
        if (StringUtils.isNotEmpty(textBrut)) {
            String[] tabs = textBrut.split(SEPARATOR);
            for (String tab : tabs) {
                try {
                    contributors.add(tab);
                } catch (Exception e) {
                }
            }
        }
        if (CollectionUtils.isNotEmpty(contributors)) {
            return contributors;
        }
        return null;
    }

    private ConceptLabel getLabel(String labelBrut, String idLang) {
        ConceptLabel conceptLabel = null;

        if (StringUtils.isNotEmpty(labelBrut)) {
            String[] tabs = labelBrut.split(SEPARATOR);

            for (String tab : tabs) {
                try {
                    String[] element = tab.split(SUB_SEPARATOR);
                    conceptLabel = new ConceptLabel();
                    conceptLabel.setIdLang(idLang);
                    conceptLabel.setLabel(element[0]);
                    conceptLabel.setIdTerm(element[1]);
                    conceptLabel.setId(Integer.parseInt(element[2]));
                } catch (Exception e) {
                }
            }
        }
        return conceptLabel;
    }

    private List<ConceptLabel> getLabels(String labelBrut, String idLang) {
        List<ConceptLabel> conceptLabels = new ArrayList<>();

        if (StringUtils.isNotEmpty(labelBrut)) {
            String[] tabs = labelBrut.split(SEPARATOR);

            for (String tab : tabs) {
                try {
                    String[] element = tab.split(SUB_SEPARATOR);
                    ConceptLabel conceptLabel = new ConceptLabel();
                    conceptLabel.setIdLang(idLang);
                    conceptLabel.setLabel(element[0]);
                    conceptLabel.setIdTerm(element[1]);
                    if(element.length > 2)
                        conceptLabel.setId(Integer.parseInt(element[2]));

                    conceptLabels.add(conceptLabel);
                } catch (Exception e) {
                }
            }
        }
        if (CollectionUtils.isNotEmpty(conceptLabels)) {
            Collections.sort(conceptLabels);
            return conceptLabels;
        }
        return null;
    }

    private List<ConceptNote> getNotes(String textBrut) {
        List<ConceptNote> conceptNotes = new ArrayList<>();


        if (StringUtils.isNotEmpty(textBrut)) {
            String[] tabs = textBrut.split(SEPARATOR);

            for (String tab : tabs) {
                try {
                    String[] element = tab.split(SUB_SEPARATOR);
                    ConceptNote conceptNote = new ConceptNote();
                    conceptNote.setIdNote(Integer.parseInt(element[0]));
                    conceptNote.setLabel(fr.cnrs.opentheso.utils.StringUtils.normalizeStringForXml(element[1]));
                    conceptNote.setIdLang(element[2]);
                    if(element.length > 3)
                        conceptNote.setNoteSource(element[3]);
                    conceptNotes.add(conceptNote);
                } catch (Exception e) {
                }
            }
        }
        if (CollectionUtils.isNotEmpty(conceptNotes)) {
            return conceptNotes;
        }
        return null;
    }

    private List<ConceptRelation> getRelations(String textBrut) {
        Set<ConceptRelation> conceptRelations = new HashSet<>();
        if (StringUtils.isNotEmpty(textBrut)) {
            String[] tabs = textBrut.split(SEPARATOR);
            for (String tab : tabs) {
                try {
                    String[] element = tab.split(SUB_SEPARATOR);
                    ConceptRelation conceptRelation = new ConceptRelation();
                    conceptRelation.setUri(element[0]);
                    conceptRelation.setRole(element[1]);
                    conceptRelation.setIdConcept(element[2]);
                    if(element.length > 3) {
                        conceptRelation.setLabel(element[3]);
                    } else {
                        conceptRelation.setLabel("");
                    }
                    conceptRelations.add(conceptRelation);
                } catch (Exception e) {
                }
            }
        }
        if (CollectionUtils.isNotEmpty(conceptRelations)) {
            List<ConceptRelation> sortedList = new ArrayList<>(conceptRelations);
            Collections.sort(sortedList);
            return sortedList;
        }
        return null;
    }

    private List<ConceptIdLabel> getFromIdLabel(String textBrut) {
        List<ConceptIdLabel> membres = new ArrayList<>();
        if (StringUtils.isNotEmpty(textBrut)) {
            String[] tabs = textBrut.split(SEPARATOR);
            for (String tab : tabs) {
                try {
                    String[] element = tab.split(SUB_SEPARATOR);
                    ConceptIdLabel membre = new ConceptIdLabel();
                    membre.setUri(element[0]);
                    membre.setIdentifier(element[1]);
                    if(element.length > 2) {
                        if (StringUtils.isNotEmpty(element[2]) && !element[2].equalsIgnoreCase("null")) {
                            membre.setLabel(element[2]);
                        } else {
                            membre.setLabel("");
                        }
                    } else {
                        membre.setLabel("");
                    }
                    membres.add(membre);
                } catch (Exception e) {
                }
            }
        }
        if (CollectionUtils.isNotEmpty(membres)) {
            Collections.sort(membres);
            return membres;
        }
        return null;
    }

    public List<NodeConceptGraph> getConceptsTTForGraph(String idThesaurus, String idLang) {

        log.debug("Chargement des TopTerm pour le graphe du thésaurus '{}' (lang='{}')", idThesaurus, idLang);
        var result = resourceRepository.getTopConceptsForGraph(idThesaurus, idLang);
        if(CollectionUtils.isEmpty(result)) {
            log.error("Aucun top term n'est trouvé pour le thésaurus '{}' (lang='{}')", idThesaurus, idLang);
            return Collections.emptyList();
        }

        log.error("{}} top term trouvés pour le thésaurus '{}' (lang='{}')", result.size(), idThesaurus, idLang);
        var topTerms = new ArrayList<>(result.stream()
                .map(element -> NodeConceptGraph.builder()
                        .idConcept(element.getIdconcept())
                        .idThesaurus(idThesaurus)
                        .idLang(idLang)
                        .isTerm(true)
                        .uri(element.getLocalUri())
                        .statusConcept(element.getStatus())
                        .haveChildren(Boolean.TRUE.equals(element.getHavechildren()))
                        .prefLabel(element.getLabel() != null ? element.getLabel() : "")
                        .images(StringUtils.isNotBlank(element.getImage()) ? getExternalResources(element.getImage()) : null)
                        .altLabels(StringUtils.isNotBlank(element.getAltlabel()) ? getLabelsWithoutSub(element.getAltlabel()) : null)
                        .definitions(StringUtils.isNotBlank(element.getDefinition()) ? getLabelsWithoutSub(element.getDefinition()) : null)
                        .build())
                .toList());

        try {
            topTerms.sort(Comparator.naturalOrder());
        } catch (Exception e) {
            topTerms.sort(Comparator.comparing(NodeConceptGraph::getPrefLabel, Comparator.nullsLast(String::compareToIgnoreCase)));
        }
        return topTerms;
    }

    private List<String> getExternalResources(String textBrut) {
        List<String> resources = new ArrayList<>();
        if (StringUtils.isNotEmpty(textBrut)) {
            String[] tabs = textBrut.split(SEPARATOR);
            for (String tab : tabs) {
                try {
                    resources.add(tab);
                } catch (Exception e) {
                }
            }
        }
        if (CollectionUtils.isNotEmpty(resources)) {
            return resources;
        }
        return null;
    }

    private List<String> getLabelsWithoutSub(String labelBrut) {
        List<String> labels = new ArrayList<>();

        if (StringUtils.isNotEmpty(labelBrut)) {
            String[] tabs = labelBrut.split(SEPARATOR);

            for (String tab : tabs) {
                try {
                    labels.add(tab);
                } catch (Exception e) {
                }
            }
        }
        if (CollectionUtils.isNotEmpty(labels)) {
            return labels;
        }
        return null;
    }

    public List<NodeConceptGraph> getConceptsNTForGraph(String idThesaurus, String idConceptBT, String idLang) {

        log.debug("Chargement de la liste de fils pour le concept '{}' du thésaurus '{}' trié par alphabétique", idConceptBT, idThesaurus);
        var result = resourceRepository.getNarrowersForGraph(idThesaurus, idConceptBT, idLang);
        if(CollectionUtils.isEmpty(result)) {
            log.error("Aucun fils n'est trouvé pour le thésaurus '{}' (lang='{}')", idThesaurus, idLang);
            return Collections.emptyList();
        }

        log.debug("{} fils trouvés pour le concept '{}' du thésaurus '{}'", result.size(), idThesaurus, idLang);
        return result.stream()
                .map(element -> NodeConceptGraph.builder()
                        .idConcept(element.getIdconcept2())
                        .idThesaurus(idThesaurus)
                        .idLang(idLang)
                        .isTerm(true)
                        .uri(element.getLocalUri())
                        .statusConcept(element.getStatus())
                        .haveChildren(Boolean.TRUE.equals(element.getHavechildren()))
                        .prefLabel(StringUtils.defaultIfBlank(element.getLabel(), ""))
                        .altLabels(StringUtils.isNotBlank(element.getAltlabel()) ? getLabelsWithoutSub(element.getAltlabel()) : null)
                        .definitions(StringUtils.isNotBlank(element.getDefinition()) ? getLabelsWithoutSub(element.getDefinition()) : null)
                        .images(StringUtils.isNotBlank(element.getImage()) ? getExternalResources(element.getImage()) : null)
                        .build())
                .toList();
    }

    public List<NodeConceptTree> getConceptsNTForTree(String idThesaurus, String idConceptBT, String idLang, boolean isSortByNotation, boolean isPrivate) {

        log.debug("Chargement des enfants + facettes pour le concept '{}' (theso='{}', lang='{}')", idConceptBT, idThesaurus, idLang);
        List<NodeConceptTree> results = new ArrayList<>();

        log.debug("Recherche des concepts fils");
        List<NarrowerTreeProjection> fils = resourceRepository.getNarrowersForTree(idThesaurus, idConceptBT, idLang, isPrivate);
        if (CollectionUtils.isEmpty(fils)) {
            log.debug("Aucun concept fils n'est trouvé pour le concept '{}' (lang='{}')", idThesaurus, idLang);
        } else {
            log.debug("{} concept fils trouvés pour le concept '{}' (lang='{}')", fils.size(), idThesaurus, idLang);
            for (NarrowerTreeProjection p : fils) {
                NodeConceptTree node = new NodeConceptTree();
                node.setIdConcept(p.getIdconcept2());
                node.setIdThesaurus(idThesaurus);
                node.setIdLang(idLang);
                node.setTerm(true);
                node.setNotation(isSortByNotation ? p.getNotation() : "");
                node.setTitle(StringUtils.defaultIfEmpty(p.getLabel(), ""));
                node.setHaveChildren(Boolean.TRUE.equals(p.getHavechildren()));
                node.setStatusConcept(p.getStatus());
                results.add(node);
            }
        }

        // Facets
        log.debug("Recherche des facets présent dans le concept {} (lang = {}) définit dans le thésaurus {}", idConceptBT, idLang, idThesaurus);
        List<FacetProjection> facets = resourceRepository.getFacetsOfConcept(idThesaurus, idConceptBT, idLang);
        if (CollectionUtils.isEmpty(facets)) {
            log.debug("Aucune facets n'est trouvée pour le concept '{}' (lang='{}')", idThesaurus, idLang);
        } else {
            log.debug("{} facets trouvées pour le concept '{}' (lang='{}')", facets.size(), idThesaurus, idLang);
            for (FacetProjection f : facets) {
                NodeConceptTree node = new NodeConceptTree();
                node.setIdConcept(f.getIdFacet());
                node.setIdThesaurus(idThesaurus);
                node.setIdLang(idLang);
                node.setFacet(true);
                node.setNotation("");
                node.setTitle(StringUtils.defaultIfEmpty(f.getLibelle(), ""));
                node.setHaveChildren(Boolean.TRUE.equals(f.getHaveMembers()));
                node.setStatusConcept("");
                results.add(node);
            }
        }

        // Tri
        if (!isSortByNotation) {
            try {
                results.sort(Comparator.naturalOrder());
            } catch (Exception e) {
                results.sort(Comparator.comparing(NodeConceptTree::getTitle, Comparator.nullsLast(String::compareToIgnoreCase)));
            }
        }

        return results;
    }

    public List<ConceptRelation> getListNT(String idTheso, String idConcept, String idLang, int offset, int step) {
        log.debug("Chargement des NT pour le concept '{}', thésaurus '{}', langue '{}', offset={}, step={}",
                idConcept, idTheso, idLang, offset, step);

        boolean isPrivate = currentUser.getNodeUser() != null;
        var result = resourceRepository.getNextNT(idTheso, idConcept, idLang, offset, step, isPrivate);
        if (!result.isEmpty() && result.get(0).getNarrower() != null) {
            return getRelations(result.get(0).getNarrower());
        }
        return Collections.emptyList();
    }
}