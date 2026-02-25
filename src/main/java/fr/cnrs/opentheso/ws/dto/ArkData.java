package fr.cnrs.opentheso.ws.dto;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class ArkData {

    private String arkId;
    private int naan;
    private String url;
    private boolean linkup;
    private boolean redirect;
    private int userArkId;
    private OffsetDateTime modificationDate;
    private String title;
    private String creator;
    private boolean deprecated;
    private String replacedBy;
}
