package it.geosolutions.geobatch.gaez.utils.aggregator;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.keyvalue.MultiKey;
import org.jaitools.media.jai.classifiedstats.Result;

public abstract class Aggregator {

	public abstract Map<MultiKey, AggregableStats> aggregate(
			List<Map<MultiKey, List<Result>>> splittedResults,
			Set<MultiKey> keySet);

	/**
	 * Aggregate an array of stats
	 * 
	 * @param stats
	 * @return an Aggregator containing the aggregated statistic
	 * @throws NullPointerException
	 *             if input array is null
	 * @throws IllegalArgumentException
	 *             if array has size 0
	 */
	public static AggregableStats aggregate(final AggregableStats stats[])
			throws IllegalArgumentException, NullPointerException {
		if (stats == null)
			throw new NullPointerException("The passed stats array is null");
		final int size = stats.length;
		if (size < 1) {
			throw new IllegalArgumentException(
					"The array stats size to aggregate is empty");
		}
		if (size == 1) {
			return stats[1];
		}

		AggregableStats ret = aggregate(stats[0], stats[1]);
		for (int i = 2; i < stats.length; i++) {
			ret = aggregate(ret, stats[i]);
		}
		return ret;
	}

	protected static AggregableStats buildAggregableStats(
			final List<Result> results) {
		final AggregableStats stat = new AggregableStats();

		if (results.size() > 0)
			stat.setSize(results.get(0).getNumAccepted());
		else
			throw new IllegalArgumentException(
					"The passed results list is empty");

		for (Result res : results) {
			switch (res.getStatistic()) {
			case SUM:
				stat.setSum(res.getValue());
				break;
			case MEAN:
				stat.setMean(res.getValue());
				break;
			case MAX:
				stat.setMax(res.getValue());
				break;
			case MIN:
				stat.setMin(res.getValue());
				break;
			case VARIANCE:
				stat.setVariance(res.getValue());
				break;
			default:
				break;
			}
		}
		return stat;
	}

	public static AggregableStats aggregate(final AggregableStats statA,
			final AggregableStats statB) {
		if (statA == null || statB == null) {
			return null;
		}

		double min = statA.getMin();

		double max = statA.getMax();

		if (statB.getMin() < min || Double.isNaN(min)) {
			min = statB.getMin();
		}
		if (statB.getMax() > max || Double.isNaN(max)) {
			max = statB.getMax();
		}

		// sum
		final double sumA = statA.getSum();
		final double sumB = statB.getSum();
		final double sum;
		if (!Double.isNaN(sumA)) {
			if (!Double.isNaN(sumB))
				sum = sumA + sumB;
			else
				sum = sumA;
		} else {
			if (!Double.isNaN(sumB))
				sum = sumB;
			else
				sum = Double.NaN;
		}

		// size
		final long bSize = statB.getSize();
		final long aSize = statA.getSize();
		final long size = aSize + bSize;

		double mean = statA.getMean();
		if (Double.isNaN(mean)) {
			mean = 0;
		}

		final double varianceB = statB.getVariance();
		final double varianceA = statA.getVariance();

		final double variance;
		if (size == 0) {
			variance = Double.NaN;
		} else if (size == 1) {
			variance = 0d;
		} else {
			final double secondMoment = varianceB * (bSize - 1);
			final double meanDiff = statB.getMean() - mean;

			double m2 = (!Double.isNaN(varianceA)) ? varianceA * (size - 1) : 0;// sm.getResult();

			m2 = m2 + secondMoment + meanDiff * meanDiff * aSize * bSize / size;
			variance = m2 / (size - 1);

		}

		mean = sum / size;

		return new AggregableStats(mean, variance, size, max, min, sum);
	}

}