package com.silviuned.service;

import com.silviuned.model.UserRating;
import com.silviuned.service.algorithms.Algorithm;
import com.silviuned.service.algorithms.BlendedAlgorithm;
import com.silviuned.service.algorithms.EmAlgorithm;
import com.silviuned.service.data.DataManager;
import com.silviuned.service.postProcessing.GlobalBiasCorrector;
import com.silviuned.service.postProcessing.ItemBasedCorrector;
import com.silviuned.service.algorithms.SvdAlgorithm;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.silviuned.service.data.DataLoader.*;

public class Main {

	public static void main(String[] args) {
		long startTime = System.currentTimeMillis();

		// Loading the data
		Map<Integer, List<UserRating>> trainingData = loadData(TRAINING_DATA_PATH);
		Map<Integer, List<UserRating>> testData = loadData(TEST_DATA_PATH);

		// Initializing the data manager
		DataManager dm = new DataManager(trainingData);

		// Running the algorithms
		List<Algorithm> algorithms = new ArrayList<>();
		algorithms.add(new SvdAlgorithm(dm));
		algorithms.add(new EmAlgorithm(dm));
		algorithms.add(new BlendedAlgorithm(algorithms.get(0), algorithms.get(1)));

		for (Algorithm algorithm : algorithms) {
			algorithm.train();

			// Post processors
			ItemBasedCorrector itemBasedCorrector = new ItemBasedCorrector(dm, algorithm);
			GlobalBiasCorrector globalBiasCorrector = new GlobalBiasCorrector(testData, algorithm, itemBasedCorrector);

			double rmse = RmseEvaluator.eval(algorithm, testData, itemBasedCorrector, globalBiasCorrector, true);
			System.out.println("RMSE:\t" + rmse);
		}

		// Displaying the total execution time
		long sec = (System.currentTimeMillis() - startTime) / 1000;
		System.out.println("Duration: " + sec + " sec.");
	}

}
