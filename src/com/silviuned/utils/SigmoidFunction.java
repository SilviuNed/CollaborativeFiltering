package com.silviuned.utils;

/**
 * Created by Silviu on 6/1/2017.
 */
public class SigmoidFunction {

    // Calculates the value of the sigmoid function for the specified input.
    public static double eval(double x) {
        return 1.0 / (1 + Math.pow(Math.E, -x)) - 0.5;
    }
}
