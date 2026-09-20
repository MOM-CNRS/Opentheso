package fr.cnrs.opentheso.models.concept;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
/**
 *
 * @author miledrousset
 */

@Data
public class DCMIResource {
    private String title;
    private String creator;
    private String subject;
    private String description;
    private String publisher;
    private String contributor;
    private String date;
    private String type;
    private String format;
    private String identifier;
    private String source;
    private String language;
    private String relation;
    private String coverage;
    private String rights;
    private String rightsHolder;
    
    private String conformsTo;
    private String created;  
    private String modified;    
    private String isRequiredBy ;
    private String license;
    private String replaces;
    private String alternative;    

    // Constructeur par défaut
    public DCMIResource() {
    }
    
    /// quelques éléments du Dcterms  
    public static final String TITLE = "title";
    public static final String CREATOR = "creator";
    public static final String CONTRIBUTOR = "contributor";    
    public static final String SUBJECT = "subject";    

    public static final String DESCRIPTION = "description";
    public static final String PUBLISHER = "publisher";
    public static final String DATE = "date";
    public static final String TYPE = "type";
    public static final String FORMAT = "format";
    public static final String IDENTIFIER = "identifier";
    public static final String SOURCE = "source";
    public static final String LANGUAGE = "language";
    public static final String RELATION = "relation";
    public static final String COVERAGE = "coverage";
    public static final String RIGHTS = "rights";     
    public static final String RIGHTS_HOLDER = "rightsHolder";      
    
    public static final String CONFORMS_TO = "conformsTo";    
    public static final String CREATED = "created";     
    public static final String MODIFIED = "modified";      
    public static final String IS_REQUIRED_BY = "isRequiredBy";    
    public static final String LICENSE = "license";      
    public static final String REPLACES = "replaces";     
    public static final String ALTERNATIVE = "alternative";
    public static final String MAITRE = "maitre";
    
    /// Les types de données
    public static final String TYPE_DATE = "date";
    public static final String TYPE_LANGUE = "langString";
    public static final String TYPE_STRING = "string";    
    public static final String TYPE_RESOURCE = "resource";
    public static final String TYPE_BOOLEAN = "boolean";
    
    public List<String> getAllResources(){
        List allResources = new ArrayList();
        allResources.add("title");
        allResources.add("creator");
        allResources.add("subject");     
        allResources.add("description");
        allResources.add("publisher");
        allResources.add("contributor"); 
        allResources.add("date");
        allResources.add("type");
        allResources.add("format"); 
        allResources.add("identifier");
        allResources.add("language");
        allResources.add("relation");      
        allResources.add("coverage");
        allResources.add("rights");  
        allResources.add("rightsHolder");        
        
        allResources.add("conformsTo");     
        allResources.add("created");    
        allResources.add("modified");         
        allResources.add("isRequiredBy");    
        allResources.add("license"); 
        allResources.add("replaces");  
        allResources.add("alternative");
        allResources.add("maitre");
        return allResources;
    }
    
    public List<String> getAllTypes(){
        List allTypes = new ArrayList();
        allTypes.add("date");
        allTypes.add("string"); 
        allTypes.add("resource");
        allTypes.add("boolean");
        return allTypes;
    }    
    
}
