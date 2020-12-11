package com.example.dominoassistant;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.media.Image;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Stack;

public class CalculateTrainsActivity extends AppCompatActivity {
    private ArrayList<Domino> dominoes;
    private DominoTrainSolver trainSolver;
    private ArrayList<DominoTrain> solvedTrains;
    private ArrayList<DominoTrain> uniqueTrains;
    private String dominoesString;
    private String startingPipsString;
    private int dominoHeight;
    int[] dominoImageIds;
    int[] doublesDepth;

    enum Direction{
        CENTER,
        BELOW,
        ABOVE
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calculate_trains);
        setTitle("Best Potential Trains");
        SelectDominoesActivity.thisActivity.finish();

        dominoesString = getIntent().getStringExtra("dominoesString");
        //dominoesString = "2,1;1,3;0,3;3,5;4,5;3,3;";
        //dominoesString = "5,4;4,10;10,10;10,12;12,1;10,3;3,7;7,7;7,8;7,12;7,1;1,3;1,9;10,6;6,8;7,4;";
        startingPipsString = getIntent().getStringExtra("startingPips");
        //startingPipsString = "2";
        //startingPipsString = "5";
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
        solvedTrains = null; // set to null to indicate that solved trains may be garbage collected


        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                drawTrain(0, 1);
                if (uniqueTrains.size() > 1){
                    drawTrain(1, 2);
                }
                if (uniqueTrains.size() > 2){
                    drawTrain(2, 3);
                }
            }
        });
        //drawTrain(0, 2);
    }

    public void selectDominoes(View view) {
        Intent intent = new Intent(getBaseContext(), SelectDominoesActivity.class);
        //intent.putExtra("dominoesString", Domino.getDominoesString(dominoes));
        intent.putExtra("dominoesString", dominoesString);
        startActivity(intent);
        finish();
    }

    // trainIndex is the index within the solved unique dominoes ArrayList
    // trainPosition is position 1, 2, or 3, which are top, middle, and bottom areas, respectively
    private void drawTrain(int trainIndex, int trainPosition){
        DominoTrain train = uniqueTrains.get(trainIndex);
        // Set the text values for the train
        TextView trainDesc;
        ConstraintLayout parentConstraintLayout;
        switch (trainPosition){
            case 1:
                trainDesc = findViewById(R.id.trainDescText1);
                parentConstraintLayout = (ConstraintLayout)findViewById(R.id.trainConstraint1); break;
            case 2:
                trainDesc = findViewById(R.id.trainDescText2);
                parentConstraintLayout = (ConstraintLayout)findViewById(R.id.trainConstraint2); break;
            case 3:
                trainDesc = findViewById(R.id.trainDescText3);
                parentConstraintLayout = (ConstraintLayout)findViewById(R.id.trainConstraint3); break;
            default:
                trainDesc = new TextView(this); // should never happen
                parentConstraintLayout = new ConstraintLayout(this); // should never happen
        }
        trainDesc.setText("Length: " + Integer.toString(train.length) + "\nDoubles: " + Integer.toString(train.numDoubles) + "\nPoints: " + Integer.toString(train.numPoints));

        // Now, to draw the dominoes
        // associate each domino with their ImageView ID
        dominoImageIds = new int[train.train.size()];
        doublesDepth = new int[train.train.size()];
        //String dominoHeight = Integer.toString(48 / train.numDoubles) + "dp";
        dominoHeight = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, (int) (48 / (1 + train.length/10)) , getResources().getDisplayMetrics()));

        // Start with root domino because it has special constraints
        ImageView firstDomino = createEmptyDomino();
        dominoImageIds[train.rootIndex] = firstDomino.getId();
        parentConstraintLayout.addView(firstDomino);
        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone(parentConstraintLayout);
        constraintSet.connect(firstDomino.getId(), ConstraintSet.TOP, parentConstraintLayout.getId(), ConstraintSet.TOP);
        constraintSet.connect(firstDomino.getId(), ConstraintSet.BOTTOM, parentConstraintLayout.getId(), ConstraintSet.BOTTOM);
        // Constraint to START with margin of 4dp
        constraintSet.connect(firstDomino.getId(), ConstraintSet.START, parentConstraintLayout.getId(), ConstraintSet.START, Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, getResources().getDisplayMetrics())));
        constraintSet.constrainWidth(firstDomino.getId(), ConstraintLayout.LayoutParams.WRAP_CONTENT);
        constraintSet.constrainHeight(firstDomino.getId(), dominoHeight);
        constraintSet.applyTo(parentConstraintLayout);

        addPipImages(parentConstraintLayout, firstDomino.getId(), train.train.get(train.rootIndex));
        // Draw the rest of the dominoes
        doublesDepth[train.rootIndex] = 0;
        Stack<Integer> nodesToVisit = new Stack<>();
        nodesToVisit.push(train.train.get(train.rootIndex).nextNodeAIndex);
        while (!nodesToVisit.isEmpty()){
            int currentNodeIndex = nodesToVisit.pop();
            DominoTrainNode currentNode = train.train.get(currentNodeIndex);
            int parentNodeIndex = currentNode.parentNodeIndex;
            DominoTrainNode parentNode = train.train.get(parentNodeIndex);
            doublesDepth[currentNodeIndex] = doublesDepth[parentNodeIndex];
            if (parentNode.doubleTile){
                doublesDepth[currentNodeIndex]++;
            }
            // Domino to draw is directly ahead of its parent
            if (parentNode.nextNodeAIndex == currentNodeIndex){
                drawDomino(parentConstraintLayout, currentNodeIndex, parentNodeIndex, Direction.CENTER, currentNode, train.numDoubles);
            }
            // Domino to draw is below its parent
            else if (parentNode.nextNodeBIndex == currentNodeIndex){
                //doublesDepth[currentNodeIndex]++;
                drawDomino(parentConstraintLayout, currentNodeIndex, parentNodeIndex, Direction.BELOW, currentNode, train.numDoubles);
            }
            // Domino to draw is above its parent
            else if (parentNode.nextNodeCIndex == currentNodeIndex){
               //doublesDepth[currentNodeIndex]++;
                drawDomino(parentConstraintLayout, currentNodeIndex, parentNodeIndex, Direction.ABOVE, currentNode, train.numDoubles);
            }

            if (currentNode.nextNodeAIndex != -1){
                nodesToVisit.push(currentNode.nextNodeAIndex);
                if (currentNode.nextNodeBIndex != -1){
                    nodesToVisit.push(currentNode.nextNodeBIndex);
                    if (currentNode.nextNodeCIndex != -1){
                        nodesToVisit.push(currentNode.nextNodeCIndex);
                    }
                }
            }
        }
    }

    private void drawDomino(ConstraintLayout parentLayout, int nodeIndex, int parentIndex, Direction direction, DominoTrainNode currentNode, int numDoubles){
        // Create empty domino
        ImageView currentDomino = createEmptyDomino();
        int currentDominoId = currentDomino.getId();
        int parentDominoId = dominoImageIds[parentIndex];
        dominoImageIds[nodeIndex] = currentDominoId;
        parentLayout.addView(currentDomino);

        // Check if connecting line is needed
        // Done prior to cloning parent constraints b/c must be added now if needed later
        int connectingLineId = -1;
        int margin = -1;
        if (direction != Direction.CENTER){
            View connectingLine = new View(this);
            connectingLineId = View.generateViewId();
            connectingLine.setId(connectingLineId);
            connectingLine.setBackgroundColor(this.getColor(R.color.black));
            parentLayout.addView(connectingLine);
        }

        // Constrain empty domino with appropriate relation to their parent domino
        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone(parentLayout);
        constraintSet.connect(currentDominoId, ConstraintSet.START, parentDominoId, ConstraintSet.END);
        constraintSet.constrainWidth(currentDominoId, ConstraintLayout.LayoutParams.WRAP_CONTENT);
        constraintSet.constrainHeight(currentDominoId, dominoHeight);
        if (direction == Direction.CENTER){
            constraintSet.connect(currentDominoId, ConstraintSet.TOP, parentDominoId, ConstraintSet.TOP);
            constraintSet.connect(currentDominoId, ConstraintSet.BOTTOM, parentDominoId, ConstraintSet.BOTTOM);
        }
        // Setup connecting line for non-center position dominoes, also calculate gap distance
        else {
            constraintSet.connect(connectingLineId, ConstraintSet.START, currentDominoId, ConstraintSet.START);
            constraintSet.constrainWidth(connectingLineId, Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3, getResources().getDisplayMetrics())));
            constraintSet.constrainHeight(connectingLineId, ConstraintLayout.LayoutParams.MATCH_CONSTRAINT);
            int gap = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics()));
            margin = gap + (numDoubles - doublesDepth[nodeIndex]) * (gap + dominoHeight);
        }
        if (direction == Direction.BELOW){
            constraintSet.connect(currentDominoId, ConstraintSet.TOP, parentDominoId, ConstraintSet.BOTTOM, margin);
            constraintSet.connect(connectingLineId, ConstraintSet.BOTTOM, currentDominoId, ConstraintSet.TOP);
            constraintSet.connect(connectingLineId, ConstraintSet.TOP, parentDominoId, ConstraintSet.BOTTOM);

        }
        else if (direction == Direction.ABOVE){
            constraintSet.connect(currentDominoId, ConstraintSet.BOTTOM, parentDominoId, ConstraintSet.TOP, margin);
            constraintSet.connect(connectingLineId, ConstraintSet.TOP, currentDominoId, ConstraintSet.BOTTOM);
            constraintSet.connect(connectingLineId, ConstraintSet.BOTTOM, parentDominoId, ConstraintSet.TOP);
        }

        constraintSet.applyTo(parentLayout);
        addPipImages(parentLayout, currentDominoId, currentNode);
    }

    private ImageView createEmptyDomino(){
        ImageView domino = new ImageView(this);
        domino.setId(View.generateViewId());
        domino.setImageResource(R.drawable.blank_domino_graphic_transparent);
        domino.setAdjustViewBounds(true);
        domino.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        return domino;
    }

    // Adds the pip images (with ConstraintLayout) to a blank domino
    private void addPipImages(ConstraintLayout parentLayout, int dominoID, DominoTrainNode dominoTrainNode){
        int numberA;
        int numberB;
        if (dominoTrainNode.numberARootward){
            numberA = dominoTrainNode.domino.numberA;
            numberB = dominoTrainNode.domino.numberB;
        }
        else {
            numberA = dominoTrainNode.domino.numberB;
            numberB = dominoTrainNode.domino.numberA;
        }
        ConstraintLayout constraintLayout = new ConstraintLayout(this);
        ImageView pipsImageA = new ImageView(this);
        ImageView pipsImageB = new ImageView(this);
        int constraintLayoutId = View.generateViewId();
        int pipsImageAId = View.generateViewId();
        int pipsImageBId = View.generateViewId();
        constraintLayout.setId(constraintLayoutId);
        pipsImageA.setId(pipsImageAId);
        pipsImageB.setId(pipsImageBId);
        pipsImageA.setImageResource(getDominoPipsImageID(numberA));
        pipsImageB.setImageResource(getDominoPipsImageID(numberB));
        constraintLayout.addView(pipsImageA);
        constraintLayout.addView(pipsImageB);
        parentLayout.addView(constraintLayout);

        // Note, ConstraintSets only work on direct children of a parent layout!
        // So in this case, I first do my constraints on the ConstraintLayout that holds the two pip images,
        // then I apply those, clone that ConstraintLayout's constraints, and constrain the two pip images.
        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone(parentLayout);
        // Constrain the enclosing ConstraintLayout
        constraintSet.connect(constraintLayoutId, ConstraintSet.START, dominoID, ConstraintSet.START);
        constraintSet.connect(constraintLayoutId, ConstraintSet.END, dominoID, ConstraintSet.END);
        constraintSet.connect(constraintLayoutId, ConstraintSet.TOP, dominoID, ConstraintSet.TOP);
        constraintSet.connect(constraintLayoutId, ConstraintSet.BOTTOM, dominoID, ConstraintSet.BOTTOM);
        constraintSet.constrainHeight(constraintLayoutId, ConstraintLayout.LayoutParams.MATCH_CONSTRAINT);
        constraintSet.constrainWidth(constraintLayoutId, ConstraintLayout.LayoutParams.MATCH_CONSTRAINT);

        constraintSet.applyTo(parentLayout);
        constraintSet.clone(constraintLayout);

        // Constrain the pip images
        constraintSet.connect(pipsImageAId, ConstraintSet.START, constraintLayoutId, ConstraintSet.START);
        constraintSet.connect(pipsImageAId, ConstraintSet.END, pipsImageBId, ConstraintSet.START);
        constraintSet.connect(pipsImageAId, ConstraintSet.TOP, constraintLayoutId, ConstraintSet.TOP);
        constraintSet.connect(pipsImageAId, ConstraintSet.BOTTOM, constraintLayoutId, ConstraintSet.BOTTOM);

        constraintSet.connect(pipsImageBId, ConstraintSet.START, pipsImageAId, ConstraintSet.END);
        constraintSet.connect(pipsImageBId, ConstraintSet.END, constraintLayoutId, ConstraintSet.END);
        constraintSet.connect(pipsImageBId, ConstraintSet.TOP, constraintLayoutId, ConstraintSet.TOP);
        constraintSet.connect(pipsImageBId, ConstraintSet.BOTTOM, constraintLayoutId, ConstraintSet.BOTTOM);

        constraintSet.constrainHeight(pipsImageAId, ConstraintLayout.LayoutParams.MATCH_CONSTRAINT);
        constraintSet.constrainWidth(pipsImageAId, ConstraintLayout.LayoutParams.MATCH_CONSTRAINT);
        constraintSet.constrainHeight(pipsImageBId, ConstraintLayout.LayoutParams.MATCH_CONSTRAINT);
        constraintSet.constrainWidth(pipsImageBId, ConstraintLayout.LayoutParams.MATCH_CONSTRAINT);
        constraintSet.constrainPercentHeight(pipsImageAId, 0.8f);
        constraintSet.constrainPercentHeight(pipsImageBId, 0.8f);
        constraintSet.applyTo(constraintLayout);
    }

    // Given a number of pips, returns the ID of the corresponding image
    private int getDominoPipsImageID(int numberPips){
        String numberSuffix = "";
        switch (numberPips){
            case 1: numberSuffix = "one"; break;
            case 2: numberSuffix = "two"; break;
            case 3: numberSuffix = "three"; break;
            case 4: numberSuffix = "four"; break;
            case 5: numberSuffix = "five"; break;
            case 6: numberSuffix = "six"; break;
            case 7: numberSuffix = "seven"; break;
            case 8: numberSuffix = "eight"; break;
            case 9: numberSuffix = "nine"; break;
            case 10: numberSuffix = "ten"; break;
            case 11: numberSuffix = "eleven"; break;
            case 12: numberSuffix = "twelve"; break;
        }
        String imageName = "pips_" + numberSuffix;
        return getResources().getIdentifier(imageName, "drawable", getPackageName());
    }
}