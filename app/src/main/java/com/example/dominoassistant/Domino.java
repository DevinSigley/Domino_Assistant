package com.example.dominoassistant;

import androidx.annotation.Nullable;

import java.util.ArrayList;

public class Domino {
    public int numberA;
    public int numberB;

    public Domino() {
        this.numberA = -1;
        this.numberB = -1;
    }

    public Domino(int numberA, int numberB) {
        this.numberA = numberA;
        this.numberB = numberB;
    }

    // Adds dominoes to a returned ArrayList, doesn't allow duplicates
    public static ArrayList<Domino> decodeDominoes(String dominoes){
        ArrayList<Domino> resultList = new ArrayList<>();
        for (int i = 0; i < dominoes.length(); i += 4){
            Domino tmpDomino = new Domino();
            // First number is single-digit
            if (dominoes.charAt(i+1) == ','){
                tmpDomino.numberA = Character.getNumericValue(dominoes.charAt(i));
            }
            else {
                ++i; // if it's two digits, first digit is known to be a 10
                tmpDomino.numberA = 10 + Character.getNumericValue(dominoes.charAt(i));
            }
            // Second number is single-digit
            if (dominoes.charAt(i+3) == ';'){
                tmpDomino.numberB = Character.getNumericValue(dominoes.charAt(i+2));
            }
            else {
                ++i;
                tmpDomino.numberB = 10 + Character.getNumericValue(dominoes.charAt(i+2));
            }

            // don't allow duplicates
            if (!resultList.contains(tmpDomino)){
                resultList.add(tmpDomino);
            }

        }
        /*int i = 0;
        while (i < dominoes.length()){
            Domino tmpDomino = new Domino(0, 0);
            while (dominoes.charAt(i) != ','){
                if (dominoes.charAt(i+1) == ','){

                }
                tmpDomino.numberA += Character.getNumericValue(dominoes.charAt(i));
                ++i;
            }
            ++i; // currently on a ',' so go past it
            while (dominoes.charAt(i) != ';'){
                tmpDomino.numberB += Character.getNumericValue(dominoes.charAt(i));
                ++i;
            }
            ++i; // currently on a ';' so go past it
            resultList.add(tmpDomino);
        }*/
        return resultList;
    }

    public static String getDominoesString(ArrayList<Domino> dominoes){
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < dominoes.size(); ++i){
            sb.append(dominoes.get(i).numberA);
            sb.append(',');
            sb.append(dominoes.get(i).numberB);
            sb.append(';');
        }
        return sb.toString();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj instanceof  Domino){
            return (this.numberA == ((Domino) obj).numberA && this.numberB == ((Domino) obj).numberB);
        }
        return false;
        //return super.equals(obj);
    }

    public static int sumDominoes(ArrayList<Domino> dominoes){
        int result = 0;
        for (int i = 0; i < dominoes.size(); i++){
            result += dominoes.get(i).numberA;
            result += dominoes.get(i).numberB;
        }
        return result;
    }
}
