package com.example.dominoassistant;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;

public class CalculateTrainsActivity extends AppCompatActivity {
    private ArrayList<Domino> dominoes;
    private DominoTrainSolver trainSolver;
    ArrayList<DominoTrain> solvedTrains;
    ArrayList<DominoTrain> uniqueTrains;
    private String dominoesString;
    private String startingPipsString;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calculate_trains);
        setTitle("Potential Trains");

        dominoesString = getIntent().getStringExtra("dominoesString");
        startingPipsString = getIntent().getStringExtra("startingPips");
        dominoes = Domino.decodeDominoes(dominoesString);
        trainSolver = new DominoTrainSolver(dominoes);
        solvedTrains = trainSolver.solveForTrains(Integer.parseInt(startingPipsString));
        // sort trains, with priority in order of length, number of doubles, then number of points
        Collections.sort(solvedTrains, (train, t1) -> 100000 * (t1.length - train.length) + 1000 * (t1.numDoubles - train.numDoubles) + (t1.numPoints - train.numPoints));
        uniqueTrains = new ArrayList<>();
        uniqueTrains.add(solvedTrains.get(0));
        for (int i = 1; i < solvedTrains.size(); ++i){
            if (!solvedTrains.get(i).equals(solvedTrains.get(i-1))){
                uniqueTrains.add(solvedTrains.get(i));
            }
        }

    }

    public void selectDominoes(View view) {
        Intent intent = new Intent(getBaseContext(), SelectDominoesActivity.class);
        intent.putExtra("dominoesString", Domino.getDominoesString(dominoes));
        startActivity(intent);
    }
}