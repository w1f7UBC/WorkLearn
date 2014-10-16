package com.AROMA.JavaSimulation;

import com.sandwell.JavaSimulation.Tester;

public class Tester_Rolos extends Tester {
	public static double max( double... values ) {

		double max = Double.NEGATIVE_INFINITY;

		for( double each : values ) {
			max = Math.max(max, each);
		}
		return max;
	}
	public static double min( double... values ) {

		double min = Double.POSITIVE_INFINITY;

		for( double each : values ) {
			min = Math.min(min, each);
		}
		return min;
	}
}
