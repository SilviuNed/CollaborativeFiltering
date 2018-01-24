package com.silviuned.service.algorithms;

import com.silviuned.service.data.DataManager;

/**
 * Created by Silviu on 5/27/2017.
 */
public interface Algorithm {

    void train();

    double predictRating(int movieId, int userId);

    DataManager getDataManager();

    double getNearIntegerRounderDiff();
}
