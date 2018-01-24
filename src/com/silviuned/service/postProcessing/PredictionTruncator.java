package com.silviuned.service.postProcessing;

/**
 * Created by Silviu on 6/19/2017.
 */
public class PredictionTruncator {

    // Trucates the prediction if it's not between the [0, 5] interval.
    public static double truncate(double prediction) {
        if (prediction > 5) {
            return 5;
        } else if (prediction < 0){
            return 0;
        } else {
            return prediction;
        }
    }
}
