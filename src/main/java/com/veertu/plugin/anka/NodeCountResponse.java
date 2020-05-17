package com.veertu.plugin.anka;

public class NodeCountResponse {

    public final int numNodes;
    public final int numNodesPerLabel;

    public NodeCountResponse(int numNodes, int numNodesPerLabel) {
        this.numNodes = numNodes;
        this.numNodesPerLabel = numNodesPerLabel;
    }


}
