package fr.cnrs.opentheso.services.mappers;

import fr.cnrs.opentheso.entites.Gps;
import fr.cnrs.opentheso.models.skos.SkosConceptUpdateDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class SkosConceptUpdateMapper {

    /**
     * Normalise le DTO : relations, labels, notes, images.
     * Doit être appelé une seule fois avant la persistance.
     */
    public SkosConceptUpdateDto finalizeMapping(SkosConceptUpdateDto dto) {
   //     populateLabels(dto);
        return dto;
    }



}