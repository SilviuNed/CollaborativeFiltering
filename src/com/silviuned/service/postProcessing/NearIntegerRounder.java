package com.silviuned.service.postProcessing;

/**
 * Created by Silviu on 6/18/2017.
 */
public class NearIntegerRounder {

    // Rounding off the rating if the distance to the nearest integer is <= diff.
    public static double round(double rating, double diff) {
        int upper = (int) (rating + diff);
        int lower = (int) (rating - diff);
        int current = (int) rating;

        if (upper - current == 1) {
            return upper;
        } else if (current - lower == 1) {
            return current;
        } else {
            return rating;
        }
    }
}
