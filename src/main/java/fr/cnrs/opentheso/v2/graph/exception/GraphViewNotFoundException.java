package fr.cnrs.opentheso.v2.graph.exception;

public class GraphViewNotFoundException extends RuntimeException {

    public GraphViewNotFoundException(int viewId) {
        super("Vue graphe introuvable : " + viewId);
    }
}
