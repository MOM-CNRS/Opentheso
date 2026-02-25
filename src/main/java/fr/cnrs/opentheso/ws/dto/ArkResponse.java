package fr.cnrs.opentheso.ws.dto;

import lombok.Data;

@Data
public class ArkResponse {

    private ArkData ark;
    private String message;
    private String status;
}
