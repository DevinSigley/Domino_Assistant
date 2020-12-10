package com.example.dominoassistant;

import java.util.ArrayList;
import java.util.Arrays;

public class DominoTrainSolver {
    private ArrayList<Domino> dominoes;
    private ArrayList<DominoTrain> dominoTrains;
    private boolean[] nodesAvailable;
    private ArrayList<ArrayList<Integer>> dominoAdjacencyList;
    private ArrayList<DominoTrainNode> referenceNodes;

    public DominoTrainSolver(ArrayList<Domino> dominoes) {
        this.dominoes = dominoes;
        nodesAvailable = new boolean[dominoes.size()];
        Arrays.fill(nodesAvailable, true);
        dominoTrains = new ArrayList<>();
        referenceNodes = new ArrayList<>();
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

    public ArrayList<DominoTrain> solveForTrains(int startingPips) {
        // Calculate trains where each of the dominoes is the start
        for (int i = 0; i < dominoes.size(); ++i){
            if (dominoes.get(i).numberA == startingPips){
                solveForOneDomino(i, true);
            }
            else if (dominoes.get(i).numberB == startingPips){
                solveForOneDomino(i, false);
            }
        }
        return dominoTrains;
    }

    public void solveForOneDomino(int startingDominoIndex, boolean numberARootward){
        DominoTrain startingTrain = new DominoTrain(startingDominoIndex);
        startingTrain.train = DominoTrainNode.clone(referenceNodes);
        startingTrain.train.get(startingDominoIndex).numberARootward = numberARootward;
        startingTrain.length = 1;
        startingTrain.numPoints += dominoes.get(startingDominoIndex).numberA + dominoes.get(startingDominoIndex).numberB;
        if (startingTrain.train.get(startingDominoIndex).doubleTile){
            startingTrain.numDoubles++;
        }
        ArrayList<Integer> leafIndices = new ArrayList<>();
        leafIndices.add(startingDominoIndex);
        recursiveDFS(startingDominoIndex, startingTrain, leafIndices);

    }

    private void recursiveDFS(int node, DominoTrain currentTrain, ArrayList<Integer> leafIndices){
        boolean anyNodeAddedToTree = false;
        nodesAvailable[node] = false;
        // no leaves have available adjacent dominoes, so record train
        if (leafIndices.size() == 0){
            // record train
            dominoTrains.add(DominoTrain.clone(currentTrain));
        }
        else {
            for (int i = 0; i < leafIndices.size(); ++i){
                int leafIndex = leafIndices.get(i);
                // Check each adjacent node to see if it can be added to end of this leaf
                for (int j = 0; j < dominoAdjacencyList.get(leafIndex).size() - 1; ++j){
                    int nodeBeingVisited = dominoAdjacencyList.get(leafIndex).get(j);
                    if (nodesAvailable[nodeBeingVisited]){
                        DominoTrain copyCurrentTrain = new DominoTrain();
                        boolean thisNodeAddedToTree = false;
                        // current numberB matches new numberA
                        if (currentTrain.train.get(leafIndex).numberARootward && dominoes.get(leafIndex).numberB == dominoes.get(nodeBeingVisited).numberA){
                            copyCurrentTrain = DominoTrain.clone(currentTrain);
                            copyCurrentTrain.train.get(nodeBeingVisited).numberARootward = true;
                            thisNodeAddedToTree = true;
                        }

                        else if (currentTrain.train.get(leafIndex).numberARootward && dominoes.get(leafIndex).numberB == dominoes.get(nodeBeingVisited).numberB){
                            copyCurrentTrain = DominoTrain.clone(currentTrain);
                            copyCurrentTrain.train.get(nodeBeingVisited).numberARootward = false;
                            thisNodeAddedToTree = true;
                        }

                        else if (!currentTrain.train.get(leafIndex).numberARootward && dominoes.get(leafIndex).numberA == dominoes.get(nodeBeingVisited).numberA){
                            copyCurrentTrain = DominoTrain.clone(currentTrain);
                            copyCurrentTrain.train.get(nodeBeingVisited).numberARootward = true;
                            thisNodeAddedToTree = true;
                        }

                        else if (!currentTrain.train.get(leafIndex).numberARootward && dominoes.get(leafIndex).numberA == dominoes.get(nodeBeingVisited).numberB){
                            copyCurrentTrain = DominoTrain.clone(currentTrain);
                            copyCurrentTrain.train.get(nodeBeingVisited).numberARootward = false;
                            thisNodeAddedToTree = true;
                        }

                        if (thisNodeAddedToTree){
                            anyNodeAddedToTree = true;
                            copyCurrentTrain.train.get(leafIndex).setNextIndex(nodeBeingVisited);
                            copyCurrentTrain.train.get(nodeBeingVisited).parentNodeIndex = leafIndex;
                            ArrayList<Integer> copyLeafIndices = new ArrayList<>(leafIndices);
                            copyLeafIndices.remove(i);
                            copyLeafIndices.add(nodeBeingVisited);
                            if (copyCurrentTrain.train.get(nodeBeingVisited).doubleTile){
                                copyCurrentTrain.numDoubles++;
                                copyLeafIndices.add(nodeBeingVisited);
                                copyLeafIndices.add(nodeBeingVisited);
                            }
                            copyCurrentTrain.length++;
                            copyCurrentTrain.numPoints += dominoes.get(nodeBeingVisited).numberA + dominoes.get(nodeBeingVisited).numberB;
                            recursiveDFS(nodeBeingVisited, copyCurrentTrain, copyLeafIndices);
                        }
                    }
                }

                leafIndices.remove(i);
                i--;
            }
            if (leafIndices.size() == 0 && !anyNodeAddedToTree){
                // record train
                dominoTrains.add(DominoTrain.clone(currentTrain));
            }
        }
        nodesAvailable[node] = true;
        // pop its leaf?

    }
}
