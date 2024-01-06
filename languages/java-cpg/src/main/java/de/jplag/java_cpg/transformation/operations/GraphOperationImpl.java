package de.jplag.java_cpg.transformation.operations;

import de.fraunhofer.aisec.cpg.graph.Node;
import de.jplag.java_cpg.transformation.matching.edges.CpgEdge;
import de.jplag.java_cpg.transformation.matching.edges.CpgMultiEdge;
import de.jplag.java_cpg.transformation.matching.pattern.NodePattern;
import de.jplag.java_cpg.transformation.matching.pattern.WildcardGraphPattern;

public abstract class GraphOperationImpl<S extends Node,T extends Node> implements GraphOperation {

    protected final NodePattern<? extends S> parentPattern;
    protected final CpgEdge<S, T> edge;

    protected GraphOperationImpl(NodePattern<? extends S> parentPattern, CpgEdge<S, T> edge) {
        this.parentPattern = parentPattern;
        this.edge = edge;
    }

    public boolean isWildcarded() {
        return this.parentPattern instanceof WildcardGraphPattern<?>.ParentNodePattern
            && this.edge instanceof WildcardGraphPattern<?>.Edge;
    }

    public boolean isMultiEdged() {
        return this.edge instanceof CpgMultiEdge<S,T>.Any1ofNEdge;
    }
}