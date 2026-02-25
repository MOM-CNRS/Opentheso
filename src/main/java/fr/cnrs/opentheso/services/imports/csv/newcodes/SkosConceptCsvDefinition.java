package fr.cnrs.opentheso.services.imports.csv.newcodes;

import fr.cnrs.opentheso.models.skos.ResourceType;
import fr.cnrs.opentheso.models.skos.SkosConceptDto;

import java.util.List;
import java.util.function.Function;

/**
 * Définit les colonnes CSV pour SkosConceptDto.
 * Colonnes multilingues et multiples sont automatiquement gérées via CsvMapper.
 */
public class SkosConceptCsvDefinition {

    public static List<CsvColumnDefinition<SkosConceptDto, ?>> getDefinitions() {
        return List.of(
                // Colonnes simples
                new CsvColumnDefinition<>("URI", Function.identity(), SkosConceptDto::setUri),
                new CsvColumnDefinition<>("localURI", Function.identity(), SkosConceptDto::setLocalUri),
                new CsvColumnDefinition<>("identifier", Function.identity(), SkosConceptDto::setIdentifier),
                new CsvColumnDefinition<>("arkId", Function.identity(), SkosConceptDto::setPermanentId),
                new CsvColumnDefinition<>("skos:notation", Function.identity(), SkosConceptDto::setNotation),

                // rdf:type → ResourceType (ENUM)
                new CsvColumnDefinition<>("rdf:type",
                        ResourceType::fromRdfName,
                        SkosConceptDto::setResourceType),

                // Boolean : owl:deprecated
                new CsvColumnDefinition<>("owl:deprecated",
                        value -> value.equalsIgnoreCase("true") || value.equals("1"),
                        SkosConceptDto::setDeprecated),

                // ConceptType
                new CsvColumnDefinition<>("conceptType", Function.identity(), SkosConceptDto::setConceptType),

                // Dates
                new CsvColumnDefinition<>("dcterms:created", Function.identity(), SkosConceptDto::setCreated),
                new CsvColumnDefinition<>("dcterms:modified", Function.identity(), SkosConceptDto::setModified),

                // Créateur / contributeurs
                new CsvColumnDefinition<>("dcterms:creator", Function.identity(), SkosConceptDto::setCreatorName),
                new CsvColumnDefinition<>("dcterms:contributor",
                        value -> List.of(value.split("##")),
                        SkosConceptDto::setContributorName),

                // GPS : convertir les nombres en String ou conserver le format original
                new CsvColumnDefinition<SkosConceptDto, Double>(
                        "geo:lat",
                        Double::parseDouble,
                        (dto, val) -> dto.setLatitude(val)
                ),
                new CsvColumnDefinition<SkosConceptDto, Double>(
                        "geo:long",
                        Double::parseDouble,
                        (dto, val) -> dto.setLongitude(val)
                ),

                // GPS complet : on stocke la chaîne brute, à traiter ensuite si besoin
                new CsvColumnDefinition<>("geo:gps", Function.identity(), SkosConceptDto::setGeoGps)
        );
    }
}
