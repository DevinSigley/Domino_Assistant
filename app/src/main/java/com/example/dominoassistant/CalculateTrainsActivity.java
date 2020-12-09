package com.example.dominoassistant;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;

public class CalculateTrainsActivity extends AppCompatActivity {
    private ArrayList<Domino> dominoes;
    private String dominoesString;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calculate_trains);

        dominoesString = getIntent().getStringExtra("dominoesString");
        dominoes = Domino.decodeDominoes(dominoesString);

        ArrayList<ArrayList<DominoTrainNode>> dominoTrains = new ArrayList<>();
        ArrayList<DominoTrainNode> referenceTrain = new ArrayList<>();
        for (int i = 0; i < dominoes.size(); ++i){
            DominoTrainNode node = new DominoTrainNode(dominoes.get(i));
            referenceTrain.add(node);
        }

        // adjacency list containing domino indices
        ArrayList<ArrayList<Integer>> dominoAdjacencyList = new ArrayList<>();
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
            currentList.add(-1);
            dominoAdjacencyList.add(currentList);
        }

        // keep track of dominoes already in train
        boolean[] nodeAvailable = new boolean[dominoes.size()];

        // TODO: support double-dominoes, support specifying a starting pip number, allow backtracking to find multiple trains
        for (int i = 0; i < dominoes.size(); ++i){
            Arrays.fill(nodeAvailable, true);
            nodeAvailable[i] = false;
            ArrayList<DominoTrainNode> currentTrain = new ArrayList<>();
            DominoTrainNode parentNode = new DominoTrainNode(dominoes.get(i));
            int currentEndValue = parentNode.domino.numberA;

            int j = i;
            int k = 0;
            int currentAdjacencyValue = dominoAdjacencyList.get(j).get(k);
            while (currentAdjacencyValue != -1){
                if (nodeAvailable[currentAdjacencyValue]){
                    Domino tmpDomino = dominoes.get(currentAdjacencyValue);
                    if (currentEndValue == tmpDomino.numberA || currentEndValue == tmpDomino.numberB){
                        nodeAvailable[currentAdjacencyValue] = false;
                        DominoTrainNode newNode = new DominoTrainNode(tmpDomino);
                        if (currentEndValue == newNode.domino.numberA){
                            newNode.numberARootward = true;
                            currentEndValue = newNode.domino.numberB;
                        }
                        else {
                            newNode.numberARootward = false;
                            currentEndValue = newNode.domino.numberA;
                        }
                        newNode.parentNodeIndex = j;
                        parentNode.nextNodeAIndex = currentAdjacencyValue;
                        currentTrain.add(parentNode);
                        currentTrain.add(newNode);
                        parentNode = newNode;
                        j = currentAdjacencyValue;
                        k = 0;
                    }
                    else {
                        k++;
                    }
                }
                else {
                    k++;
                }
                currentAdjacencyValue = dominoAdjacencyList.get(j).get(k);
            }
            dominoTrains.add(currentTrain);
        }

        TextView testText = (TextView) findViewById(R.id.trainBuilderTestText);
        testText.setText(dominoTrains.toString());
    }

    private void DepthFirstSearch (ArrayList<ArrayList<DominoTrainNode>> dominoTrains){

    }

    private void dfsRecursive(){

    }
}