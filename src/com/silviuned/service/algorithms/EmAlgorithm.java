package com.silviuned.service.algorithms;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.silviuned.model.UserRating;
import com.silviuned.service.data.DataManager;
import com.silviuned.utils.GaussDistribution;

public class EmAlgorithm implements Algorithm {

	// Value chosen in overflow in step E.
	private static final double EPSILON = Math.pow(0.1, 11);

	// The additive smoothing parameter. (aka Laplace smoothing, aka Lidstone smoothing). Step M.
	private static double ALPHA = 0.35;

	// Parameter chosen so that e^-k will not be underflow in step E.
	private static final double K = 24;

	private static final int NR_OF_GROUPS = 17;

	private static double RMSE_DIFF = 0.004;

	private static final double NEAR_INTEGER_ROUNDER_DIFF = 0.01;

	private static final boolean VERBOSE = false;
	
	/* The latent/hidden random variables.
	 * q[g][m] denotes the probability that movie m belongs to group g.
	 */
	private Table<Integer, Integer, Double> q;
	
	/*
	 * The ratings of user u for group g follows a Gaussian distribution with
	 * mean mean[g][u] and variance variance[g][u].
	 */
	private Table<Integer, Integer, Double> mean;
	private Table<Integer, Integer, Double> variance;
	
	private DataManager dm;

	public EmAlgorithm(DataManager dm) {
		this.dm = dm;
		this.q = HashBasedTable.create();
		this.mean = HashBasedTable.create();
		this.variance = HashBasedTable.create();
	}

	@Override
	public void train() {
		long startTime = System.currentTimeMillis();
		if (VERBOSE) {
			System.out.println("Nr groups:\t" + NR_OF_GROUPS);
		}
		
		// Randomly generate the values for the hidden variables.
		generateInitialLatentVariables();

		mStep();
		double currentRmse = dm.getRmse(this);
		if (VERBOSE) {
			System.out.println("0:\tRMSE: " + currentRmse + "\t" + getTime(System.currentTimeMillis() - startTime));
		}

		int itteration = 0;
		double previousRmse = -1;
		while (previousRmse == -1 || previousRmse - currentRmse > RMSE_DIFF) {
			itteration++;
			previousRmse = currentRmse;
			eStep();
			mStep();
			currentRmse = dm.getRmse(this);
			if (VERBOSE) {
				System.out.println(itteration + ":\tRMSE: " + currentRmse + "\t" + getTime(System.currentTimeMillis() - startTime));
			}
		}
	}

	@Override
	public double predictRating(int movieId, int userId) {
		double result;
		if (dm.existsMovie(movieId) && dm.existsUser(userId)) {
			result = 0;
			for (int g = 0; g < NR_OF_GROUPS; g++) {
				result += (q.get(g, movieId) * mean.get(g, userId));
			}
		} else {
			result = dm.getTrueAverageMovieRating(movieId)
					+ dm.getTrueAverageUserOffset(userId);
		}

		if (result > 5) {
			result = 5;
		} else if (result < 0){
			result = 0;
		}

		return result;
	}

	@Override
	public DataManager getDataManager() {
		return dm;
	}

	// Randomly generates the initial values for the latent variables
	private void generateInitialLatentVariables() {
		for (int m : dm.getData().keySet()) {
			double sum = 0;
			for (int g = 0; g < NR_OF_GROUPS; g++) {
				q.put(g, m, Math.random());
				sum += q.get(g, m);
			}
			// Sum of all probabilities for a movie should add up to 1.
			for (int g = 0; g < NR_OF_GROUPS; g++) {
				if (q.get(g, m) != 0) {
					q.put(g, m, q.get(g, m) / sum);
				}
			}
		}
	}

	// Performs the E-step (one iteration).
	// Values were scaled to make them numerically stable.
	private void eStep() {
		for (int m : dm.getData().keySet()) {
			double maxExp = -Double.MAX_VALUE;
			double[] numerators = new double[NR_OF_GROUPS];
			for (int g = 0; g < NR_OF_GROUPS; g++) {

				numerators[g] = Math.max(Math.log(q.get(g, m)), 0);
				for (UserRating userRating : dm.getData().get(m)) {
					double gauss = GaussDistribution.logPdf(userRating.getRating(), mean.get(g, userRating.getUserId()), variance.get(g, userRating.getUserId()));
					numerators[g] += gauss;
				}
				if (numerators[g] > maxExp) {
					maxExp = numerators[g];
				}
			}

			double commonDenominator = 0;
			for (int g = 0; g < NR_OF_GROUPS; g++) {
				numerators[g] -= maxExp;
				if (numerators[g] < -1 * K) {
					numerators[g] = EPSILON;
				} else {
					numerators[g] = Math.exp(numerators[g]);
				}
				commonDenominator += numerators[g];
			}

			for (int g = 0; g < NR_OF_GROUPS; g++) {
				double result;
				if (numerators[g] == 0) {
					result = 0;
				} else {
					result = numerators[g] / commonDenominator;
				}

				q.put(g, m, result);
			}
		}
	}

	// Performs the M-step (one iteration).
	private void mStep() {
		for (int u : dm.getUserRatingsMap().keySet()) {
			for (int g = 0; g < NR_OF_GROUPS; g++) {
			    double numerator = 0;
				double denominator = 0;
				for (int m : dm.getUserRatingsMap().get(u)) {
					denominator += q.get(g, m);
					numerator += q.get(g, m) * dm.getRating(u, m);
				}

				// Smoothing
				double d = dm.getUserRatingsMap().get(u).size();
				double result = (numerator + ALPHA) / (denominator + ALPHA * d);
				mean.put(g, u, result);
			}
		}
		
		for (int u : dm.getUserRatingsMap().keySet()) {
			for (int g = 0; g < NR_OF_GROUPS; g++) {
			    double numerator = 0;
				double denominator = 0;
				for (int m : dm.getUserRatingsMap().get(u)) {
					denominator += q.get(g, m);
					numerator += q.get(g, m) * Math.pow(dm.getRating(u, m) - mean.get(g, u), 2);
				}

				// Smoothing
				double d = dm.getUserRatingsMap().get(u).size();
				double result = (numerator + ALPHA) / (denominator + ALPHA  * d);
				variance.put(g, u, result);
			}
		}
	}

	// Returns formated time as string [i.e. "(3 min 10 sec)"]
	private String getTime(long ms) {
		long seconds = ms / 1000;
		long minutes = seconds / 60;
		seconds -= minutes * 60;

		return "(" + minutes + " min " + seconds + " sec)";
	}

	@Override
	public double getNearIntegerRounderDiff() {
		return NEAR_INTEGER_ROUNDER_DIFF;
	}
}
