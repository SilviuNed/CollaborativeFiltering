package com.silviuned.service.data;

import com.silviuned.model.UserRating;
import com.silviuned.service.RmseEvaluator;
import com.silviuned.service.algorithms.Algorithm;

import java.util.*;

/**
 * Created by Silviu on 5/27/2017.
 */
public class DataManager {

    // movieId -> list of ratings for this movie
    private Map<Integer, List<UserRating>> data;

    // What follows is duplicate data, cached for performance considerations.

    // userId -> list of movies rated by the user
    private Map<Integer, List<Integer>> userRatingsMap;

    private double globalMovieRatingAverage;
    private Map<Integer, Double> movieRatingAverageMap;

    // User offset refers to the offset between the user's rating for a movie and the average rating for that movie.
    private double globalUserOffsetAverage;
    private Map<Integer, Double> userOffsetAverageMap;

    private static final double DEFAULT_RATIO = 25;


    public DataManager(Map<Integer, List<UserRating>> data) {
        this.data = data;
        this.userRatingsMap = new HashMap<>();
        this.movieRatingAverageMap = new HashMap<>();
        this.userOffsetAverageMap = new HashMap<>();

        initCache();
    }

    private void initCache() {
        // Calulating movieRatingAverageMap & extracting userRatingsMap.
        for (Map.Entry<Integer, List<UserRating>> entry : data.entrySet()) {
            int sum = 0;
            int movieId = entry.getKey();

            for (UserRating userRating : entry.getValue()) {
                int userId = userRating.getUserId();

                /* Adding rating to the userRatingMap */
                if (userRatingsMap.containsKey(userId)) {
                    userRatingsMap.get(userId).add(movieId);
                } else {
                    List<Integer> list = new ArrayList<>();
                    list.add(movieId);
                    userRatingsMap.put(userId, list);
                }

                /* Calulating sun of ratings for the movieRatingAverageMap. */
                sum += userRating.getRating();
            }
            double movieRatingAverage = sum * 1.0 / entry.getValue().size();
            movieRatingAverageMap.put(entry.getKey(), movieRatingAverage);

            globalMovieRatingAverage += movieRatingAverage;
        }

        // Calculating globalMovieRatingAverage
        globalMovieRatingAverage /= movieRatingAverageMap.size();


        // Calculating userOffsetAverageMap
        for (Map.Entry<Integer, List<Integer>> entry : userRatingsMap.entrySet()) {
            double totalOffset = 0;
            for (Integer movieId : entry.getValue()) {
                totalOffset += getRating(entry.getKey(), movieId) - getAverageRating(movieId);
            }
            double userOffsetAverage = totalOffset / entry.getValue().size();
            userOffsetAverageMap.put(entry.getKey(), userOffsetAverage);

            globalUserOffsetAverage += userOffsetAverage;
        }

        // Calculating globalUserOffsetAverage.
        globalUserOffsetAverage /= userOffsetAverageMap.size();
    }

    // Returns true if the user exists. Returns false, otherwise.
    public boolean existsUser(int userId) {
        return userRatingsMap.get(userId) != null;
    }

    // Returns true if the movie exists. Returns false, otherwise.
    public boolean existsMovie(int movieId) {
        return data.get(movieId) != null;
    }

    // Returns the total nr of users.
    public int getNrUsers() {
        return userRatingsMap.size();
    }

    // Returns the total nr of movies.
    public int getNrMovies() {
        return data.size();
    }

    // Returns the rating the user gave to the movie.
    public short getRating(int userId, int movieId) {
        for (UserRating userRating : data.get(movieId)) {
            if (userRating.getUserId() == userId) {
                return userRating.getRating();
            }
        }
        throw new java.util.NoSuchElementException();
    }

    // Returns the movie's average rating if the movie exists, and the global average otherwise.
    public double getAverageRating(int movieId) {
        Double result = movieRatingAverageMap.get(movieId);
        return result != null ? result : globalMovieRatingAverage;
    }

    // Returns the "true" (statistical) average of the movie.
    public double getTrueAverageMovieRating(int movieId) {
        List<UserRating> userRatings = data.get(movieId);
        if (userRatings == null) {
            return globalMovieRatingAverage;
        } else if (userRatings.size() == 1) {
            return (movieRatingAverageMap.get(movieId) + globalMovieRatingAverage) / 2;
        } else {
            double ratio = DEFAULT_RATIO;
            double numerator = globalMovieRatingAverage * ratio + movieRatingAverageMap.get(movieId) * userRatings.size();
            double denominator = ratio + userRatings.size();
            return numerator / denominator;
        }
    }

    // Returns the "true" (statistical) average offset of the user.
    public double getTrueAverageUserOffset(int userId) {
        List<Integer> movieIds = userRatingsMap.get(userId);
        if (movieIds == null) {
            return globalUserOffsetAverage;
        } else if (movieIds.size() == 1) {
            return (userOffsetAverageMap.get(userId) + globalUserOffsetAverage) / 2;
        } else {
            double ratio = DEFAULT_RATIO;
            double numerator = globalUserOffsetAverage * ratio + userOffsetAverageMap.get(userId) * movieIds.size();
            double denominator = ratio + movieIds.size();
            return numerator / denominator;
        }
    }

    // Returns the RMSE of running the provided algorithm on the current data.
    public double getRmse(Algorithm algorithm) {
        return RmseEvaluator.eval(algorithm, data);
    }

    public Map<Integer, List<UserRating>> getData() {
        return data;
    }

    public Map<Integer, List<Integer>> getUserRatingsMap() {
        return userRatingsMap;
    }

}
