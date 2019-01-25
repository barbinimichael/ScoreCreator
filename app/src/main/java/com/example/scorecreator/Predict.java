package com.example.scorecreator;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by micha on 4/6/2018.
 */

public class Predict {

    double[][] layer1Params;

    double[][] layer2Params;

    double [][] layer3Params;

    int[] input;

    String LOG_TAG = "Predicting";


    public Predict(int[] input, Context context) throws IOException {
        Log.d(LOG_TAG, "Prediction ");

        //InputStream is = context.getResources().openRawResource(R.raw.layer1weights);
        //layer1Params = load(25, 56449, is);

        //is = context.getResources().openRawResource(R.raw.layer2weights);
        //layer2Params = load(3, 26, is);

        InputStream is = context.getResources().openRawResource(R.raw.layer1weights_u);
        layer1Params = load(100, 5002, is);

        is = context.getResources().openRawResource(R.raw.layer2weights_u);
        layer2Params = load(50, 101, is);

        is = context.getResources().openRawResource(R.raw.layer3weights_u);
        layer3Params = load(3, 51, is);

		/*for(double[] layer: layer1Params) {
			for(double num: layer) {
				System.out.println(num);
			}
		}*/

        this.input = input;
    }

    private double[][] load(int layerRowSize, int layerColumnSize, InputStream is) throws IOException {
        double[][] layerWeights = new double[layerRowSize][layerColumnSize];

        BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));

        String line = reader.readLine();
        int rowCounter = 0;
        int columnCounter = 0;
        int current = 0;

        while (line != null) {

            while (!line.isEmpty() && line.indexOf(" ", current) > current) {
                layerWeights[rowCounter][columnCounter] = Double.parseDouble(line.substring(current, line.indexOf(" ", current)));

                current = line.indexOf(" ", current) + 1;

                columnCounter++;

                if (columnCounter >= layerColumnSize) {
                    rowCounter++;
                    columnCounter = 0;
                }
            }
            line = reader.readLine();
            current = 0;
        }
        reader.close();
        return layerWeights;
    }

    public String predict() {
        // First Layer
        double[][] outputOne = new double[1][input.length + 1]; // Size 1 x 56449
        for (int i = 0; i < input.length; i++) {
            outputOne[0][i + 1] = input[i];
        }
        // Add bias unit
        outputOne[0][0] = 1;
        // Multiply input by theta
        outputOne = multiply(outputOne, transpose(layer1Params)); // Size 1 x 25 = Size 1 x 56449 * Size (25*56449)'
        // Sigmoid function
        outputOne = sigmoid(outputOne); // Size 1 x 25


        // Second Layer
        double[][] outputTwo = new double[1][layer2Params[0].length + 1]; // Size 1 x 26
        for (int i = 0; i < outputOne[0].length; i++) {
            outputTwo[0][i + 1] = outputOne[0][i];
        }
        // Add bias unit
        outputTwo[0][0] = 1;
        // Multiply input by theta
        outputTwo = multiply(outputTwo, transpose(layer2Params)); // Size 1 x 3 = Size 1 x 26 * Size (3 x 26)'
        outputTwo = sigmoid(outputTwo);

        // Third Layer
        double[][] outputThree = new double[1][layer3Params[0].length + 1]; // Size 1 x 26
        for (int i = 0; i < outputTwo[0].length; i++) {
            outputThree[0][i + 1] = outputTwo[0][i];
        }
        // Add bias unit
        outputThree[0][0] = 1;
        // Multiply input by theta
        outputThree = multiply(outputThree, transpose(layer3Params)); // Size 1 x 3 = Size 1 x 26 * Size (3 x 26)'
        outputThree = sigmoid(outputThree);

        double max = outputThree[0][0];
        int maxPlace = 0;
        for (int i = 0; i < outputThree[0].length; i++) {
            if (outputThree[0][i] > max) {
                max = outputThree[0][i];
                maxPlace = i;
            }
            //System.out.println(outputTwo[0][i]);
        }

        String type = "unknown"; // Predicted genre
        switch(maxPlace) {
            case 0: type = "Hip-Hop/Rap";
                break;
            case 1: type = "Electronic";
                break;
            case 2: type = "Soul & R/B";
                break;
        }
        return type;
    }

    private double[][] sigmoid(double[][] input) {
        double[][] sMatrix = new double[input.length][input[0].length];
        for (int i = 0; i < input.length; i++) {
            for (int c = 0; c < input[0].length; c++) {
                sMatrix[i][c] = 1.0 / (1.0 + Math.exp(-input[i][c]));
            }
        }
        return sMatrix;
    }

    private double[][] multiply(double[][] inputOne, double[][] inputTwo) {
        double[][] output = new double[inputOne.length][inputTwo[0].length];

        for (int i = 0; i < inputOne.length; i++) {
            for (int j = 0; j < inputTwo[0].length; j++) {
                for (int k = 0; k < inputTwo.length; k++) {
                    output[i][j] += inputOne[i][k] * inputTwo[k][j];
                }
            }
        }
        return output;
    }

    private double[][] transpose(double[][] input) {
        double[][] tMatrix = new double[input[0].length][input.length];
        for (int r = 0; r < input.length; r++) {
            for (int c = 0; c < input.length; c++) {
                tMatrix[c][r] = input[r][c];
            }
        }
        return tMatrix;
    }
}
