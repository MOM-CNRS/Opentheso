package fr.cnrs.opentheso.ws.dto;

import lombok.Data;

@Data
public class ArkRequest {

    private String ark;
    private Integer naan;
    private String type;
    private String urlTarget;
    private String title;
    private String creator;
}
