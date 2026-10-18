package fr.cnrs.opentheso.services;

import fr.cnrs.opentheso.models.skos.SkosConceptImageDto;
import fr.cnrs.opentheso.entites.Gps;
import fr.cnrs.opentheso.entites.PreferredTerm;
import fr.cnrs.opentheso.models.skos.SkosConceptDto;
import fr.cnrs.opentheso.models.terms.Term;
import fr.cnrs.opentheso.repositories.*;
import fr.cnrs.opentheso.services.mappers.SkosAlignmentMapper;
import fr.cnrs.opentheso.services.mappers.SkosConceptMapper;
import fr.cnrs.opentheso.services.mappers.SkosRelationMapper;
import fr.cnrs.opentheso.services.security.CryptoService;
import fr.cnrs.opentheso.services.utils.IdentifierService;
import fr.cnrs.opentheso.ws.openapi.exception.ConceptAlreadyExistsException;
import fr.cnrs.opentheso.ws.openapi.exception.LabelAlreadyExistsException;
import fr.cnrs.opentheso.ws.openapi.exception.NotationAlreadyExistsException;
import fr.cnrs.opentheso.ws.openapi.exception.ThesaurusNotFoundException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.IntStream;


@Service
@RequiredArgsConstructor
public class ConceptAddServiceWS {

    private final ThesaurusRepository thesaurusRepository;
    private final ConceptRepository conceptRepository;
    private final PreferenceService preferenceService;
    private final IdentifierService identifierService;
    private final SkosConceptMapper skosConceptMapper;
    private final ArkService arkService;
    private final CryptoService cryptoService;
    private final TermRepository termRepository;
    private final PreferredTermRepository preferredTermRepository;
    private final NonPreferredTermService nonPreferredTermService;
    private final NonPreferredTermRepository nonPreferredTermRepository;
    private final NoteService noteService;
    private final NoteRepository noteRepository;
    private final RelationService relationService;
    private final AlignmentService alignmentService;
    private final ImageService imageService;
    private final GpsService gpsService;

    @Transactional
    public SkosConceptDto createConcept(SkosConceptDto dto, String idThesaurus, Integer creator) {
        skosConceptMapper.finalizeMapping(dto);

        // Control de cohérence avant insertion, en cas d'erreur, l'exception est capturée et l'exécution est arrêtée
        validateConcept(idThesaurus, dto);

        // sauvegarde du concept
        String identifier = saveConcept(idThesaurus, dto, creator);

        // Sauvegarde multi-langues labels
        String idTerm = savePrefLabels(identifier, idThesaurus, dto.getPrefLabels(), creator);
        if (idTerm == null) {
            throw new RuntimeException("Un prefLabel est obligatoire !");
        }
        boolean isAltLabel = true;
        saveMultiLabels(idTerm, identifier, idThesaurus, dto.getAltLabels(), isAltLabel, creator);
        saveMultiLabels(idTerm, identifier, idThesaurus, dto.getHiddenLabels(), !isAltLabel, creator);

        // Sauvegarde multi-langues notes
        saveNotes(identifier, idThesaurus, dto.getNotes(), creator);

        // Relations (Alignements + relations hiérarchiques)
        saveRelations(identifier, idThesaurus, dto.getRelations(), creator);

        // Images
        saveImages(identifier, idThesaurus, dto.getImages(), creator);
        // GPS
        if(StringUtils.isBlank(dto.getGeoGps())) {
            saveGps(identifier, idThesaurus, dto.getLatitude(), dto.getLongitude());
        } else{
            saveListGps(identifier, idThesaurus, dto.getGpsPoints());
        }
//        dto.setIdentifier(identifier);
        return dto;
    }

