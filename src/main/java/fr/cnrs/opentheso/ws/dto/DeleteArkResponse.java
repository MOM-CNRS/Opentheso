package fr.cnrs.opentheso.ws.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeleteArkResponse {
    private String status;
    private String message;
}
