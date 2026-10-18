package fr.cnrs.opentheso.services.mappers;

import fr.cnrs.opentheso.models.skos.SkosConceptImageDto;
import fr.cnrs.opentheso.entites.Gps;
import fr.cnrs.opentheso.models.skos.SkosConceptDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class SkosConceptMapper {

    /**
     * Normalise le DTO : relations, labels, notes, images.
     * Doit être appelé une seule fois avant la persistance.
     */
    public SkosConceptDto finalizeMapping(SkosConceptDto dto) {
        populateRelations(dto);
        populateNotes(dto);
        populateLabels(dto);
        populateImages(dto);
        populateGpsPointsFromGeo(dto);
        return dto;
    }

    private void populateRelations(SkosConceptDto dto) {
        String[] relationTypes = {
                "skos:broader", "skos:narrower", "skos:related",
                "broaderId", "narrowerId", "relatedId",
                "iso-thes:superOrdinate", "superOrdinateId",
                "skos:member", "memberId",
                "dcterms:isReplacedBy", "dcterms:replaces",
                "skos:exactMatch", "skos:closeMatch", "skos:relatedMatch",
                "skos:broadMatch", "skos:narrowMatch"
        };
        for (String type : relationTypes) {
            for (String uri : dto.getRawColumn(type)) {
                dto.addRelation(type.replace("skos:", ""), uri);
            }
        }
    }

    private void populateNotes(SkosConceptDto dto) {
        String[] noteTypes = {"definition", "note", "scopeNote", "historyNote", "changeNote", "editorialNote", "example"};
        for (String type : noteTypes) {
            Map<String, String> map = dto.getTranslations().get(type);
            if (map != null) {
                map.forEach((lang, value) -> dto.setNote(type, lang, value)); // unique par type/langue
            }
        }
    }

    private void populateLabels(SkosConceptDto dto) {
        // Labels optimisés : prefLabel unique, altLabel et hiddenLabel multiples
        Map<String, Map<String, String>> translations = dto.getTranslations();

        // prefLabel
        Map<String, String> prefMap = translations.get("prefLabel");
        if (prefMap != null) {
            prefMap.forEach(dto::setPrefLabel);
        }

        // altLabel
        Map<String, String> altMap = translations.get("altLabel");
        if (altMap != null) {
            altMap.forEach(dto::addAltLabel);
        }

        // hiddenLabel
        Map<String, String> hiddenMap = translations.get("hiddenLabel");
        if (hiddenMap != null) {
            hiddenMap.forEach(dto::addHiddenLabel);
        }
    }

    private void populateImages(SkosConceptDto dto) {
        for (String raw : dto.getRawColumn("foaf:Image")) {
            String[] imageEntries = raw.split("##");
            for (String entry : imageEntries) {
                if (entry == null || entry.isBlank()) continue;

                SkosConceptImageDto imageDto = new SkosConceptImageDto();
                String[] parts = entry.split("@@");
                for (String part : parts) {
                    if (part.startsWith("rdf:about=")) {
                        imageDto.setUri(part.substring("rdf:about=".length()));
                    } else if (part.contains("=")) {
                        String[] kv = part.split("=", 2);
                        imageDto.addMeta(kv[0], kv[1]);
                    }
                }
                dto.getImages().add(imageDto);
            }
        }
    }

    /**
     * Parse le champ geoGps du DTO et construit la liste de points GPS
     */
    public void populateGpsPointsFromGeo(SkosConceptDto dto) {
        dto.getGpsPoints().clear();
        if (dto.getGeoGps() == null || dto.getGeoGps().isBlank()) return;

        String[] points = dto.getGeoGps().split(",");
        for (String point : points) {
            point = point.trim();
            if (point.isEmpty()) continue;

            String[] parts = point.split("\\s+");
            if (parts.length < 2) continue;

            try {
                double lat = Double.parseDouble(parts[0]);
                double lon = Double.parseDouble(parts[1]);
                Gps gps = new Gps();
                gps.setLatitude(lat);
                gps.setLongitude(lon);
                dto.getGpsPoints().add(gps);
            } catch (NumberFormatException e) {
                log.error("Le format GPS n'est pas conforme");// on peut logger ici si besoin
            }
        }
/*
        // Pour compatibilité : premier point dans latitude/longitude
        if (!dto.getGpsPoints().isEmpty()) {
            SkosConceptDto.Gps first = dto.getGpsPoints().get(0);
            dto.setLatitude(first.getLatitude());
            dto.setLongitude(first.getLongitude());
        }
    }

        // Pour compatibilité, on peut remplir latitude/longitude avec le premier point
        if (!gpsPoints.isEmpty()) {
            this.latitude = gpsPoints.get(0).getLatitude();
            this.longitude = gpsPoints.get(0).getLongitude();
        }*/
    }
}