package it.geosolutions.geobatch.gaez.utils.aggregator;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.keyvalue.MultiKey;
import org.jaitools.media.jai.classifiedstats.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LongAggregator extends Aggregator {
	private final static Logger LOGGER = LoggerFactory
			.getLogger(LongAggregator.class);

	public LongAggregator() {
		// gaulSubSize
	}

	/**
    *
    */
	@Override
	public Map<MultiKey, AggregableStats> aggregate(
			List<Map<MultiKey, List<Result>>> splittedResults,
			Set<MultiKey> keySet) {

		int numSplits = splittedResults.size();

		if (keySet == null) {
			// First scan to initialize all the keys
			keySet = new HashSet<MultiKey>();
			for (int split = 0; split < numSplits; split++) {
				Map<MultiKey, List<Result>> resultsInSplit = splittedResults
						.get(split);
				Set<MultiKey> keysInSplit = resultsInSplit.keySet();
				keySet.addAll(keysInSplit);
			}
		}

		if (LOGGER.isDebugEnabled())
			LOGGER.debug("Found " + keySet.size() + " multikeys");

		Map<MultiKey, AggregableStats> output = new HashMap<MultiKey, AggregableStats>();

		for (MultiKey key : keySet) {
			AggregableStats aggstats = new AggregableStats();

			for (int split = 0; split < numSplits; split++) {
				Map<MultiKey, List<Result>> res = splittedResults.get(split);
				// Map<MultiKey, List<Result>> areaRes = areaSubResults.get(j);
				List<Result> results = res.get(key);
				if (results != null) {
					aggstats = Aggregator.aggregate(aggstats,
							buildAggregableStats(results));
				}
			}

			// AggregableStats finalStats = Aggregator.aggregate(aggstats);

			output.put(key, aggstats);

			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Merged #");
			}
		}

		return output;
	}

}
