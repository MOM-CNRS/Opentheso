package fr.cnrs.opentheso.services.exports;

import fr.cnrs.opentheso.bean.importexport.ExportFileBean;
import fr.cnrs.opentheso.bean.leftbody.viewtree.Tree;
import fr.cnrs.opentheso.entites.Preferences;
import fr.cnrs.opentheso.models.SkosConceptProjection;
import fr.cnrs.opentheso.models.SkosFacetProjection;
import fr.cnrs.opentheso.models.group.NodeGroup;
import fr.cnrs.opentheso.models.group.NodeGroupLabel;
import fr.cnrs.opentheso.models.nodes.NodeIdValue;
import fr.cnrs.opentheso.models.nodes.NodeImage;
import fr.cnrs.opentheso.models.nodes.NodeTree;
import fr.cnrs.opentheso.models.skosapi.SKOSGPSCoordinates;
import fr.cnrs.opentheso.models.skosapi.SKOSProperty;
import fr.cnrs.opentheso.models.skosapi.SKOSRelation;
import fr.cnrs.opentheso.models.skosapi.SKOSResource;
import fr.cnrs.opentheso.models.concept.NodeUri;
import fr.cnrs.opentheso.models.skosapi.SKOSXmlDocument;
import fr.cnrs.opentheso.models.thesaurus.NodeLangTheso;
import fr.cnrs.opentheso.repositories.ExportRepository;
import fr.cnrs.opentheso.services.ConceptService;
import fr.cnrs.opentheso.services.FacetService;
import fr.cnrs.opentheso.services.GroupService;
import fr.cnrs.opentheso.services.PreferenceService;
import fr.cnrs.opentheso.services.exports.csv.CsvWriteHelper;
import fr.cnrs.opentheso.services.exports.pdf.PdfExportType;
import fr.cnrs.opentheso.services.exports.pdf.WritePdfNewGen;
import fr.cnrs.opentheso.services.exports.rdf4j.ExportRdf4jHelperNew;
import fr.cnrs.opentheso.services.exports.rdf4j.WriteRdf4j;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.jena.sparql.function.library.leviathan.log;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.jsoup.Jsoup;
import org.primefaces.PrimeFaces;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import org.springframework.stereotype.Service;
import org.apache.commons.lang3.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExportService {

    private static final String SEPARATOR = "##";
    private static final String SUB_SEPARATOR = "@@";

    private final ExportRepository exportRepository;
    private final FacetService facetService;
    private final ConceptService conceptService;
    private final PreferenceService preferenceService;
    private final ExportRdf4jHelperNew exportRdf4jHelperNew;
    private final GroupService groupService;
    private final CsvWriteHelper csvWriteHelper;
    private final Tree tree;
    private final WritePdfNewGen writePdfNewGen;

    private int sizeOfTheso;
    private int posJ = 0;
    private int posX = 0;


    public List<SKOSResource> getAllFacettes(String idThesaurus, String baseUrl, String originalUri, Preferences nodePreference) throws Exception {

        var projections = exportRepository.getAllFacettes(idThesaurus, baseUrl);
        List<SKOSResource> result = new ArrayList<>();

        for (SkosFacetProjection p : projections) {
            boolean isExist = false;
            for (SKOSResource skosResource : result) {
                var uri = getUriForFacette(p.getId_facet(), idThesaurus, originalUri);
                if (uri.equalsIgnoreCase(skosResource.getUri())) {
                    skosResource.addLabel(p.getLexicalvalue(), p.getLang(), SKOSProperty.PREF_LABEL);
                    isExist = true;
                    break;
                }
            }

            if (!isExist) {
                var resource = new SKOSResource(getUriForFacette(p.getId_facet(), idThesaurus, originalUri), SKOSProperty.FACET);
                resource.setIdentifier(p.getId_facet());
                resource.addRelation(p.getId_concept_parent(), p.getUri_value(), SKOSProperty.SUPER_ORDINATE);

                List<String> members = facetService.getAllMembersOfFacet(p.getId_facet(), idThesaurus);
                for (String idConcept : members) {
                    NodeUri nodeUri = conceptService.getNodeUriOfConcept(idConcept, idThesaurus);
                    resource.addRelation(nodeUri.getIdConcept(), buildUri(nodeUri, idThesaurus, idConcept, originalUri, nodePreference), SKOSProperty.MEMBER);
                }

                resource.addLabel(p.getLexicalvalue(), p.getLang(), SKOSProperty.PREF_LABEL);

                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                resource.addDate(dateFormat.format(p.getCreated()), SKOSProperty.CREATED);
                resource.addDate(dateFormat.format(p.getModified()), SKOSProperty.MODIFIED);

                addDoc(p.getDefinition(), resource, SKOSProperty.DEFINITION);
                addDoc(p.getNote(), resource, SKOSProperty.NOTE);
                addDoc(p.getEditorialnote(), resource, SKOSProperty.EDITORIAL_NOTE);
                addDoc(p.getSecopenote(), resource, SKOSProperty.SCOPE_NOTE);
                addDoc(p.getHistorynote(), resource, SKOSProperty.HISTORY_NOTE);
                addDoc(p.getExample(), resource, SKOSProperty.EXAMPLE);
                addDoc(p.getChangenote(), resource, SKOSProperty.CHANGE_NOTE);

                result.add(resource);
            }
        }
        return result;
    }

    public List<SKOSResource> getAllConcepts(String idThesaurus, String baseUrl, String idGroup, String originalUri,
                                             Preferences nodePreference, boolean filterHtmlCharacter) throws Exception {

        List<SkosConceptProjection> projections = StringUtils.isEmpty(idGroup)
                ? exportRepository.getAllConcepts(idThesaurus, baseUrl)
                : exportRepository.getAllConceptsByGroup(idThesaurus, baseUrl, idGroup);

        List<SKOSResource> result = new ArrayList<>();

        for (SkosConceptProjection p : projections) {
            SKOSResource sKOSResource = new SKOSResource();
            sKOSResource.setProperty(SKOSProperty.CONCEPT);
            sKOSResource.setUri(p.getUri());
            sKOSResource.setLocalUri(p.getLocal_uri());

            sKOSResource.addIdentifier(p.getIdentifier(), SKOSProperty.IDENTIFIER);

            if (!StringUtils.isEmpty(p.getArk_id())) {
                sKOSResource.setArkId(p.getArk_id());
            }

            setStatusOfConcept(p.getType(), sKOSResource);

            getLabels(p.getPrefLab(), sKOSResource, SKOSProperty.PREF_LABEL);
            getLabels(p.getAltLab_hiden(), sKOSResource, SKOSProperty.HIDDEN_LABEL);
            getLabels(p.getAltLab(), sKOSResource, SKOSProperty.ALT_LABEL);

            if (StringUtils.isEmpty(p.getBroader())) {
                sKOSResource.getRelationsList().add(new SKOSRelation(idThesaurus, getUriThesoFromId(idThesaurus, originalUri, nodePreference),
                        SKOSProperty.TOP_CONCEPT_OF));
            }

            addRelationsGiven(p.getRelated(), sKOSResource);

            var note = p.getDefinition();
            if(StringUtils.isNotEmpty(note)){
                if(filterHtmlCharacter)
                    note = Jsoup.parse(note).text();
                addDocumentation(note, sKOSResource, SKOSProperty.DEFINITION);
            }

            note = p.getNote();
            if(StringUtils.isNotEmpty(note)){
                if(filterHtmlCharacter)
                    note = Jsoup.parse(note).text();
                addDocumentation(note, sKOSResource, SKOSProperty.NOTE);
            }

            note = p.getEditorialnote();
            if(StringUtils.isNotEmpty(note)){
                if(filterHtmlCharacter)
                    note = Jsoup.parse(note).text();
                addDocumentation(note, sKOSResource, SKOSProperty.EDITORIAL_NOTE);
            }
            note = p.getSecopenote();
            if(StringUtils.isNotEmpty(note)){
                if(filterHtmlCharacter)
                    note = Jsoup.parse(note).text();
                addDocumentation(note, sKOSResource, SKOSProperty.SCOPE_NOTE);
            }
            note = p.getHistorynote();
            if(StringUtils.isNotEmpty(note)){
                if(filterHtmlCharacter)
                    note = Jsoup.parse(note).text();
                addDocumentation(note, sKOSResource, SKOSProperty.HISTORY_NOTE);
            }
            note = p.getExample();
            if(StringUtils.isNotEmpty(note)){
                if(filterHtmlCharacter)
                    note = Jsoup.parse(note).text();
                addDocumentation(note, sKOSResource, SKOSProperty.EXAMPLE);
            }
            note = p.getChangenote();
            if(StringUtils.isNotEmpty(note)){
                if(filterHtmlCharacter)
                    note = Jsoup.parse(note).text();
                addDocumentation(note, sKOSResource, SKOSProperty.CHANGE_NOTE);
            }

            addAlignementGiven(p.getBroadMatch(), sKOSResource, SKOSProperty.BROAD_MATCH);
            addAlignementGiven(p.getCloseMatch(), sKOSResource, SKOSProperty.CLOSE_MATCH);
            addAlignementGiven(p.getExactMatch(), sKOSResource, SKOSProperty.EXACT_MATCH);
            addAlignementGiven(p.getNarrowMatch(), sKOSResource, SKOSProperty.NARROWER_MATCH);
            addAlignementGiven(p.getRelatedMatch(), sKOSResource, SKOSProperty.RELATED_MATCH);

            addRelationsGiven(p.getNarrower(), sKOSResource);

            if (p.getBroader() != null) {
                addRelationsGiven(p.getBroader(), sKOSResource);
            }

            sKOSResource.addRelation(idThesaurus, getUriThesoFromId(idThesaurus, originalUri, nodePreference), SKOSProperty.INSCHEME);

            addReplaced(p.getReplaces(), sKOSResource, SKOSProperty.REPLACES);

            addReplaced(p.getReplaced_by(), sKOSResource, SKOSProperty.IS_REPLACED_BY);

            if (!StringUtils.isEmpty(p.getNotation())) {
                sKOSResource.addNotation(p.getNotation());
            }

            addImages(sKOSResource, p.getImg());

            addMembres(sKOSResource, p.getMembre(), p.getIdentifier());

            addFacets(sKOSResource, p.getFacets(), idThesaurus, originalUri);

            addExternalResources(sKOSResource, p.getExternalResources());

            addGps(sKOSResource, p.getGpsData());

            if (p.getCreator() != null) {
                sKOSResource.addAgent(p.getCreator(), SKOSProperty.CREATOR);
            }

            if(!StringUtils.isEmpty(p.getContributor())){
                var contributors = p.getContributor().split(SEPARATOR);
                for (String contributor : contributors) {
                    sKOSResource.addAgent(contributor, SKOSProperty.CONTRIBUTOR);
                }
            }

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            if (ObjectUtils.isNotEmpty(p.getCreated())) {
                sKOSResource.addDate(dateFormat.format(p.getCreated()), SKOSProperty.CREATED);
            }

            if (ObjectUtils.isNotEmpty(p.getModified())) {
                sKOSResource.addDate(dateFormat.format(p.getModified()), SKOSProperty.MODIFIED);
            }

            result.add(sKOSResource);
        }
        return result;
    }

    private String getUriForFacette(String idFacet, String idTheso, String originalUri) {
        return originalUri + "/?idf=" + idFacet + "&idt=" + idTheso;
    }

    private void addDoc(String content, SKOSResource resource, int type) {
        if (StringUtils.isNotEmpty(content)) {
            String[] tabs = content.split(SEPARATOR);
            try {
                for (String tab : tabs) {
                    String[] parts = tab.split(SUB_SEPARATOR);
                    if (parts.length == 2) {
                        String cleanText = Jsoup.parse(parts[0]).text();
                        resource.addDocumentation(cleanText, parts[1], type);
                    }
                }
            } catch (Exception e) {
                log.error("Erreur export Concept _ doc = " + resource.getIdentifier() + "  " + content );
            }
        }
    }

    private void addGps(SKOSResource sKOSResource, String str) {
        if (StringUtils.isNotEmpty(str)) {
            String[] tabs = str.split(SEPARATOR);
            try {
                List<SKOSGPSCoordinates> tmp = new ArrayList<>();
                for (String tab : tabs) {
                    String[] element = tab.split(SUB_SEPARATOR);
                    tmp.add(new SKOSGPSCoordinates(Double.parseDouble(element[0]), Double.parseDouble(element[1])));
                }
                sKOSResource.setGpsCoordinates(tmp);
            } catch (Exception e) {
                log.error("Erreur export Concept _ gps = " + sKOSResource.getIdentifier() + "  " + str );
            }
        }
    }

    private String getPath(String originalUri) {
        if (FacesContext.getCurrentInstance() == null) {
            return originalUri;
        }

        return FacesContext.getCurrentInstance().getExternalContext().getRequestHeaderMap().get("origin")
                + FacesContext.getCurrentInstance().getExternalContext().getRequestContextPath();
    }

    private void addImages(SKOSResource resource, String textBrut) {
        if (StringUtils.isNotEmpty(textBrut)) {
            String[] images = textBrut.split(SEPARATOR);
            try {
                ArrayList<NodeImage> nodeImages = new ArrayList<>();
                for (String image : images) {
                    String[] imageDetail = image.split(SUB_SEPARATOR);
                    // if(imageDetail.length != 4) return;

                    NodeImage nodeImage = new NodeImage();
                    nodeImage.setImageName(imageDetail[0]);
                    nodeImage.setCopyRight(imageDetail[1]);
                    nodeImage.setUri(imageDetail[2]);
                    if (imageDetail.length >= 4)
                        nodeImage.setCreator(imageDetail[3]);
                    nodeImages.add(nodeImage);
                }
                resource.setNodeImages(nodeImages);
            } catch (Exception e) {
                log.error("Erreur export Concept _ images = " + resource.getIdentifier() + "  " + textBrut );
            }
        }
    }

    private void addFacets(SKOSResource resource, String textBrut, String idTheso, String originalUri) {
        if (StringUtils.isNotEmpty(textBrut)) {
            String[] idFacettes = textBrut.split(SEPARATOR);
            try {
                for (String idFacette : idFacettes) {
                    String url = getPath(originalUri) + "/?idf=" + idFacette + "&idt=" + idTheso;
                    resource.addRelation(idFacette, url, SKOSProperty.SUB_ORDINATE_ARRAY);
                }
            } catch (Exception e) {
                log.error("Erreur export Concept _ facets = " + resource.getIdentifier() + "  " + textBrut );
            }
        }
    }
    private void addExternalResources(SKOSResource resource, String textBrut) {
        if (StringUtils.isNotEmpty(textBrut)) {
            String[] externalResources = textBrut.split(SEPARATOR);
            try {
                for (String externalResource : externalResources) {
                    resource.addExternalResource(externalResource);
                }
            } catch (Exception e) {
                log.error("Erreur export Concept _ externalResources = " + resource.getIdentifier() + "  " + textBrut );
            }
        }
    }

    private void setStatusOfConcept(String status, SKOSResource sKOSResource) {
        switch (status.toLowerCase()) {
            case "ca":
                sKOSResource.setStatus(SKOSProperty.CANDIDATE);
                break;
            case "dep":
                sKOSResource.setStatus(SKOSProperty.DEPRECATED);
                break;
            default:
                sKOSResource.setStatus(SKOSProperty.CONCEPT);
                break;
        }

    }

    private void addRelationsGiven(String textBrut, SKOSResource sKOSResource) {

        if (StringUtils.isNotEmpty(textBrut)) {
            String[] tabs = textBrut.split(SEPARATOR);
            try {
                for (String tab : tabs) {
                    String[] element = tab.split(SUB_SEPARATOR);
                    sKOSResource.addRelation(element[2], element[0], getType(element[1]));
                }
            } catch (Exception e) {
                log.error("Erreur export Concept _ relations = " + sKOSResource.getIdentifier() + "  " + textBrut );
            }
        }
    }

    private int getType(String role) {
        switch (role) {
            case "RHP":
                return SKOSProperty.RELATED_HAS_PART;
            case "RPO":
                return SKOSProperty.RELATED_PART_OF;
            case "RT":
                return SKOSProperty.RELATED;
            case "NTG":
                return SKOSProperty.NARROWER_GENERIC;
            case "NTP":
                return SKOSProperty.NARROWER_PARTITIVE;
            case "NTI":
                return SKOSProperty.NARROWER_INSTANTIAL;
            case "NT":
                return SKOSProperty.NARROWER;
            case "BTG":
                return SKOSProperty.BROADER_GENERIC;
            case "BTP":
                return SKOSProperty.BROADER_PARTITIVE;
            case "BTI":
                return SKOSProperty.BROADER_INSTANTIAL;
            default:
                return SKOSProperty.BROADER;
        }
    }

    private void addReplaced(String textBrut, SKOSResource sKOSResource, int type) {

        if (StringUtils.isNotEmpty(textBrut)) {
            String[] tabs = textBrut.split(SEPARATOR);
            try {
                for (String tab : tabs) {
                    sKOSResource.addReplaces(tab, type);
                }
            } catch (Exception e) {
                log.error("Erreur export Concept _ replaced = " + sKOSResource.getIdentifier() + "  " + textBrut );
            }
        }
    }

    private void addAlignementGiven(String textBrut, SKOSResource sKOSResource, int type) {

        if (StringUtils.isNotEmpty(textBrut)) {
            String[] tabs = textBrut.split(SEPARATOR);
            try {
                for (String tab : tabs) {
                    sKOSResource.addMatch(tab.trim(), type);
                    //    log.info(textBrut);
                }
            } catch (Exception e) {
                log.error("Erreur export Concept _ alignement = " + sKOSResource.getIdentifier() + "  " + textBrut );
            }
        }
    }

    private void addDocumentation(String textBrut, SKOSResource sKOSResource, int type) throws SQLException {

        if (StringUtils.isNotEmpty(textBrut)) {
            String[] tabs = textBrut.split(SEPARATOR);
            for (String tab : tabs) {
                try {
                    String[] element = tab.split(SUB_SEPARATOR);
                    String str = fr.cnrs.opentheso.utils.StringUtils.normalizeStringForXml(element[0]);
                    sKOSResource.addDocumentation(str, element[1], type);
                } catch (Exception e) {
                    System.out.println("Erreur export Concept _ note = " + sKOSResource.getIdentifier() + " _____ " + textBrut );
                }

            }
        }
    }

    private void getLabels(String labelBrut, SKOSResource sKOSResource, int type) {
        if (StringUtils.isNotEmpty(labelBrut)) {
            String[] tabs = labelBrut.split(SEPARATOR);

            for (String tab : tabs) {
                String[] element = tab.split(SUB_SEPARATOR);
                try {
                    sKOSResource.addLabel(element[0], element[1], type);
                } catch (Exception e) {
                    System.out.println("Erreur export Concept = " + sKOSResource.getIdentifier() + "  " + labelBrut );
                }

            }
        }
    }

    private void addMembres(SKOSResource sKOSResource, String textBrut, String idConcept) {
        if (StringUtils.isNotEmpty(textBrut)) {
            String[] tabs = textBrut.split(SEPARATOR);
            try {
                for (String tab : tabs) {
                    sKOSResource.addRelation(idConcept, tab, SKOSProperty.MEMBER_OF);
                }
            } catch (Exception e) {
                log.error("Erreur export Concept _ membres = " + sKOSResource.getIdentifier() + "  " + textBrut );
            }
        }
    }

    private String getUriFromNodeUri(String idTheso, String originalUri, String idConcept, Preferences nodePreference,
                                     NodeUri nodeUri) {

        if(nodePreference.isOriginalUriIsArk() && nodeUri.getIdArk()!= null && !StringUtils.isEmpty(nodeUri.getIdArk())) {
            return originalUri + '/' + nodeUri.getIdArk();
        }
        else if(nodePreference.isOriginalUriIsArk() && (nodeUri.getIdArk() == null || StringUtils.isEmpty(nodeUri.getIdArk())) ) {
            return getPath(originalUri) + "/?idc=" + idConcept + "&idt=" + idTheso;
        } else if(nodePreference.isOriginalUriIsHandle() && !StringUtils.isEmpty(nodeUri.getIdHandle())) {
            return "https://hdl.handle.net/" + nodeUri.getIdHandle();
        } else if (nodePreference.isOriginalUriIsDoi() && !StringUtils.isEmpty(nodeUri.getIdDoi())) {
            return "https://doi.org/" + nodeUri.getIdDoi();
        } else if (!StringUtils.isEmpty(originalUri)) {
            return originalUri + "/?idc=" + idConcept + "&idt=" + idTheso;
        } else {
            return getPath(originalUri) + "/?idc=" + idConcept + "&idt=" + idTheso;
        }
    }

    private String buildUri(NodeUri nodeUri, String idTheso, String idConcept, String originalUri, Preferences pref) {
        if (pref.isOriginalUriIsArk() && StringUtils.isNotEmpty(nodeUri.getIdArk())) {
            return originalUri + '/' + nodeUri.getIdArk();
        } else if (pref.isOriginalUriIsHandle() && StringUtils.isNotEmpty(nodeUri.getIdHandle())) {
            return "https://hdl.handle.net/" + nodeUri.getIdHandle();
        } else if (pref.isOriginalUriIsDoi() && StringUtils.isNotEmpty(nodeUri.getIdDoi())) {
            return "https://doi.org/" + nodeUri.getIdDoi();
        } else {
            return originalUri + "/?idc=" + idConcept + "&idt=" + idTheso;
        }
    }

    private String getUriThesoFromId(String id, String originalUri, Preferences pref) {
        if (pref.isOriginalUriIsArk()) return pref.getOriginalUri() + "/" + pref.getIdNaan() + "/" + id;
        if (pref.isOriginalUriIsHandle()) return "https://hdl.handle.net/" + id;
        if (StringUtils.isNotEmpty(originalUri)) return originalUri + "/?idt=" + id;
        return originalUri + "/?idt=" + id;
    }

    public StreamedContent exportSkosFormat(String format, NodeIdValue nodeIdValueOfThesaurus, boolean isToogleFilterByGroup,
                                            boolean isToogleClearHtmlCharacter, List<String> selectedIdGroups) throws Exception {

        var skosxd = getConcepts(nodeIdValueOfThesaurus.getId(), isToogleFilterByGroup, isToogleClearHtmlCharacter, selectedIdGroups);
        if (skosxd == null) {
            return null;
        }

        return switch (format.toLowerCase()) {
            case "rdf" -> generateRdfResources(skosxd, RDFFormat.RDFXML, ".rdf", nodeIdValueOfThesaurus);
            case "jsonld" -> generateRdfResources(skosxd, RDFFormat.JSONLD, ".json", nodeIdValueOfThesaurus);
            case "turtle" -> generateRdfResources(skosxd, RDFFormat.TURTLE, ".ttl", nodeIdValueOfThesaurus);
            case "json" -> generateRdfResources(skosxd, RDFFormat.RDFJSON, ".json", nodeIdValueOfThesaurus);
            default -> throw new Exception("Le format d'export n'est pas valide !");
        };
    }

    public DefaultStreamedContent exportPdfFormat(NodeIdValue nodeIdValueOfThesaurus, List<String> types, String typeSelected,
                                                  boolean isToogleExportImage, String selectedLang1Pdf, String selectedLang2Pdf,
                                                  boolean isToogleFilterByGroup, boolean isToogleClearHtmlCharacter,
                                                  List<String> selectedIdGroups) throws Exception {

        SKOSXmlDocument skosxd = getConcepts(nodeIdValueOfThesaurus.getId(), isToogleFilterByGroup, isToogleClearHtmlCharacter, selectedIdGroups);
        if (skosxd == null) {
            return null;
        }

        PdfExportType pdfExportType = PdfExportType.ALPHABETIQUE;
        if (types.indexOf(typeSelected) == 0) {
            pdfExportType = PdfExportType.HIERARCHIQUE;
        }
        try ( ByteArrayInputStream flux = new ByteArrayInputStream(writePdfNewGen.createPdfFile(skosxd, selectedLang1Pdf,
                selectedLang2Pdf, pdfExportType, isToogleExportImage))) {

            PrimeFaces.current().executeScript("PF('waitDialog').hide();");
            if (flux.available() > 0) {
                return DefaultStreamedContent
                        .builder()
                        .contentType("application/pdf")
                        .name(nodeIdValueOfThesaurus.getValue() + "_" + nodeIdValueOfThesaurus.getId() + ".pdf")
                        .stream(() -> flux)
                        .build();
            } else {
                return new DefaultStreamedContent();  // Flux vide ou invalide
            }

        } catch (Exception ex) {
            PrimeFaces.current().executeScript("PF('waitDialog').hide();");
            return new DefaultStreamedContent();
        }
    }

    public DefaultStreamedContent exportCsvFormat(NodeIdValue nodeIdValueOfThesaurus, List<NodeLangTheso> selectedLanguages,
                                                  char csvDelimiterChar, boolean isToogleFilterByGroup, boolean isToogleClearHtmlCharacter,
                                                  List<String> selectedIdGroups) throws Exception {

        SKOSXmlDocument skosxd = getConcepts(nodeIdValueOfThesaurus.getId(), isToogleFilterByGroup, isToogleClearHtmlCharacter, selectedIdGroups);
        if (skosxd == null) {
            return null;
        }

        byte[] str = csvWriteHelper.writeCsv(skosxd, selectedLanguages, csvDelimiterChar);
        try ( ByteArrayInputStream flux = new ByteArrayInputStream(str)) {
            return DefaultStreamedContent.builder().contentType("text/csv")
                    .name(nodeIdValueOfThesaurus.getValue() + "_" + nodeIdValueOfThesaurus.getId() + ".csv")
                    .stream(() -> flux)
                    .build();
        } catch (Exception ex) {
            return new DefaultStreamedContent();
        }
    }

    public DefaultStreamedContent exportCsvIdFormat(NodeIdValue nodeIdValueOfThesaurus, boolean isToogleFilterByGroup,
                                                    char csvDelimiterChar, String selectedIdLangTheso, List<String> selectedIdGroups) {
        byte[] datas;
        if (isToogleFilterByGroup) {
            datas = csvWriteHelper.writeCsvById(nodeIdValueOfThesaurus.getId(), selectedIdLangTheso, selectedIdGroups, csvDelimiterChar);
        } else {
            datas = csvWriteHelper.writeCsvById(nodeIdValueOfThesaurus.getId(), selectedIdLangTheso, null, csvDelimiterChar);
        }
        if (datas == null) {
            return null;
        }

        PrimeFaces.current().executeScript("PF('waitDialog').hide();");
        try ( ByteArrayInputStream input = new ByteArrayInputStream(datas)) {
            return DefaultStreamedContent.builder()
                    .contentType("text/csv")
                    .name(nodeIdValueOfThesaurus.getValue() + "_" + nodeIdValueOfThesaurus.getId() + ".csv")
                    .stream(() -> input)
                    .build();
        } catch (IOException ex) {
            return new DefaultStreamedContent();
        }
    }

    public DefaultStreamedContent exportCsvStrucFormat(NodeIdValue nodeIdValueOfThesaurus, String selectedIdLangTheso) {
        sizeOfTheso = 0;
        var topConcepts = conceptService.getTopConceptsWithTermByTheso(nodeIdValueOfThesaurus.getId(), selectedIdLangTheso);

        for (NodeTree topConcept : topConcepts) {
            sizeOfTheso++;
            topConcept.setPreferredTerm(StringUtils.isEmpty(topConcept.getPreferredTerm())
                    ? "(" + topConcept.getIdConcept() + ")" : topConcept.getPreferredTerm());
            topConcept.setChildrens(parcourirArbre(nodeIdValueOfThesaurus.getId(), selectedIdLangTheso, topConcept.getIdConcept()));
        }

        String[][] tab = new String[sizeOfTheso][20];
        posX = 0;
        for (NodeTree topConcept : topConcepts) {
            posJ = 0;
            createMatrice(tab, topConcept);
        }

        byte[] str = csvWriteHelper.importTreeCsv(tab, ';');
        PrimeFaces.current().executeScript("PF('waitDialog').hide();");
        try ( ByteArrayInputStream flux = new ByteArrayInputStream(str)) {
            return DefaultStreamedContent.builder().contentType("text/csv")
                    .name(nodeIdValueOfThesaurus.getValue() + "_" + nodeIdValueOfThesaurus.getId() + ".csv")
                    .stream(() -> flux)
                    .build();
        } catch (Exception ex) {
            return new DefaultStreamedContent();
        }
    }

    private void createMatrice(String[][] tab, NodeTree concept) {
        tab[posX][posJ] = concept.getPreferredTerm();
        if (CollectionUtils.isNotEmpty(concept.getChildrens())) {
            posJ++;
            if (posX < tab.length-1) posX++;
            for (NodeTree nodeTree : concept.getChildrens()) {
                if (CollectionUtils.isNotEmpty(nodeTree.getChildrens())) {
                    createMatrice(tab, nodeTree);
                } else {
                    tab[posX][posJ] = nodeTree.getPreferredTerm();
                    if (posX < tab.length-1) posX++;
                    if (posJ > tab.length - 1) posJ--;
                }
            }
            posJ--;
        }
    }

    private List<NodeTree> parcourirArbre(String thesoId, String langId, String parentId) {

        List<NodeTree> concepts = conceptService.getListChildrenOfConceptWithTerm(parentId, langId, thesoId);
        for (NodeTree concept : concepts) {
            sizeOfTheso++;
            concept.setIdParent(parentId);
            concept.setPreferredTerm(StringUtils.isEmpty(concept.getPreferredTerm()) ? "(" + concept.getIdConcept() + ")" : concept.getPreferredTerm());
            concept.setChildrens(parcourirArbre(thesoId, langId, concept.getIdConcept()));
            // Les facettes sont ignorées : l'export CSV structuré ne contient que la hiérarchie de concepts.
        }

        return concepts;
    }

    public DefaultStreamedContent exportDeprecatedFormat(NodeIdValue nodeIdValueOfThesaurus, boolean isToogleFilterByGroup,
                                                         String selectedIdLangTheso, char csvDelimiterChar) {
        byte[] datas;
        if (isToogleFilterByGroup) {
            datas = csvWriteHelper.writeCsvByDeprecated(nodeIdValueOfThesaurus.getId(), selectedIdLangTheso, csvDelimiterChar);
        } else {
            datas = csvWriteHelper.writeCsvByDeprecated(nodeIdValueOfThesaurus.getId(), selectedIdLangTheso, csvDelimiterChar);
        }
        if (datas == null) {
            return null;
        }
        PrimeFaces.current().executeScript("PF('waitDialog').hide();");
        try ( ByteArrayInputStream input = new ByteArrayInputStream(datas)) {
            return DefaultStreamedContent.builder()
                    .contentType("text/csv")
                    .name(nodeIdValueOfThesaurus.getValue() + "_" + nodeIdValueOfThesaurus.getId() + ".csv")
                    .stream(() -> input)
                    .build();
        } catch (IOException ex) {
            return new DefaultStreamedContent();
        }
    }

    /**
     * Export de chaque collection en thésaurus à part, le résultat en renvoyé en Zip
     */
    public StreamedContent exportEachGroupAsThesaurusCSV(NodeIdValue nodeIdValueOfThesaurus, boolean isToogleClearHtmlCharacter,
                                                         List<NodeGroup> groupList, List<NodeLangTheso> selectedLanguages,
                                                         char csvDelimiterChar) throws IOException {

        if(CollectionUtils.isEmpty(groupList)) return null;

        List<ByteArrayInputStream> byteArrayInputStreams = new ArrayList<>();
        List<String> fileNames = new ArrayList<>();
        String extension = ".csv";
        String name;

        /// export des thésaurus au format byteArrayInputStream, un thésaurus par collection
        for (NodeGroup nodeGroup : groupList) {
            // récupérer le SKOSXmlDocument
            SKOSXmlDocument skosxd;
            try {
                skosxd = getThesoByGroup(nodeIdValueOfThesaurus.getId(), nodeGroup.getConceptGroup().getIdGroup(), isToogleClearHtmlCharacter);
            } catch (Exception ex) {
                Logger.getLogger(ExportFileBean.class.getName()).log(Level.SEVERE, null, ex);
                return null;
            }
            if(skosxd == null) continue;


            // exporter la collection en thésaurus
            byte[] str = csvWriteHelper.writeCsv(skosxd, selectedLanguages, csvDelimiterChar);
            try ( ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(str)) {
                name = nodeIdValueOfThesaurus.getValue() + "_" + nodeGroup.getLexicalValue() + extension;
                int i = 1;
                while (fileNames.contains(name)) {
                    name = nodeIdValueOfThesaurus.getValue() + "_" + nodeGroup.getLexicalValue() + "_" + i + extension;
                    i++;
                }
                fileNames.add(name);
                byteArrayInputStreams.add(byteArrayInputStream);
            } catch (Exception ex) {
                return new DefaultStreamedContent();
            }
        }
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (ZipOutputStream zipOut = new ZipOutputStream(byteArrayOutputStream)) {
            for (int i = 0; i < byteArrayInputStreams.size(); i++) {
                // Ajoutez chaque ByteArrayInputStream au zip avec un nom de fichier unique
                addToZip(zipOut, fileNames.get(i), byteArrayInputStreams.get(i));
            }
        }
        return DefaultStreamedContent.builder()
                .contentType("application/zip")
                .name(nodeIdValueOfThesaurus.getValue() + "_" + nodeIdValueOfThesaurus.getId() + ".zip")
                .stream(() -> new ByteArrayInputStream(byteArrayOutputStream.toByteArray()))
                .build();
    }


    /**
     * Export de chaque collection en thésaurus à part, le résultat en renvoyé en Zip
     */
    public StreamedContent exportEachGroupAsThesaurusSKOS(String idTheso, String format, List<NodeGroup> groupList,
                                                          boolean isToogleClearHtmlCharacter, NodeIdValue nodeIdValueOfTheso) throws IOException {

        if(CollectionUtils.isEmpty(groupList)) return null;

        List<ByteArrayInputStream> byteArrayInputStreams = new ArrayList<>();
        List<String> fileNames = new ArrayList<>();
        String extension = ".rdf";
        String name;

        /// export des thésaurus au format byteArrayInputStream, un thésaurus par collection
        for (NodeGroup nodeGroup : groupList) {
            // récupérer le SKOSXmlDocument
            SKOSXmlDocument skosxd;
            try {
                skosxd = getThesoByGroup(idTheso, nodeGroup.getConceptGroup().getIdGroup(), isToogleClearHtmlCharacter);
            } catch (Exception ex) {
                Logger.getLogger(ExportFileBean.class.getName()).log(Level.SEVERE, null, ex);
                return null;
            }
            if(skosxd == null) continue;

            ByteArrayInputStream byteArrayInputStream = null;
            // exporter la collection en thésaurus
            switch (format.toLowerCase()) {
                case "rdf":
                    byteArrayInputStream = generateRdfResourcesByteArray(skosxd, RDFFormat.RDFXML);
                    extension = ".rdf";
                    break;
                case "jsonld":
                    byteArrayInputStream = generateRdfResourcesByteArray(skosxd, RDFFormat.JSONLD);
                    extension = ".json";
                    break;
                case "turtle":
                    byteArrayInputStream = generateRdfResourcesByteArray(skosxd, RDFFormat.TURTLE);
                    extension = ".ttl";
                    break;
                case "json":
                    byteArrayInputStream = generateRdfResourcesByteArray(skosxd, RDFFormat.RDFJSON);
                    extension = ".json";
                    break;
                default:
                    break;
            }
            name = nodeIdValueOfTheso.getValue() + "_" + nodeGroup.getLexicalValue() + extension;
            int i = 1;
            while (fileNames.contains(name)) {
                name = nodeIdValueOfTheso.getValue() + "_" + nodeGroup.getLexicalValue() + "_" + i + extension;
                i++;
            }
            fileNames.add(name);
            byteArrayInputStreams.add(byteArrayInputStream);
        }
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (ZipOutputStream zipOut = new ZipOutputStream(byteArrayOutputStream)) {
            for (int i = 0; i < byteArrayInputStreams.size(); i++) {
                // Ajoutez chaque ByteArrayInputStream au zip avec un nom de fichier unique
                addToZip(zipOut, fileNames.get(i), byteArrayInputStreams.get(i));
            }
        }
        return DefaultStreamedContent.builder()
                .contentType("application/zip")
                .name(nodeIdValueOfTheso.getValue() + "_" + nodeIdValueOfTheso.getId() + ".zip")
                .stream(() -> new ByteArrayInputStream(byteArrayOutputStream.toByteArray()))
                .build();
    }

    private void addToZip(ZipOutputStream zipOut, String fileName, ByteArrayInputStream inputStream) throws IOException {
        ZipEntry entry = new ZipEntry(fileName);
        zipOut.putNextEntry(entry);

        byte[] buffer = new byte[1024];
        int len;
        while ((len = inputStream.read(buffer)) > 0) {
            zipOut.write(buffer, 0, len);
        }
        zipOut.closeEntry();
    }

    private ByteArrayInputStream generateRdfResourcesByteArray(SKOSXmlDocument xmlDocument, RDFFormat format) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            WriteRdf4j writeRdf4j = new WriteRdf4j(xmlDocument);
            Rio.write(writeRdf4j.getModel(), out, format);
            writeRdf4j.closeCache();
            return new ByteArrayInputStream(out.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private SKOSXmlDocument getConcepts(String idTheso, boolean isToogleFilterByGroup, boolean isToogleClearHtmlCharacter,
                                        List<String> selectedIdGroups) throws Exception {

        var nodePreference = preferenceService.getThesaurusPreferences(idTheso);

        if (nodePreference == null) {
            return null;
        }
        //controle si l'URL d'origine est vide
        if(StringUtils.isEmpty(nodePreference.getOriginalUri())) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "",
                    "Veuillez ajouter une URI valide dans les préférences du thésaurus !"));
            return null;
        }

        var skosXmlDocument = new SKOSXmlDocument();
        skosXmlDocument.setConceptScheme(exportRdf4jHelperNew.exportThesoV2(idTheso, nodePreference));
        String baseUrl;
        if(StringUtils.isEmpty(nodePreference.getCheminSite())) {
            String contextPath = FacesContext.getCurrentInstance().getExternalContext().getApplicationContextPath();
            String serverAdress = FacesContext.getCurrentInstance().getExternalContext().getRequestServerName();
            String protocole = FacesContext.getCurrentInstance().getExternalContext().getRequestScheme();
            HttpServletRequest request = ((HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest());
            baseUrl = protocole + "://" + serverAdress + ":" + request.getLocalPort() + contextPath;
        } else {
            baseUrl = nodePreference.getCheminSite();
        }
        List<SKOSResource> concepts = new ArrayList<>();

        // cas d'export pour tout le thésaurus avec les collections et facettes
        if (!isToogleFilterByGroup) {
            var collectionsList = exportRdf4jHelperNew.exportCollectionsV2(idTheso);
            for (SKOSResource group : collectionsList) {
                skosXmlDocument.addGroup(group);
            }

            concepts = getAllConcepts(idTheso, baseUrl, null, nodePreference.getOriginalUri(), nodePreference, isToogleClearHtmlCharacter);

            // export des facettes
            List<SKOSResource> facettes = getAllFacettes(idTheso, baseUrl, nodePreference.getOriginalUri(), nodePreference);
            for (SKOSResource facette : facettes) {
                skosXmlDocument.addFacet(facette);
            }

        } else {
            /// Export filtré par collection, on filtre également les Facettes qui sont dans les collections sélectionnées
            for (String idGroup : selectedIdGroups) {
                NodeGroupLabel nodeGroupLabel = groupService.getNodeGroupLabel(idGroup, idTheso);
                SKOSResource sKOSResource = new SKOSResource(exportRdf4jHelperNew.getUriFromGroup(nodeGroupLabel), SKOSProperty.CONCEPT_GROUP);
                sKOSResource.addRelation(nodeGroupLabel.getIdGroup(), exportRdf4jHelperNew.getUriFromGroup(nodeGroupLabel), SKOSProperty.MICROTHESAURUS_OF);
                skosXmlDocument.addGroup(exportRdf4jHelperNew.exportThisCollectionV2(idTheso, idGroup));

                concepts.addAll(getAllConcepts(idTheso, baseUrl, idGroup, nodePreference.getOriginalUri(), nodePreference, isToogleClearHtmlCharacter));
            }

            // export des facettes filtrées
            List<SKOSResource> facettes = getAllFacettes(idTheso, baseUrl, nodePreference.getOriginalUri(), nodePreference);
            for (SKOSResource facette : facettes) {
                if(facetService.isFacetInGroups(idTheso, facette.getIdentifier(),  selectedIdGroups)){
                    skosXmlDocument.addFacet(facette);
                }
            }

        }

        for (SKOSResource concept : concepts) {
            skosXmlDocument.addconcept(concept);
        }

        return skosXmlDocument;
    }

    private SKOSXmlDocument getThesoByGroup(String idTheso, String idGroup, boolean isToogleClearHtmlCharacter) throws Exception {

        var nodePreference = preferenceService.getThesaurusPreferences(idTheso);

        if (nodePreference == null) {
            return null;
        }
        //controle si l'URL d'origine est vide
        if(StringUtils.isEmpty(nodePreference.getOriginalUri())) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "",
                    "Veuillez ajouter une URI valide dans les préférences du thésaurus !"));
            return null;
        }

        var skosXmlDocument = new SKOSXmlDocument();
        skosXmlDocument.setConceptScheme(exportRdf4jHelperNew.exportThesoV2(idTheso, nodePreference));

        String contextPath = FacesContext.getCurrentInstance().getExternalContext().getApplicationContextPath();
        String serverAdress = FacesContext.getCurrentInstance().getExternalContext().getRequestServerName();
        String protocole = FacesContext.getCurrentInstance().getExternalContext().getRequestScheme();
        HttpServletRequest request = ((HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest());
        String baseUrl = protocole + "://" + serverAdress + ":" + request.getLocalPort() + contextPath;

        List<SKOSResource> concepts = new ArrayList<>();

        NodeGroupLabel nodeGroupLabel = groupService.getNodeGroupLabel(idGroup, idTheso);
        SKOSResource sKOSResource = new SKOSResource(exportRdf4jHelperNew.getUriFromGroup(nodeGroupLabel), SKOSProperty.CONCEPT_GROUP);
        sKOSResource.addRelation(nodeGroupLabel.getIdGroup(), exportRdf4jHelperNew.getUriFromGroup(nodeGroupLabel), SKOSProperty.MICROTHESAURUS_OF);
        skosXmlDocument.addGroup(exportRdf4jHelperNew.exportThisCollectionV2(idTheso, idGroup));

        concepts.addAll(getAllConcepts(idTheso, baseUrl, idGroup, nodePreference.getOriginalUri(), nodePreference, isToogleClearHtmlCharacter));

        // export des facettes filtrées
        List<SKOSResource> facettes = getAllFacettes(idTheso, baseUrl,
                nodePreference.getOriginalUri(), nodePreference);
        List<String> groups = new ArrayList<>();
        groups.add(idGroup);

        for (SKOSResource facette : facettes) {
            if(facetService.isFacetInGroups(idTheso, facette.getIdentifier(),  groups)){
                skosXmlDocument.addFacet(facette);
            }
        }

        for (SKOSResource concept : concepts) {
            skosXmlDocument.addconcept(concept);
        }

        return skosXmlDocument;
    }

    private StreamedContent generateRdfResources(SKOSXmlDocument xmlDocument, RDFFormat format, String extension,
                                                 NodeIdValue nodeIdValueOfThesaurus) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            WriteRdf4j writeRdf4j = new WriteRdf4j(xmlDocument);
            Rio.write(writeRdf4j.getModel(), out, format);
            writeRdf4j.closeCache();

            ByteArrayInputStream input = new ByteArrayInputStream(out.toByteArray());

            return DefaultStreamedContent.builder()
                    .contentType("application/xml")
                    .name(nodeIdValueOfThesaurus.getValue() + "_" + nodeIdValueOfThesaurus.getId() + extension)
                    .stream(() -> input)
                    .build();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}