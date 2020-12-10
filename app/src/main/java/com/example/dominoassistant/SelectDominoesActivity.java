package com.example.dominoassistant;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Layout;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.zip.Inflater;

public class SelectDominoesActivity extends AppCompatActivity {
    private Button addDominoButton;
    private ArrayList<Domino> dominoes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Domino Selection");
        setContentView(R.layout.activity_select_dominoes);

        // Get the dominoes sent via Intent
        String dominoesString = getIntent().getStringExtra("dominoesString");
        if (dominoesString == null){
            dominoesString = "";
        }
        dominoes = Domino.decodeDominoes(dominoesString);
        // Sort by first domino number, then by second domino number, low to high
        Collections.sort(dominoes, new Comparator<Domino>() {
            @Override
            public int compare(Domino domino, Domino t1) {
                // 20 * numberA to weight it so it's the primary number to be sorted by
                return 20 * (domino.numberA - t1.numberA) + (domino.numberB - t1.numberB);
            }
        });

        TextView totalPipsText = (TextView) findViewById(R.id.totalPipsText);
        //totalPipsText.setText("Total pips: " + String.valueOf(Domino.sumDominoes(dominoes)));
        totalPipsText.setText(String.valueOf(Domino.sumDominoes(dominoes)) + " pips");


        TableLayout leftDominoesTable = findViewById(R.id.dominoesTableLayoutLeft);
        TableLayout rightDominoesTable = findViewById(R.id.dominoesTableLayoutRight);

