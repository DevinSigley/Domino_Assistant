package com.example.dominoassistant;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Stack;

public class DominoTrainSolver {
    private ArrayList<Domino> dominoes;
    private ArrayList<ArrayList<DominoTrainNode>> dominoTrains;
    private boolean[] nodesAvailable;
    private ArrayList<ArrayList<Integer>> dominoAdjacencyList;
    //private ArrayList<Integer> leafIndices;
    private ArrayList<DominoTrainNode> referenceNodes;

    public DominoTrainSolver(ArrayList<Domino> dominoes) {
        this.dominoes = dominoes;
        nodesAvailable = new boolean[dominoes.size()];
        Arrays.fill(nodesAvailable, true);
        dominoTrains = new ArrayList<>();
        referenceNodes = new ArrayList<>();
        //leafIndices = new Stack<>();


        // create adjacency list for domino indices
        dominoAdjacencyList = new ArrayList<>();
        for (int i = 0; i < dominoes.size(); ++i){
            Domino currentDomino = dominoes.get(i);
            ArrayList<Integer> currentList = new ArrayList<>();
            for (int j = 0; j < dominoes.size(); ++j){
                if (i != j){
                    Domino queriedDomino = dominoes.get(j);
                    if (currentDomino.numberA == queriedDomino.numberA || currentDomino.numberA == queriedDomino.numberB || currentDomino.numberB == queriedDomino.numberA || currentDomino.numberB == queriedDomino.numberB){
                        currentList.add(j);
                    }
                }
            }
            // terminate list with -1
            currentList.add(-1);
            dominoAdjacencyList.add(currentList);

            // populate reference nodes
            DominoTrainNode newNode = new DominoTrainNode(currentDomino);
            referenceNodes.add(newNode);
        }
    }

    public ArrayList<ArrayList<DominoTrainNode>> solveForTrains() {
        // Calculate trains where each of the dominoes is the start
        for (int i = 0; i < dominoes.size(); ++i){
            solveForOneDomino(i);
        }
        return dominoTrains;
    }

    public void solveForOneDomino(int startingDominoIndex){
        ArrayList<DominoTrainNode> startingTrain = DominoTrainNode.clone(referenceNodes);
        startingTrain.get(startingDominoIndex).numberARootward = true;
        ArrayList<Integer> leafIndices = new ArrayList<>();
        leafIndices.add(startingDominoIndex);
        recursiveDFS(startingDominoIndex, startingTrain, leafIndices);

        // TODO: solve domino when .numberARootward = false

/*        leafIndices.push(startingDominoIndex);

        while (!leafIndices.isEmpty()){
            int currentIndex = leafIndices.pop();
            for (int i = 0; i < dominoAdjacencyList.get(currentIndex).size() - 1; ++i){
                int potentialIndex = dominoAdjacencyList.get(currentIndex).get(i);
                if (nodesAvailable[potentialIndex]){

                }
            }
        }
*/

        // First, search as if numberA has to be matched

    }

