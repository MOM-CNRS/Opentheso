package fr.cnrs.opentheso.stats.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Data
@AllArgsConstructor
public class LanguageOption implements Serializable {

    private String code;
    private String label;
}