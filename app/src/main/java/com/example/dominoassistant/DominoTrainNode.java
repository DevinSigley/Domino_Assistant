package com.example.dominoassistant;

import java.util.ArrayList;

public class DominoTrainNode {
    public Domino domino;
    public int parentNodeIndex;
    public int nextNodeAIndex;
    public int nextNodeBIndex;
    public int nextNodeCIndex;
    public boolean doubleTile;
    public boolean numberARootward; // used to determine if Domino.numberA or B are connecting to parent

    public DominoTrainNode(Domino domino) {
        this.domino = domino;
        parentNodeIndex = -1;
        nextNodeAIndex = -1;
        nextNodeBIndex = -1;
        nextNodeCIndex = -1;
        doubleTile = domino.numberA == domino.numberB;
    }

    public static DominoTrainNode clone(DominoTrainNode inputNode){
        DominoTrainNode outputNode = new DominoTrainNode(inputNode.domino);
        outputNode.parentNodeIndex = inputNode.parentNodeIndex;
        outputNode.nextNodeAIndex = inputNode.nextNodeAIndex;
        outputNode.nextNodeBIndex = inputNode.nextNodeBIndex;
        outputNode.nextNodeCIndex = inputNode.nextNodeCIndex;
        outputNode.numberARootward = inputNode.numberARootward;
        return outputNode;
    }

    public static ArrayList<DominoTrainNode> clone(ArrayList<DominoTrainNode> inputNodes){
        ArrayList<DominoTrainNode> outputNodes = new ArrayList<>();
        for (int i = 0; i < inputNodes.size(); ++i){
            outputNodes.add(DominoTrainNode.clone(inputNodes.get(i)));
        }
        return outputNodes;
    }

    public void setNextIndex(int index){
        if (nextNodeAIndex == -1){
            nextNodeAIndex = index;
        }
        else if (nextNodeBIndex == -1){
            nextNodeBIndex = index;
        }
        else if (nextNodeCIndex == -1){
            nextNodeCIndex = index;
        }
    }

}