package com.silviuned.service.algorithms;

import com.silviuned.model.UserRating;
import com.silviuned.service.data.DataManager;
import com.silviuned.service.RmseEvaluator;
import com.silviuned.service.postProcessing.PredictionTruncator;
import com.silviuned.utils.SigmoidFunction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Silviu on 5/15/2017.
 */
public class SvdAlgorithm implements Algorithm {

    private static final boolean USE_LIMIT = true;
    private static final int FEATURE_LIMIT = 22;

    private static final boolean USE_SIGMOID = true;
    private static final int NR_NON_LINEAR_FEATURES = 12;

    private static final boolean USE_CACHE = true;
    private static final double INITIAL_FEATURE_VALUE = 0.1;
    private static final double LEARNING_RATE = 0.0004;
    private static final double RMSE_DIFFERENCE = -0.00008;
    private static final double REGULARIZATION_COEF = 0.02;

    private static final double NEAR_INTEGER_ROUNDER_DIFF = 0.001;

    private static final boolean VERBOSE = false;

    private DataManager dm;

    private Map<Integer, Map<Integer, Double>> predictionCache = new HashMap<>();

    private Map<Integer, List<Double>> userFeatureVectors;
    private Map<Integer, List<Double>> movieFeatureVectors;

    private int currentFeature = -1;
    private int lastCachedFeature = -1;

    public SvdAlgorithm(DataManager dm) {
        this.dm = dm;

        userFeatureVectors = new HashMap<>(dm.getNrUsers());
        for (Integer userId : dm.getUserRatingsMap().keySet()) {
            userFeatureVectors.put(userId, new ArrayList<>());
        }

        movieFeatureVectors = new HashMap<>(dm.getNrMovies());
        for (Map.Entry<Integer, List<UserRating>> entry : dm.getData().entrySet()) {
            movieFeatureVectors.put(entry.getKey(), new ArrayList<>());

            Map<Integer, Double> movieCache = new HashMap<>();
            for (UserRating userRating : entry.getValue()) {
                movieCache.put(userRating.getUserId(), 0.0);
            }
            predictionCache.put(entry.getKey(), movieCache);
        }
    }

    @Override
    public void train() {
        double previousOuterRmse = -1;
        double currentRmse = -1;

        while (previousOuterRmse == -1 || currentRmse - previousOuterRmse < RMSE_DIFFERENCE) {
            currentFeature++;
            previousOuterRmse = currentRmse;

            // Initialize the new feature everywhere.
            for (List<Double> list : userFeatureVectors.values()) {
                list.add(INITIAL_FEATURE_VALUE);
            }
            for (List<Double> list : movieFeatureVectors.values()) {
                list.add(INITIAL_FEATURE_VALUE);
            }

            double previousInnerRmse = -1;
            currentRmse = -1;

            while (previousInnerRmse == -1 || currentRmse - previousInnerRmse < RMSE_DIFFERENCE) {
                previousInnerRmse = currentRmse;
                for (Map.Entry<Integer, List<UserRating>> entry : dm.getData().entrySet()) {
                    int movieId = entry.getKey();
                    for (UserRating userRating : entry.getValue()) {
                        int userId = userRating.getUserId();
                        double prediction;
                        if (currentFeature == 0 && currentRmse == -1) {
                            prediction = predictRatingInit(movieId, userId);

                        } else {
                            prediction = predictRating(movieId, userId);
                        }

                        double error = userRating.getRating() - prediction;

                        double userFeatureOld = userFeatureVectors.get(userId).get(currentFeature);
                        double movieFeatureOld = movieFeatureVectors.get(movieId).get(currentFeature);
                        double userDelta = LEARNING_RATE * (error * movieFeatureOld - REGULARIZATION_COEF * userFeatureOld);
                        double movieDelta = LEARNING_RATE * (error * userFeatureOld - REGULARIZATION_COEF * movieFeatureOld);

                        double userFeatureNew = userFeatureOld + userDelta;
                        double movieFeatureNew = movieFeatureOld + movieDelta;
                        userFeatureNew = limit(userFeatureNew);
                        movieFeatureNew = limit(movieFeatureNew);
                        userFeatureVectors.get(userId).set(currentFeature, userFeatureNew);
                        movieFeatureVectors.get(movieId).set(currentFeature, movieFeatureNew);
                    }
                }
                currentRmse = RmseEvaluator.eval(this, dm.getData());
            }

            updateCache();
            if (VERBOSE) {
                System.out.println(currentFeature + ": " + currentRmse);
            }
        }
    }

