package com.example.dominoassistant;

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

            resultList.add(tmpDomino);
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
}
