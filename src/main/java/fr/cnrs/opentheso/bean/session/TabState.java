package fr.cnrs.opentheso.bean.session;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TabState {
    private String thesoId;
    private String conceptId;
}