    // ----------------- Helpers -----------------
    private String saveConcept(String idThesaurus, SkosConceptDto dto, Integer creator) {
        // Sauvegarde du concept principal
        boolean topConcept = dto.getRelations("broader").isEmpty(); // true si aucune broader

        // Déterminer status en fonction de dto.getResourceType() ou dto.getDeprecated()
        String status;
        if (dto.isDeprecated()) {
            status = "DEP";
        } else if ("candidate".equalsIgnoreCase(dto.getConceptType()) || "CA".equalsIgnoreCase(dto.getConceptType())) {
            status = "CA";
        } else {
            status = "D"; // par défaut descripteur
        }

        // récupération des préférences.
        var preferences = preferenceService.getThesaurusPreferences(idThesaurus);
        if (preferences == null) {
            throw new RuntimeException("Pas de préférences valides pour ce thésaurus !");
        }

        // génération de l'identifiant
        String identifier = dto.getIdentifier();
        if (StringUtils.isEmpty(identifier)) {
            String generatedId;
            if (preferences.getIdentifierType() == 1) {
                generatedId = identifierService.getAlphaNumericId();
            } else {
                generatedId = identifierService.getNumericConceptId();
            }
            identifier = generatedId; // affectation finale, mais IntelliJ est content
        }

        // génération de l'idArk
        String permanentId = dto.getPermanentId();

        conceptRepository.saveConcept(
                identifier,                 // idConcept
                idThesaurus,                // idThesaurus
                permanentId,                // idArk
                dto.getNotation(),          // notation
                topConcept,
                creator,                    // creator
                dto.getConceptType(),       // type (concept, place...)
                status                      // status D, CA, dep
        );
        if (StringUtils.isBlank(permanentId)) {
            if (preferences.isUseArkLocal()) {
                ArrayList<String> idConcepts = new ArrayList<>();
                idConcepts.add(identifier);
                if (!arkService.generateArkIdLocal(idThesaurus, idConcepts)) {
                    throw new RuntimeException("La création du Ark a échoué");
                }
            }
            if (preferences.isUseOpenArk()) {
                // générer ark sur le serveur
                ArrayList<String> idConcepts = new ArrayList<>();
                idConcepts.add(identifier);
                String apiKeyOpenArk = cryptoService.decrypt(preferences.getApiKeyOpenArk());
                if (!arkService.generateArkWithOpenArk(idThesaurus, idConcepts,
                        preferences.getSourceLang(), "" + creator, apiKeyOpenArk, preferences)) {
                    throw new RuntimeException("La création du Ark a échoué");
                }
            }
        }
        return identifier;
    }

    private String savePrefLabels(String idConcept,
                                  String idThesaurus,
                                  Map<String, String> labels,
                                  int idUser) {

        if (labels == null) return null;
        String idTerm = generateNextIdTerm(idThesaurus);
        for (var entry : labels.entrySet()) {

            String lang = entry.getKey();
            String value = entry.getValue();
            if (value == null || (value = value.trim()).isEmpty()) continue;

            if (termRepository.existsPrefLabel(value, lang, idThesaurus)) {
                throw new LabelAlreadyExistsException(value);
            }
            termRepository.save(
                    fr.cnrs.opentheso.entites.Term.builder()
                            .idTerm(idTerm)
                            .lexicalValue(value)
                            .lang(lang)
                            .idThesaurus(idThesaurus)
                            .source("API")
                            .status("D")
                            .contributor(idUser)
                            .creator(idUser)
                            .created(new Date())
                            .modified(new Date())
                            .build()
            );
            preferredTermRepository.save(PreferredTerm.builder()
                    .idConcept(idConcept)
                    .idThesaurus(idThesaurus)
                    .idTerm(idTerm)
                    .build());
        }
        return idTerm;
    }

    private void saveMultiLabels(String idTerrm, String idConcept,
                                 String idThesaurus, Map<String,
                    List<String>> labels, boolean isAltLabel, Integer idUser) {
        if (labels == null) return;

        for (var entry : labels.entrySet()) {
            String lang = entry.getKey();
            List<String> values = entry.getValue();
            if (values == null) continue;

            for (String value : values) {
                if (value == null || (value = value.trim()).isEmpty()) continue;
                if (!nonPreferredTermService.addNonPreferredTerm(Term.builder()
                        .idTerm(idTerrm)
                        .lexicalValue(value.trim())
                        .lang(lang)
                        .idThesaurus(idThesaurus)
                        .hidden(!isAltLabel)
                        .status(isAltLabel ? "Hidden" : "USE")
                        .build(), idUser)) {

                    throw new LabelAlreadyExistsException(value);
                }
            }
        }
    }

