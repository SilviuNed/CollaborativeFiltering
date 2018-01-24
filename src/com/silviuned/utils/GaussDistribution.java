package com.silviuned.utils;

public class GaussDistribution {

	/* Calculates the value of the log probability density function for a univariate gaussian distribution,
	 * with the specified mean and variance. */
	public static double logPdf(double x, double mean, double variance) {
		if (variance != 0) {
			double result = -0.5 * Math.log(2 * Math.PI) - 0.5 * Math.log(variance)
				- Math.pow(x - mean, 2) / (2 * variance);
			return result;
		} else {
			return 0;
		}
	}

}
