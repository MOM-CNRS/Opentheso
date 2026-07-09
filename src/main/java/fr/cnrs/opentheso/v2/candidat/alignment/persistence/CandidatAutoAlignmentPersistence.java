package fr.cnrs.opentheso.v2.candidat.alignment.persistence;

import fr.cnrs.opentheso.entites.Alignement;
import fr.cnrs.opentheso.entites.Gps;
import fr.cnrs.opentheso.entites.ImageExterne;
import fr.cnrs.opentheso.entites.Note;
import fr.cnrs.opentheso.models.alignment.AlignementSource;
import fr.cnrs.opentheso.models.alignment.NodeAlignment;
import fr.cnrs.opentheso.models.alignment.NodeAlignmentSmall;
import fr.cnrs.opentheso.models.alignment.SelectedResource;
import fr.cnrs.opentheso.models.nodes.NodeImage;
import fr.cnrs.opentheso.models.notes.NodeNote;
import fr.cnrs.opentheso.models.terms.NodeTermTraduction;
import fr.cnrs.opentheso.repositories.AlignementRepository;
import fr.cnrs.opentheso.repositories.AlignementSourceRepository;
import fr.cnrs.opentheso.repositories.AlignementTypeRepository;
import fr.cnrs.opentheso.repositories.ConceptRepository;
import fr.cnrs.opentheso.repositories.GpsRepository;
import fr.cnrs.opentheso.repositories.ImagesRepository;
import fr.cnrs.opentheso.repositories.NoteRepository;
import fr.cnrs.opentheso.repositories.PreferredTermRepository;
import fr.cnrs.opentheso.repositories.TermRepository;
import fr.cnrs.opentheso.repositories.ThesaurusLabelRepository;
import fr.cnrs.opentheso.utils.StringUtils;
import fr.cnrs.opentheso.v2.concept.write.persistence.ConceptTranslationWriteRepository;
import fr.cnrs.opentheso.v2.concept.write.persistence.ConceptWritePostMutationRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CandidatAutoAlignmentPersistence {

    private final AlignementSourceRepository alignementSourceRepository;
    private final AlignementTypeRepository alignementTypeRepository;
    private final ThesaurusLabelRepository thesaurusLabelRepository;
    private final AlignementRepository alignementRepository;
    private final PreferredTermRepository preferredTermRepository;
    private final TermRepository termRepository;
    private final NoteRepository noteRepository;
    private final ImagesRepository imagesRepository;
    private final GpsRepository gpsRepository;
    private final ConceptRepository conceptRepository;
    private final ConceptTranslationWriteRepository conceptTranslationWriteRepository;
    private final ConceptWritePostMutationRepository conceptWritePostMutationRepository;

    public List<AlignementSource> loadAlignmentSources(String thesaurusId) {
        var projections = alignementSourceRepository.findAllByThesaurus(thesaurusId);
        if (CollectionUtils.isEmpty(projections)) {
            return List.of();
        }
        return projections.stream()
                .map(element -> AlignementSource.builder()
                        .id(element.getId())
                        .source(element.getSource())
                        .requete(element.getRequete())
                        .typeRequete(element.getTypeRequete())
                        .alignement_format(element.getAlignement_format())
                        .description(element.getDescription())
                        .source_filter(element.getSource_filter())
                        .isGps(element.getGps())
                        .build())
                .toList();
    }

    public List<Map.Entry<String, String>> loadAlignmentTypes() {
        var types = alignementTypeRepository.findAll();
        if (CollectionUtils.isEmpty(types)) {
            return List.of();
        }
        var map = new HashMap<String, String>();
        types.forEach(type -> map.put(String.valueOf(type.getId()), type.getLabelSkos()));
        return new ArrayList<>(map.entrySet());
    }

    public List<String> loadThesaurusLanguages(String thesaurusId) {
        return new ArrayList<>(thesaurusLabelRepository.findDistinctLangByIdThesaurus(thesaurusId));
    }

    public List<NodeAlignment> loadExistingAlignments(String conceptId, String thesaurusId) {
        var alignements = alignementRepository.findAllAlignmentsByConceptAndThesaurus(conceptId, thesaurusId);
        if (CollectionUtils.isEmpty(alignements)) {
            return List.of();
        }
        return alignements.stream()
                .map(element -> NodeAlignment.builder()
                        .id_alignement(element.getId())
                        .thesaurus_target(element.getThesaurus_target())
                        .concept_target(element.getConcept_target())
                        .uri_target(element.getUri_target())
                        .alignement_id_type(element.getAlignement_id_type())
                        .internal_id_concept(element.getInternal_id_concept())
                        .internal_id_thesaurus(element.getInternal_id_thesaurus())
                        .build())
                .toList();
    }

    public List<NodeTermTraduction> loadTranslations(String thesaurusId, String conceptId) {
        var traductions = termRepository.findAllTraductionsOfConcept(conceptId, thesaurusId);
        return traductions == null ? List.of() : traductions;
    }

    public List<NodeNote> loadNotes(String conceptId, String thesaurusId) {
        var notes = noteRepository.findAllByIdentifierAndIdThesaurus(conceptId, thesaurusId);
        if (notes.isEmpty()) {
            return List.of();
        }
        return notes.stream()
                .map(element -> NodeNote.builder()
                        .idTerm(conceptId)
                        .idNote(element.getId())
                        .lang(element.getLang())
                        .lexicalValue(element.getLexicalValue())
                        .modified(element.getModified())
                        .created(element.getCreated())
                        .noteTypeCode(element.getNoteTypeCode())
                        .noteSource(element.getNoteSource())
                        .identifier(element.getIdentifier())
                        .build())
                .toList();
    }

    public List<NodeImage> loadImages(String conceptId, String thesaurusId) {
        var result = imagesRepository.findAllByIdConceptAndIdThesaurus(conceptId, thesaurusId);
        return result.stream()
                .map(element -> NodeImage.builder()
                        .idConcept(element.getIdConcept())
                        .idThesaurus(element.getIdThesaurus())
                        .imageName(element.getImageName())
                        .copyRight(element.getImageCopyright())
                        .uri(element.getExternalUri())
                        .build())
                .toList();
    }

    public List<NodeAlignmentSmall> loadAlignmentSmallList(String conceptId, String thesaurusId) {
        var alignements = alignementRepository.findAllAlignmentsByConceptAndThesaurus(conceptId, thesaurusId);
        if (CollectionUtils.isEmpty(alignements)) {
            return List.of();
        }
        return alignements.stream()
                .map(element -> NodeAlignmentSmall.builder()
                        .uri_target(element.getUri_target())
                        .alignement_id_type(element.getAlignement_id_type())
                        .build())
                .toList();
    }

    @Transactional
    public boolean addAlignment(
            int userId,
            String conceptTarget,
            String thesaurusTarget,
            String uriTarget,
            int alignmentTypeId,
            String conceptId,
            String thesaurusId,
            int alignmentSourceId
    ) {
        thesaurusTarget = StringUtils.convertString(thesaurusTarget);
        uriTarget = StringUtils.convertString(uriTarget);
        conceptTarget = StringUtils.convertString(conceptTarget);

        if (alignementRepository.existsByConceptThesaurusTypeAndUri(thesaurusId, conceptId, alignmentTypeId, uriTarget)) {
            return true;
        }

        var alignementType = alignementTypeRepository.findById(alignmentTypeId);
        if (alignementType.isEmpty()) {
            return false;
        }

        Optional<fr.cnrs.opentheso.entites.AlignementSource> source = alignmentSourceId > 0
                ? alignementSourceRepository.findById(alignmentSourceId)
                : Optional.empty();

        alignementRepository.save(Alignement.builder()
                .author(userId)
                .conceptTarget(conceptTarget)
                .thesaurusTarget(thesaurusTarget)
                .uriTarget(uriTarget)
                .urlAvailable(true)
                .alignementType(alignementType.get())
                .internalIdConcept(conceptId)
                .internalIdThesaurus(thesaurusId)
                .alignementSource(source.orElse(null))
                .created(new Date())
                .modified(new Date())
                .build());
        return true;
    }

    @Transactional
    public boolean addSelectedTranslations(
            String thesaurusId,
            String conceptId,
            int userId,
            List<SelectedResource> translations
    ) {
        var preferredTerm = preferredTermRepository.findByIdThesaurusAndIdConcept(thesaurusId, conceptId);
        if (preferredTerm.isEmpty()) {
            return false;
        }
        String idTerm = preferredTerm.get().getIdTerm();

        for (SelectedResource selectedResource : translations) {
            if (!selectedResource.isSelected()) {
                continue;
            }
            String lang = selectedResource.getIdLang();
            String value = StringUtils.convertString(selectedResource.getGettedValue());
            if (termRepository.findByIdTermAndIdThesaurusAndLang(idTerm, thesaurusId, lang).isPresent()) {
                conceptTranslationWriteRepository.updateTranslation(idTerm, thesaurusId, lang, value, userId);
            } else {
                conceptTranslationWriteRepository.insertTranslation(idTerm, thesaurusId, lang, value, userId);
            }
        }
        return true;
    }

    @Transactional
    public boolean addSelectedDefinitions(
            String conceptId,
            String thesaurusId,
            int userId,
            String noteSource,
            List<SelectedResource> definitions
    ) {
        for (SelectedResource selectedResource : definitions) {
            if (!selectedResource.isSelected()) {
                continue;
            }
            String lang = normalizeLang(selectedResource.getIdLang());
            String noteValue = StringUtils.clearNoteFromP(
                    StringUtils.clearValue(StringEscapeUtils.unescapeXml(selectedResource.getGettedValue())));
            if (isNoteDuplicate(conceptId, thesaurusId, lang, noteValue, "definition")) {
                continue;
            }
            var existing = noteRepository.findAllByIdentifierAndIdThesaurusAndNoteTypeCodeAndLang(
                    conceptId, thesaurusId, "definition", lang);
            if (!existing.isEmpty()) {
                existing.get(0).setLexicalValue(noteValue);
                existing.get(0).setNoteSource(noteSource);
                noteRepository.save(existing.get(0));
            } else {
                noteRepository.save(Note.builder()
                        .noteTypeCode("definition")
                        .idThesaurus(thesaurusId)
                        .lang(lang)
                        .lexicalValue(noteValue)
                        .identifier(conceptId)
                        .noteSource(noteSource)
                        .idUser(userId)
                        .created(new Date())
                        .modified(new Date())
                        .build());
            }
        }
        return true;
    }

    @Transactional
    public boolean addSelectedImages(
            String conceptId,
            String thesaurusId,
            int userId,
            String imageName,
            String noteSource,
            List<SelectedResource> images
    ) {
        for (SelectedResource selectedResource : images) {
            if (!selectedResource.isSelected()) {
                continue;
            }
            ImageExterne saved = imagesRepository.save(ImageExterne.builder()
                    .imageCreator("")
                    .idUser(userId)
                    .idConcept(conceptId)
                    .idThesaurus(thesaurusId)
                    .imageName(org.apache.commons.lang3.StringUtils.isEmpty(imageName) ? "" : imageName)
                    .imageCopyright(noteSource)
                    .externalUri(selectedResource.getGettedValue().trim())
                    .build());
            if (ObjectUtils.isEmpty(saved)) {
                return false;
            }
        }
        return true;
    }

    @Transactional
    public boolean insertGpsCoordinates(String conceptId, String thesaurusId, double latitude, double longitude) {
        var existing = gpsRepository.findByIdConceptAndIdThesoOrderByPosition(conceptId, thesaurusId);
        if (CollectionUtils.isNotEmpty(existing)) {
            return gpsRepository.updateCoordinates(conceptId, thesaurusId, latitude, longitude) > 0;
        }
        var gpsSaved = gpsRepository.save(Gps.builder()
                .idTheso(thesaurusId)
                .idConcept(conceptId)
                .longitude(longitude)
                .latitude(latitude)
                .build());
        if (ObjectUtils.isEmpty(gpsSaved)) {
            return false;
        }
        return conceptRepository.setGpsTag(true, conceptId, thesaurusId) > 0;
    }

    @Transactional
    public void touchConcept(String thesaurusId, String conceptId, int userId) {
        conceptWritePostMutationRepository.touchConcept(thesaurusId, conceptId, userId);
    }

    private boolean isNoteDuplicate(String conceptId, String thesaurusId, String lang, String note, String typeCode) {
        var noteFound = noteRepository.findAllByIdentifierAndIdThesaurusAndNoteTypeCodeAndLangAndLexicalValue(
                conceptId, thesaurusId, typeCode, lang, StringUtils.convertString(note));
        return !noteFound.isEmpty();
    }

    private String normalizeLang(String lang) {
        return switch (lang) {
            case "en-GB", "en-US" -> "en";
            case "pt-BR", "pt-PT" -> "pt";
            default -> lang;
        };
    }
}
