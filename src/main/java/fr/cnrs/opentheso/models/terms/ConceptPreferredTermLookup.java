package fr.cnrs.opentheso.models.terms;

@FunctionalInterface
public interface ConceptPreferredTermLookup {

    Term getThisTerm(String idConcept, String idThesaurus, String idLang);
}
