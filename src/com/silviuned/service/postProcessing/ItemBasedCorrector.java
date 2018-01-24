package com.silviuned.service.postProcessing;

import com.silviuned.model.UserRating;
import com.silviuned.service.algorithms.Algorithm;
import com.silviuned.service.data.DataManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Silviu on 6/18/2017.
 * Reduces the RMSE, by shifting the predictions.
 * The shifting value is different for each movie.
 * It represents the difference between the rating mean and the prediction mean.
 */
public class ItemBasedCorrector {

    private Map<Integer, Double> movieAveragePrediction = new HashMap<>();

    public ItemBasedCorrector(DataManager dataManager, Algorithm algorithm) {
        for (Map.Entry<Integer, List<UserRating>> entry : dataManager.getData().entrySet()) {
            int movieId = entry.getKey();
            double ratingMean = dataManager.getAverageRating(movieId);
            double predictionMean = 0;
            int count = 0;
            for (UserRating userRating : entry.getValue()) {
                double prediction = algorithm.predictRating(movieId, userRating.getUserId());
                predictionMean += prediction;
                count++;
            }
            predictionMean /= count;
            movieAveragePrediction.put(movieId, ratingMean - predictionMean);
        }
    }

    public double correct(int movieId, double prediction) {
        if (movieAveragePrediction.containsKey(movieId)) {
            return prediction + movieAveragePrediction.get(movieId);
        } else {
            return prediction;
        }
    }
}
