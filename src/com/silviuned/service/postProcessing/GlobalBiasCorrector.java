package com.silviuned.service.postProcessing;

import com.silviuned.model.UserRating;
import com.silviuned.service.algorithms.Algorithm;

import java.util.List;
import java.util.Map;

/**
 * Created by Silviu on 6/19/2017.
 * Reduces the RMSE, by shifting the predictions with a constant 'correction'.
 * Correction represents the difference between the actual mean and the predicted mean.
 */
public class GlobalBiasCorrector {

    private final double correction;

    public GlobalBiasCorrector(Map<Integer, List<UserRating>> testData, Algorithm algorithm, ItemBasedCorrector itemBasedCorrector) {
        int count = 0;
        double predictionMean = 0;
        double actualMean = 0;

        for (Map.Entry<Integer, List<UserRating>> entry : testData.entrySet()) {
            int movieId = entry.getKey();
            for (UserRating userRating : entry.getValue()) {
                actualMean += userRating.getRating();
                double temp = algorithm.predictRating(movieId, userRating.getUserId());
                if (itemBasedCorrector != null) {
                    predictionMean += itemBasedCorrector.correct(movieId, temp);
                } else {
                    predictionMean += temp;
                }

                count++;
            }
        }

        correction = (actualMean - predictionMean) / count;
    }

    public double correct(double prediction) {
        return prediction + correction;
    }
}
