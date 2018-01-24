package com.silviuned.service;

import com.silviuned.model.UserRating;
import com.silviuned.service.algorithms.Algorithm;
import com.silviuned.service.postProcessing.GlobalBiasCorrector;
import com.silviuned.service.postProcessing.ItemBasedCorrector;
import com.silviuned.service.postProcessing.NearIntegerRounder;
import com.silviuned.service.postProcessing.PredictionTruncator;

import java.util.List;
import java.util.Map;

/**
 * Created by Silviu on 5/27/2017.
 */
public class RmseEvaluator {

    // Calculates the RMSE of running the specified algorithm on the provided data. No post processors are used.
    public static double eval(Algorithm algorithm, Map<Integer, List<UserRating>> data) {
        return eval(algorithm, data, null, null, false);
    }

    // Calculates the RMSE of running the specified algorithm on the provided data, with the specified processors/
    public static double eval(Algorithm algorithm, Map<Integer, List<UserRating>> data,
                              ItemBasedCorrector itemBasedCorrector, GlobalBiasCorrector globalBiasCorrector,
                              boolean useNearIntergerRounding) {
        int count = 0;
        double sum = 0;

        for (Map.Entry<Integer, List<UserRating>> entry : data.entrySet()) {
            int movieId = entry.getKey();
            for (UserRating userRating : entry.getValue()) {
                count++;
                Double prediction = algorithm.predictRating(movieId, userRating.getUserId());

                if (itemBasedCorrector != null){
                    prediction = itemBasedCorrector.correct(movieId, prediction);
                }

                if (globalBiasCorrector != null) {
                    prediction = globalBiasCorrector.correct(prediction);
                }

                prediction = PredictionTruncator.truncate(prediction);

                if (useNearIntergerRounding) {
                    prediction = NearIntegerRounder.round(prediction, algorithm.getNearIntegerRounderDiff());
                }

                sum += Math.pow(userRating.getRating() - prediction, 2);
            }
        }

        return sum / count;
    }
}