    private void saveNotes(
            String idConcept,
            String idThesaurus,
            Map<String, Map<String, String>> notes,
            Integer idUser
    ) {

        if (notes == null || notes.isEmpty()) return;

        for (var typeEntry : notes.entrySet()) {

            String noteType = typeEntry.getKey();
            Map<String, String> langMap = typeEntry.getValue();

            if (langMap == null || langMap.isEmpty()) continue;

            for (var langEntry : langMap.entrySet()) {

                String lang = langEntry.getKey();
                String value = langEntry.getValue();

                if (value == null) continue;

                value = value.trim();
                if (value.isEmpty()) continue;

                noteService.addNote(
                        idConcept,
                        lang,
                        idThesaurus,
                        value,
                        noteType,
                        "",
                        idUser
                );
            }
        }
    }

    /**
     * Mappe le type de relation SKOS à un int interne pour l’alignement
     */
    private void saveRelations(
            String idConcept,
            String idThesaurus,
            Map<String, List<String>> relations,
            Integer idUser
    ) {
        if (relations == null || relations.isEmpty()) return;

        for (var entry : relations.entrySet()) {
            String relationType = entry.getKey();
            List<String> targets = entry.getValue();

            if (targets == null || targets.isEmpty()) continue;

            for (String targetUri : targets) {
                if (targetUri == null || targetUri.isBlank()) continue;

                // Cas des alignements SKOS

                Integer alignmentProp = SkosAlignmentMapper.toAlignmentProperty(relationType);
                if (alignmentProp != null) {
                    alignmentService.addNewAlignment(
                            idUser,
                            "",
                            "",
                            targetUri,
                            alignmentProp,
                            idConcept,
                            idThesaurus,
                            0
                    );
                    continue;
                }

                // Cas des relations hiérarchiques / associatives : utilisation de la nouvelle méthode
                String relationCode = SkosRelationMapper.toDbRelation(relationType);
                if (relationCode != null) {
                    relationService.addRelation(idConcept, idThesaurus, targetUri, relationCode, idUser);
                }
            }
        }
    }

    private void validateConcept(String idThesaurus, SkosConceptDto dto) {
        // Control de cohérence avant insertion
        if (!thesaurusRepository.existsById(idThesaurus)) {
            throw new ThesaurusNotFoundException(idThesaurus);
        }
        if (StringUtils.isNotBlank(dto.getIdentifier())) {
            if (conceptRepository.existsByIdConceptAndIdThesaurus(dto.getIdentifier(), idThesaurus)) {
                throw new ConceptAlreadyExistsException(dto.getIdentifier());
            }
        }
        if (StringUtils.isNotBlank(dto.getNotation())) {
            if (conceptRepository.existsByIdThesaurusAndNotation(idThesaurus, dto.getNotation())) {
                throw new NotationAlreadyExistsException(dto.getNotation(), idThesaurus);
            }
        }
        if (dto.getPrefLabels() == null || dto.getPrefLabels().isEmpty()) {
            throw new RuntimeException("Un prefLabel est obligatoire");
        }
    }

    private void saveImages(String idConcept, String idThesaurus, List<SkosConceptImageDto> images, Integer idUser) {
        if (images == null) return;
        for (SkosConceptImageDto img : images) {
            imageService.addExternalImage(idConcept, idThesaurus,
                    img.getMetadata().getOrDefault("name", ""),
                    img.getMetadata().getOrDefault("copyright", ""),
                    img.getUri(),
                    img.getMetadata().getOrDefault("creator", ""),
                    idUser);
        }
    }

    private void saveGps(String idConcept, String idThesaurus, Double latitude, Double longitude){
        if(latitude == null || longitude == null) return;
        Gps gps = new Gps();
        gps.setLatitude(latitude);
        gps.setLongitude(longitude);
        gps.setIdConcept(idConcept);
        gps.setIdTheso(idThesaurus);
        gps.setPosition(0);

        gpsService.saveNewGps(gps);
    }

    private void saveListGps(String idConcept, String idThesaurus, List<Gps> geoGps){
        if(geoGps == null || geoGps.isEmpty()) return;
        IntStream.range(0, geoGps.size())
                .forEach(i -> {
                    Gps gps = geoGps.get(i);
                    gps.setIdConcept(idConcept);
                    gps.setIdTheso(idThesaurus);
                    gps.setPosition(i + 1);
                    gpsService.saveNewGps(gps);
                });
    }


    private String generateNextIdTerm(String idThesaurus) {
        int idTermNum = termRepository.getMaxInternalId();
        String idTerm;

        do {
            idTerm = String.valueOf(++idTermNum);
        } while (termRepository.findByIdTermAndIdThesaurus(idTerm, idThesaurus).isPresent());
        return idTerm;
    }
}