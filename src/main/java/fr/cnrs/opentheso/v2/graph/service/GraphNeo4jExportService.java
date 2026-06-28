package fr.cnrs.opentheso.v2.graph.service;

import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.graph.model.GraphExportEntry;
import lombok.RequiredArgsConstructor;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.QueryConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Properties;

@Service
@RequiredArgsConstructor
public class GraphNeo4jExportService {

    @Value("${app.neo4j.enabled:false}")
    private boolean neo4jEnabled;

    @Value("${neo4j.serverName}")
    private String serverNameNeo4j;

    @Value("${neo4j.serverPort}")
    private String serverPortNeo4j;

    @Value("${neo4j.user}")
    private String userNeo4j;

    @Value("${neo4j.password}")
    private String passwordNeo4j;

    @Value("${neo4j.databaseName}")
    private String databaseNameNeo4j;

    private final GraphViewReadService graphViewReadService;

    public void exportView(String viewId, String openthesoBaseUrl) {
        if (!neo4jEnabled) {
            MessageUtils.showWarnMessage("Export Neo4J désactivé (app.neo4j.enabled=false)");
            return;
        }

        var view = graphViewReadService.loadView(viewId);
        if (view == null || !view.hasExports()) {
            return;
        }

        var properties = buildNeo4jProperties();
        var thesaurusImportUri = openthesoBaseUrl + "/openapi/v1/thesaurus/%THESO_ID%";
        var branchImportUri = openthesoBaseUrl + "/openapi/v1/concept/%THESO_ID%/%TOP_CONCEPT_ID%/expansion?way=down";
        var dbUri = "neo4j://" + properties.getProperty("neo4j.serverName") + ":"
                + properties.getProperty("neo4j.serverPort");

        try (var driver = GraphDatabase.driver(
                dbUri,
                AuthTokens.basic(properties.getProperty("neo4j.user"), properties.getProperty("neo4j.password")))) {
            driver.verifyConnectivity();

            var builder = new StringBuilder();
            for (GraphExportEntry export : view.getExports()) {
                builder.append("CALL n10s.rdf.import.fetch(\"");
                builder.append(export.conceptId() == null
                        ? thesaurusImportUri.replace("%THESO_ID%", export.thesaurusId())
                        : branchImportUri.replace("%THESO_ID%", export.thesaurusId())
                                .replace("%TOP_CONCEPT_ID%", export.conceptId()));
                builder.append("\", \"RDF/XML\", {headerParams: { Accept: \"application/rdf+xml;charset=utf-8\"}});\n");
            }

            driver.executableQuery("CALL apoc.cypher.runMany('" + builder + "', {}, {statistics:false,timeout:10})")
                    .withConfig(QueryConfig.builder()
                            .withDatabase(properties.getProperty("neo4j.databaseName"))
                            .build())
                    .execute();

            MessageUtils.showInformationMessage("Export réussi vers Neo4J !");
        } catch (Exception ex) {
            MessageUtils.showErrorMessage("Erreur de connexion à la base de données Neo4J !");
        }
    }

    private Properties buildNeo4jProperties() {
        var props = new Properties();
        props.setProperty("neo4j.serverName", serverNameNeo4j);
        props.setProperty("neo4j.serverPort", serverPortNeo4j);
        props.setProperty("neo4j.user", userNeo4j);
        props.setProperty("neo4j.password", passwordNeo4j);
        props.setProperty("neo4j.databaseName", databaseNameNeo4j);
        return props;
    }
}
