package com.example.dominoassistant;

import java.util.ArrayList;

public class DominoTrain {
    public ArrayList<DominoTrainNode> train;
    public int numDoubles;
    public int length;
    public int numPoints;
    public int rootIndex;

    public DominoTrain() {
    }

    public DominoTrain(int rootIndex) {
        this.rootIndex = rootIndex;
        length = 0;
        numDoubles = 0;
        numPoints = 0;
    }

    public static DominoTrain clone(DominoTrain input){
        DominoTrain result = new DominoTrain(input.rootIndex);
        result.length = input.length;
        result.numDoubles = input.numDoubles;
        result.numPoints = input.numPoints;
        result.train = DominoTrainNode.clone(input.train);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof  DominoTrain){
            return (this.length == ((DominoTrain) obj).length && this.numDoubles == ((DominoTrain) obj).numDoubles && this.numPoints == ((DominoTrain) obj).numPoints);
        }
        return false;
        //return super.equals(obj);
    }
}