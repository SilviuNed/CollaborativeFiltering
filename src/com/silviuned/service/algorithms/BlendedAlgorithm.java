package com.silviuned.service.algorithms;

import com.silviuned.service.data.DataManager;

/**
 * Created by Silviu on 6/28/2017.
 */
public class BlendedAlgorithm implements Algorithm {

    private static final double RATIO = 0.55;
    private Algorithm algorithm1;
    private Algorithm algorithm2;

    public BlendedAlgorithm(Algorithm algorithm1, Algorithm algorithm2) {
        this.algorithm1 = algorithm1;
        this.algorithm2 = algorithm2;
    }

    @Override
    public void train() {}

    @Override
    public double predictRating(int movieId, int userId) {
        double prediction1 = algorithm1.predictRating(movieId, userId);
        double prediction2 = algorithm2.predictRating(movieId, userId);
        return prediction1 * RATIO + prediction2 * (1 - RATIO);
    }

    @Override
    public DataManager getDataManager() {
        return algorithm1.getDataManager();
    }

    @Override
    public double getNearIntegerRounderDiff() {
        return (algorithm1.getNearIntegerRounderDiff() + algorithm2.getNearIntegerRounderDiff()) / 2;
    }
}
