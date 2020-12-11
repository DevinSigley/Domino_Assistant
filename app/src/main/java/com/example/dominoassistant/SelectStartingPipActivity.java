package com.example.dominoassistant;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputFilter;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import java.util.ArrayList;

public class SelectStartingPipActivity extends Activity {
    private String dominoesString;
    private EditText pipsInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_starting_pip);
        setTitle("Select starting pips");
        dominoesString = getIntent().getStringExtra("dominoesString");

        // Setup min/max range for number inputs
        pipsInput = (EditText) findViewById(R.id.selectPipsNumberInput);
        pipsInput.setFilters(new InputFilter[]{new InputFilterMinMax("0", "12")});
    }

    public void closeActivity(View view) {
        this.finish();
    }

    public void startCalculateTrainsActivity(View view) {
        String startingPips = pipsInput.getText().toString();
        if (startingPips.isEmpty()){
            Toast.makeText(this, "Please enter the starting pip value", Toast.LENGTH_LONG).show();
        }
        else {
            ArrayList<Domino> dominoes = Domino.decodeDominoes(dominoesString);
            int pips = Integer.parseInt(startingPips);
            boolean trainPossible = false;
            for (int i = 0; i < dominoes.size() && !trainPossible; ++i){
                if (dominoes.get(i).numberA == pips || dominoes.get(i).numberB == pips){
                    trainPossible = true;
                }
            }

            if (trainPossible){
                Intent intent = new Intent(getBaseContext(), CalculateTrainsActivity.class);
                intent.putExtra("dominoesString", dominoesString);
                intent.putExtra("startingPips", startingPips);
                startActivity(intent);
                finish();
            }
            else {
                Toast.makeText(this, "No trains can be made with that starting value", Toast.LENGTH_LONG).show();
            }

        }

    }
}