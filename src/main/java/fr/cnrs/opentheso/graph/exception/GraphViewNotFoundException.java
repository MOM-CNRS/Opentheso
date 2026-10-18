package fr.cnrs.opentheso.graph.exception;

public class GraphViewNotFoundException extends RuntimeException {

    public GraphViewNotFoundException(int viewId) {
        super("Vue graphe introuvable : " + viewId);
    }
}
