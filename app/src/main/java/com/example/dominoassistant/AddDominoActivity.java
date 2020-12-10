package com.example.dominoassistant;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputFilter;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class AddDominoActivity extends Activity {//extends AppCompatActivity {
    //private ArrayList<Domino> dominoes;
    private String dominoesString;
    private EditText addDominoNumberA;
    private EditText addDominoNumberB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Add a domino");
        setContentView(R.layout.activity_add_domino);
        dominoesString = getIntent().getStringExtra("dominoesString");
        //dominoes = Domino.decodeDominoes(dominoesString);

        // Setup min/max range for number inputs
        addDominoNumberA = (EditText) findViewById(R.id.addDominoNumberA);
        addDominoNumberA.setFilters(new InputFilter[]{new InputFilterMinMax("0", "12")});
        addDominoNumberB = (EditText) findViewById(R.id.addDominoNumberB);
        addDominoNumberB.setFilters(new InputFilter[]{new InputFilterMinMax("0", "12")});
    }

    public void closeActivity(View view) {
        this.finish();
    }

    public void addViaCamera(View view) {
        Intent intent = new Intent(getBaseContext(), CameraActivity.class);
        intent.putExtra("dominoesString", dominoesString);
        startActivity(intent);
    }

    public void submitNewDomino(View view) {
        String numberAString = addDominoNumberA.getText().toString();
        String numberBString = addDominoNumberB.getText().toString();
        if (numberAString.isEmpty() || numberBString.isEmpty()){
            Toast.makeText(this, "Please enter the pip values", Toast.LENGTH_LONG).show();
        }
        else {
            if (Integer.parseInt(numberAString) < Integer.parseInt(numberBString)){
                dominoesString = dominoesString.concat(numberAString + "," + numberBString + ";");
            }
            else {
                dominoesString = dominoesString.concat(numberBString + "," + numberAString + ";");
            }

            Intent intent = new Intent(getBaseContext(), SelectDominoesActivity.class);
            intent.putExtra("dominoesString", dominoesString);
            startActivity(intent);
        }
    }
}