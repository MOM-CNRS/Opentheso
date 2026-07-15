package fr.cnrs.opentheso.stats.services;

import com.fasterxml.jackson.databind.ObjectMapper;

import javax.enterprise.context.ApplicationScoped;
import java.io.Serializable;

@ApplicationScoped
public class DashboardChartBuilder implements Serializable {

    private final ObjectMapper mapper = new ObjectMapper();

}