    private void recursiveDFS(int node, ArrayList<DominoTrainNode> currentTrain, ArrayList<Integer> leafIndices){
        nodesAvailable[node] = false;
        // no leaves have available adjacent dominoes, so record train
        if (leafIndices.isEmpty()){
            // record train
            dominoTrains.add(DominoTrainNode.clone(currentTrain));
        }
        else {
            for (int i = 0; i < leafIndices.size(); ++i){
                int leafIndex = leafIndices.get(i);
                // Check each adjacent node to see if it can be added to end of this leaf
                for (int j = 0; j < dominoAdjacencyList.get(leafIndex).size() - 1; ++j){
                    int nodeBeingVisited = dominoAdjacencyList.get(leafIndex).get(j);
                    if (nodesAvailable[nodeBeingVisited]){
                        // TODO: check if == numberA or == numberB
                        // current numberB matches new numberA
                        if (currentTrain.get(leafIndex).numberARootward && dominoes.get(leafIndex).numberB == dominoes.get(nodeBeingVisited).numberA){
                            ArrayList<DominoTrainNode> copyCurrentTrain = DominoTrainNode.clone(currentTrain);
                            copyCurrentTrain.get(leafIndex).setNextIndex(nodeBeingVisited);
                            copyCurrentTrain.get(nodeBeingVisited).numberARootward = true;
                            copyCurrentTrain.get(nodeBeingVisited).parentNodeIndex = leafIndex;
                            ArrayList<Integer> copyLeafIndices = new ArrayList<>(leafIndices);
                            copyLeafIndices.remove(i);
                            copyLeafIndices.add(nodeBeingVisited);
                            if (copyCurrentTrain.get(nodeBeingVisited).doubleTile){
                                copyLeafIndices.add(nodeBeingVisited);
                                copyLeafIndices.add(nodeBeingVisited);
                            }
                            recursiveDFS(nodeBeingVisited, copyCurrentTrain, copyLeafIndices);
                        }

                        else if (currentTrain.get(leafIndex).numberARootward && dominoes.get(leafIndex).numberB == dominoes.get(nodeBeingVisited).numberB){
                            ArrayList<DominoTrainNode> copyCurrentTrain = DominoTrainNode.clone(currentTrain);
                            copyCurrentTrain.get(leafIndex).setNextIndex(nodeBeingVisited);
                            copyCurrentTrain.get(nodeBeingVisited).numberARootward = false;
                            copyCurrentTrain.get(nodeBeingVisited).parentNodeIndex = leafIndex;
                            ArrayList<Integer> copyLeafIndices = new ArrayList<>(leafIndices);
                            copyLeafIndices.remove(i);
                            copyLeafIndices.add(nodeBeingVisited);
                            if (copyCurrentTrain.get(nodeBeingVisited).doubleTile){
                                copyLeafIndices.add(nodeBeingVisited);
                                copyLeafIndices.add(nodeBeingVisited);
                            }
                            recursiveDFS(nodeBeingVisited, copyCurrentTrain, copyLeafIndices);
                        }

                        else if (!currentTrain.get(leafIndex).numberARootward && dominoes.get(leafIndex).numberA == dominoes.get(nodeBeingVisited).numberA){
                            ArrayList<DominoTrainNode> copyCurrentTrain = DominoTrainNode.clone(currentTrain);
                            copyCurrentTrain.get(leafIndex).setNextIndex(nodeBeingVisited);
                            copyCurrentTrain.get(nodeBeingVisited).numberARootward = true;
                            copyCurrentTrain.get(nodeBeingVisited).parentNodeIndex = leafIndex;
                            ArrayList<Integer> copyLeafIndices = new ArrayList<>(leafIndices);
                            copyLeafIndices.remove(i);
                            copyLeafIndices.add(nodeBeingVisited);
                            if (copyCurrentTrain.get(nodeBeingVisited).doubleTile){
                                copyLeafIndices.add(nodeBeingVisited);
                                copyLeafIndices.add(nodeBeingVisited);
                            }
                            recursiveDFS(nodeBeingVisited, copyCurrentTrain, copyLeafIndices);
                        }

                        else if (!currentTrain.get(leafIndex).numberARootward && dominoes.get(leafIndex).numberA == dominoes.get(nodeBeingVisited).numberB){
                            ArrayList<DominoTrainNode> copyCurrentTrain = DominoTrainNode.clone(currentTrain);
                            copyCurrentTrain.get(leafIndex).setNextIndex(nodeBeingVisited);
                            copyCurrentTrain.get(nodeBeingVisited).numberARootward = false;
                            copyCurrentTrain.get(nodeBeingVisited).parentNodeIndex = leafIndex;
                            ArrayList<Integer> copyLeafIndices = new ArrayList<>(leafIndices);
                            copyLeafIndices.remove(i);
                            copyLeafIndices.add(nodeBeingVisited);
                            if (copyCurrentTrain.get(nodeBeingVisited).doubleTile){
                                copyLeafIndices.add(nodeBeingVisited);
                                copyLeafIndices.add(nodeBeingVisited);
                            }
                            recursiveDFS(nodeBeingVisited, copyCurrentTrain, copyLeafIndices);
                        }
                        // add it to the current train, add it as a leaf?
                        // TODO: pass copies of currentTrain and leafIndices?
                    }
                }
                leafIndices.remove(leafIndex);
                i--;
            }
        }
        nodesAvailable[node] = true;
        // pop its leaf?

    }
    /*
    private void recursiveDFS(int node, boolean matchNumberA, ArrayList<DominoTrainNode> currentTrain){
        nodesAvailable[node] = false;
        // no leaves have available adjacent dominoes, so record train
        if (leafIndices.isEmpty()){
            // TODO: record train
        }
        else {
            for (int i = 0; i < leafIndices.size(); ++i){
                // Check each adjacent node to see if it can be added to end of this leaf
                for (int j = 0; j < dominoAdjacencyList.get(node).size() - 1; ++j){
                    int nodeBeingVisited = dominoAdjacencyList.get(node).get(j);
                    if (nodesAvailable[nodeBeingVisited]){
                        // TODO: check if == numberA or == numberB
                        // add it to the current train, add it as a leaf?
                    }
                }
            }
        }
        nodesAvailable[node] = true;
        // pop its leaf?

    }
    */

}