        // For each domino, get the next TableRow, set it visible, set its pip images
        for (int i = 0; i < dominoes.size(); ++i){
            Domino currentDomino = dominoes.get(i);
            TableRow tableRow;
            if (i % 2 == 0){
                tableRow = (TableRow) leftDominoesTable.getChildAt(i/2);
            }
            else {
                tableRow = (TableRow) rightDominoesTable.getChildAt((i-1)/2);
            }
            tableRow.setVisibility(View.VISIBLE);
            String leftDominoHalfIDString = "selectDominoImage" + String.valueOf(i+1) + "A";
            String rightDominoHalfIDString = "selectDominoImage" + String.valueOf(i+1) + "B";
            int leftDominoHalfID = getResources().getIdentifier(leftDominoHalfIDString, "id", getPackageName());
            int rightDominoHalfID = getResources().getIdentifier(rightDominoHalfIDString, "id", getPackageName());
            ImageView leftDominoHalf = (ImageView) findViewById(leftDominoHalfID);
            ImageView rightDominoHalf = (ImageView) findViewById(rightDominoHalfID);

            // Set left half of domino image
            if (currentDomino.numberA == 0){
                leftDominoHalf.setVisibility(View.INVISIBLE);
            }
            else {
                int leftDominoHalfPipsImageID = getDominoPipsImageID(currentDomino.numberA);
                leftDominoHalf.setImageResource(leftDominoHalfPipsImageID);
            }
            // Set right half of domino image
            if (currentDomino.numberB == 0){
                rightDominoHalf.setVisibility(View.INVISIBLE);
            }
            else {
                int rightDominoHalfPipsImageID = getDominoPipsImageID(currentDomino.numberB);
                rightDominoHalf.setImageResource(rightDominoHalfPipsImageID);
            }
        }


    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    // Function used by the "Remove" buttons for removing a domino
    public void removeDomino(View view) {
        // Figure out which button called the function (so we know which domino to remove)
        Button callingButton = (Button) findViewById(view.getId());
        String buttonNameFull = getResources().getResourceEntryName(callingButton.getId());
        // "selectDominoRemoveButton" ends at index 23, so starting at 24 gives us a string with just the number
        int dominoNumber = Integer.parseInt(buttonNameFull.substring(24));
        dominoes.remove(dominoNumber - 1);

        // Now redirect to this Activity with the updated dominoes list, chosen domino removed
        Intent intent = new Intent(getBaseContext(), SelectDominoesActivity.class);
        intent.putExtra("dominoesString", Domino.getDominoesString(dominoes));
        startActivity(intent);
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

    // Used to start a Dialog for manually adding a Domino to the selection
    public void addDomino(View view){
        Intent intent = new Intent(getBaseContext(), AddDominoActivity.class);
        if (dominoes != null && dominoes.size() > 0){
            intent.putExtra("dominoesString", Domino.getDominoesString(dominoes));
        }
        else {
            intent.putExtra("dominoesString", "");
        }
        startActivity(intent);
    }

    public void buildTrains(View view) {
        if (dominoes != null && dominoes.size() > 0){
            Intent intent = new Intent(getBaseContext(), SelectStartingPipActivity.class);
            intent.putExtra("dominoesString", Domino.getDominoesString(dominoes));
            startActivity(intent);
        }
        else {
            Toast.makeText(this, "Please select dominoes before computing trains", Toast.LENGTH_LONG).show();
        }
    }

    // Apply custom layout to menu bar to include "Clear dominoes" button
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // R.menu.mymenu is a reference to an xml file named mymenu.xml which should be inside your res/menu directory.
        // If you don't have res/menu, just create a directory named "menu" inside res
        getMenuInflater().inflate(R.menu.select_dominoes_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    // Handle "Clear dominoes" button from action bar
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_clear_dominoes) {
            Intent intent = new Intent(getBaseContext(), SelectDominoesActivity.class);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }
}


// This was an attempt to dynamically create the View and add dominoes to it.
// Wasn't successful because Views with wrap_content dimensions failed to consider
// the Views added to them dynamically.
/*
        Resources r = getResources();
        String dominoesString = getIntent().getStringExtra("dominoesString");
        //TextView dominoesText = findViewById(R.id.dominoesTextView);
        //dominoesText.setText(dominoesString);
        ArrayList<Domino> dominoes = Domino.decodeDominoes(dominoesString);

        //ConstraintLayout parentConstraintLayout = new ConstraintLayout(this);
        //parentConstraintLayout.setLayoutParams(new ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.MATCH_PARENT));
        int parentConstraintLayoutID = View.generateViewId();

        LayoutInflater inflater = getLayoutInflater();
        ConstraintLayout parentConstraintLayout = (ConstraintLayout) inflater.inflate(R.layout.button_layout_test, null);
        parentConstraintLayout.setId(parentConstraintLayoutID);
        // TO DO: is this okay?:
        setContentView(parentConstraintLayout);

        // Setup left and right tables to hold dominoes and their remove buttons
        TableLayout leftDominoTable = new TableLayout(this);
        TableLayout.LayoutParams leftTableParams = new TableLayout.LayoutParams(TableLayout.LayoutParams.WRAP_CONTENT, TableLayout.LayoutParams.WRAP_CONTENT);
        int dpiToPix8 = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, r.getDisplayMetrics());
        int dpiToPix16 = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, r.getDisplayMetrics());
        int dpiToPix24 = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24, r.getDisplayMetrics());
        leftTableParams.leftMargin = dpiToPix16;
        leftTableParams.topMargin = dpiToPix24;
        leftDominoTable.setLayoutParams(leftTableParams);
        int leftDominoTableID = View.generateViewId();
        leftDominoTable.setId(leftDominoTableID);

        ConstraintSet constraints = new ConstraintSet();
        constraints.clone(parentConstraintLayout);
        constraints.connect(leftDominoTableID, ConstraintSet.TOP, parentConstraintLayoutID, ConstraintSet.TOP);
        constraints.connect(leftDominoTableID, ConstraintSet.START, parentConstraintLayoutID, ConstraintSet.START);
        parentConstraintLayout.addView(leftDominoTable);

        TableLayout rightDominoTable = new TableLayout(this);
        TableLayout.LayoutParams rightTableParams = new TableLayout.LayoutParams(TableLayout.LayoutParams.WRAP_CONTENT, TableLayout.LayoutParams.WRAP_CONTENT);
        rightTableParams.rightMargin = dpiToPix16;
        rightTableParams.topMargin = dpiToPix24;
        rightDominoTable.setLayoutParams(rightTableParams);
        int rightDominoTableID = View.generateViewId();
        rightDominoTable.setId(rightDominoTableID);
        constraints.connect(rightDominoTableID, ConstraintSet.TOP, parentConstraintLayoutID, ConstraintSet.TOP);
        constraints.connect(rightDominoTableID, ConstraintSet.END, parentConstraintLayoutID, ConstraintSet.END);
        parentConstraintLayout.addView(rightDominoTable);

        // TO DO: Add table rows, dominoes, and their remove buttons here (don't forget invisible button for spacing):


        // Creating the "Add Domino" and "Submit" buttons
        // First creating the layout they're in
        LinearLayout bottomButtonsLayout = new LinearLayout(this);
        parentConstraintLayout.addView(bottomButtonsLayout);
        bottomButtonsLayout.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams bottomButtonsLayoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        bottomButtonsLayoutParams.leftMargin = dpiToPix8;
        bottomButtonsLayoutParams.rightMargin = dpiToPix8;
        //bottomButtonsLayout.setLayoutParams(bottomButtonsLayoutParams);
        int bottomButtonsLayoutID = View.generateViewId();
        bottomButtonsLayout.setId(bottomButtonsLayoutID);
        constraints.connect(bottomButtonsLayoutID, ConstraintSet.START, parentConstraintLayoutID, ConstraintSet.START);
        constraints.connect(bottomButtonsLayoutID, ConstraintSet.END, parentConstraintLayoutID, ConstraintSet.END);
        //constraints.connect(bottomButtonsLayoutID, ConstraintSet.TOP, parentConstraintLayoutID, ConstraintSet.TOP);
        constraints.connect(bottomButtonsLayoutID, ConstraintSet.BOTTOM, parentConstraintLayoutID, ConstraintSet.BOTTOM);
        //parentConstraintLayout.addView(bottomButtonsLayout);

        //Button addDominoButton = new Button(this);
        addDominoButton = new Button(this);
        int addDominoButtonID = View.generateViewId();
        addDominoButton.setId(addDominoButtonID);
        addDominoButton.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        bottomButtonsLayout.addView(addDominoButton);
        addDominoButton.setText("Add Domino");
        addDominoButton.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#2196F3"))); // set button to blue

        addDominoButton.requestLayout();
        addDominoButton.invalidate();
        addDominoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CharSequence toastText = "Clicked 'Add Domino'";
                Toast.makeText(getApplicationContext(), toastText, Toast.LENGTH_SHORT).show();
            }
        });

        ImageView testDominoImage = new ImageView(this);
        testDominoImage.setId(View.generateViewId());
        testDominoImage.setImageResource(R.drawable.blank_domino_graphic_transparent);
        bottomButtonsLayout.addView(testDominoImage);

        int visibility = bottomButtonsLayout.getVisibility();
        bottomButtonsLayout.setVisibility(View.GONE);
        bottomButtonsLayout.setVisibility(visibility);



        //LayoutInflater inflater = getLayoutInflater();
        //View inflatedBottomButtons = inflater.inflate(findViewById(R.id.addSubmitButtonsLayout), parentConstraintLayout, true);
        constraints.applyTo(parentConstraintLayout);
        //setContentView(parentConstraintLayout);
        //bottomButtonsLayout.requestLayout();
        //bottomButtonsLayout.invalidate();
        ConstraintLayout.LayoutParams testParams = new ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.WRAP_CONTENT);
        //bottomButtonsLayout.setLayoutParams(testParams);

        setContentView(R.layout.activity_select_dominoes);
         */