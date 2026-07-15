package fr.cnrs.opentheso.stats.dto;

import lombok.Data;

@Data
/**
 * Nombre d'appels à un endpoint API donné, sur une période.
 */
public class ApiUsageStat {

    private final String url;
    private final String httpMethod;
    private final long nbAppels;

    public ApiUsageStat(String url, String httpMethod, long nbAppels) {
        this.url = url;
        this.httpMethod = httpMethod;
        this.nbAppels = nbAppels;
    }
}
