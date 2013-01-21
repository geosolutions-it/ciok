package it.geosolutions.geobatch.gaez.utils.aggregator;


import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.keyvalue.MultiKey;
import org.jaitools.media.jai.classifiedstats.Result;
import org.jaitools.numeric.Statistic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ShortAggregator extends Aggregator {
	private final static Logger LOGGER = LoggerFactory.getLogger(ShortAggregator.class);

	public ShortAggregator() {
	}
	
	
	@Override
	public Map<MultiKey, AggregableStats> aggregate(
			List<Map<MultiKey, List<Result>>> splittedResults, Set<MultiKey> keySet) {
		
		final long size=splittedResults.size();

		Map<MultiKey, AggregableStats> output = new HashMap<MultiKey, AggregableStats>();

		if (keySet==null){
			keySet=new HashSet<MultiKey>();
		// First scan to initialize all the keys
			for (int j = 0; j < size; j++) {
				Map<MultiKey, List<Result>> res = splittedResults.get(j);
				Set<MultiKey> keys = res.keySet();
				final Iterator<MultiKey> iteratorKey = keys.iterator();
				while (iteratorKey.hasNext()) {
					MultiKey key = iteratorKey.next();
					if (!keySet.contains(key)) {
						keySet.add(key);
					}
				}
			}
		}
		
		final Iterator<MultiKey> iteratorKey = keySet.iterator();
		while (iteratorKey.hasNext()) {

			MultiKey key = iteratorKey.next();
			AggregableStats stat = new AggregableStats();
			for (int j = 0; j < size; j++) {
				Map<MultiKey, List<Result>> res = splittedResults.get(j);
				List<Result> results = res.get(key);
				if (results != null && !results.isEmpty()) {
					
					stat=Aggregator.aggregate(stat, buildAggregableStats(results));
					
				}
			}
			
//			stat.setSize(stat.getSize());
//			stat.setMax(stat.getMax());
//			stat.setMin(stat.getMin());
//			stat.setSum(stat.getSum());

			output.put(key, stat);
			
			if (LOGGER.isDebugEnabled())
				LOGGER.debug("Merged #");

		}
		return output;
	}


}