    @Override
    public double predictRating(int movieId, int userId) {
        double result;
        if (dm.existsMovie(movieId) && dm.existsUser(userId)) {
            if (USE_CACHE) {
                Double cachedPrediction = predictionCache.get(movieId).get(userId);
                if (cachedPrediction != null) {
                    if (currentFeature > lastCachedFeature) {
                        double temp = userFeatureVectors.get(userId).get(currentFeature) * movieFeatureVectors
                                .get(movieId).get(currentFeature);
                        if (USE_SIGMOID && currentFeature <= NR_NON_LINEAR_FEATURES) {
                            temp = SigmoidFunction.eval(temp);
                        }
                        result = cachedPrediction + temp;
                    } else {
                        result = cachedPrediction;
                    }
                } else {
                    // This combination of user <-> movie was not found in train data.
                    result = predictRatingWithoutCache(movieId, userId);
                }
            } else {
                result = predictRatingWithoutCache(movieId, userId);
            }
        } else {
            result = dm.getTrueAverageMovieRating(movieId)
                    + dm.getTrueAverageUserOffset(userId);
        }

        result = PredictionTruncator.truncate(result);

        return result;
    }

    // Predicts the rating without using the cache.
    // Usually called when USE_CACHE is false or when the <movie, user> tuple was not found in the cache.
    private double predictRatingWithoutCache(int movieId, int userId) {
        List<Double> movieFeatures = movieFeatureVectors.get(movieId);
        List<Double> userFeatures = userFeatureVectors.get(userId);

        double result = 0;
        for (int i = 0; i <= currentFeature; i++) {
            double temp = movieFeatures.get(i) * userFeatures.get(i);

            if (USE_SIGMOID && i <= NR_NON_LINEAR_FEATURES) {
                result += SigmoidFunction.eval(temp);
            } else {
                result += temp;
            }
        }

        result = PredictionTruncator.truncate(result);

        return result;
    }

    // Returns the initial prediction for the <movie, user> tuple.
    private double predictRatingInit(int movieId, int userId)
    {
        double result = dm.getTrueAverageMovieRating(movieId) + dm.getTrueAverageUserOffset(userId);
        result = PredictionTruncator.truncate(result);
        return result;
    }

    @Override
    public DataManager getDataManager() {
        return dm;
    }

    // Updates the cache
    private void updateCache() {
        for (Map.Entry<Integer, Map<Integer, Double>>  entry : predictionCache.entrySet()) {
            int movieId = entry.getKey();
            for (Map.Entry<Integer, Double> userEntry : entry.getValue().entrySet()) {
                double temp = userFeatureVectors.get(userEntry.getKey()).get(currentFeature) * movieFeatureVectors.get(movieId).get(currentFeature);
                if (USE_SIGMOID && currentFeature <= NR_NON_LINEAR_FEATURES) {
                    temp = SigmoidFunction.eval(temp);
                }
                userEntry.setValue(userEntry.getValue() + temp);
            }
            lastCachedFeature = currentFeature;
        }
    }

    // Applies limit filtering to the provided feature (truncates the feature if it's too small/large).
    private double limit(double x) {
        if (USE_LIMIT) {
            if (x > FEATURE_LIMIT) {
                return FEATURE_LIMIT;
            } else if (x < FEATURE_LIMIT * (-1)) {
                return FEATURE_LIMIT * (-1);
            }
        }

        return x;
    }

    @Override
    public double getNearIntegerRounderDiff() {
        return NEAR_INTEGER_ROUNDER_DIFF;
    }

}
