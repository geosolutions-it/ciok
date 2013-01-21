package it.geosolutions.geobatch.gaez.utils.rules.ruleB;


import it.geosolutions.geobatch.gaez.utils.aggregator.AggregableStats;
import it.geosolutions.geobatch.gaez.utils.aggregator.LongAggregator;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.keyvalue.MultiKey;
import org.jaitools.media.jai.classifiedstats.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RuleBLongAggregator extends LongAggregator {
	private final static Logger LOGGER = LoggerFactory.getLogger(RuleBLongAggregator.class);
	
	Map<MultiKey, Double> areaSum;
	Set<MultiKey> keySet;
	final long size;
	public RuleBLongAggregator(List<Map<MultiKey, List<Result>>> areaSubResults) {
		super();
		
		size=areaSubResults.size();
		
		keySet=new HashSet<MultiKey>();
		
		// First scan to initialize all the keys
		for (int j = 0; j < size; j++) {
			Map<MultiKey, List<Result>> res = areaSubResults.get(j);
			Set<MultiKey> keys = res.keySet();
			final Iterator<MultiKey> iteratorKey = keys.iterator();
			while (iteratorKey.hasNext()) {
				MultiKey key = iteratorKey.next();
				if (!keySet.contains(key)) {
					keySet.add(key);
				}
				
			}
		}

		// calculate area sum once
		areaSum=new HashMap<MultiKey, Double>();
		final Iterator<MultiKey> iteratorKey = keySet.iterator();
		while (iteratorKey.hasNext()) {
			double area = 0;
			MultiKey key = iteratorKey.next();
			for (int j = 0; j < size; j++) {
				Map<MultiKey, List<Result>> areaRes = areaSubResults.get(j);
				List<Result> areaResult = areaRes.get(key);
			
				if (areaResult != null && !areaResult.isEmpty()) {
					Result ar = areaResult.get(0);
					area += ar.getValue();
				}
			}
			areaSum.put(key, area);
		}
	}

	@Override
	public Map<MultiKey, AggregableStats> aggregate(
		List<Map<MultiKey, List<Result>>> splittedResults, Set<MultiKey> keySet) {
		Map<MultiKey, AggregableStats> ret;
		if (keySet==null)
			ret=super.aggregate(splittedResults, this.keySet);
		else
			ret=super.aggregate(splittedResults, keySet);
		
		postAggregate(ret);
		return ret;
	}
	
	public void postAggregate(Map<MultiKey, AggregableStats> aggregatedStats) {
		
		final Iterator<MultiKey> iteratorKey = keySet.iterator();
		while (iteratorKey.hasNext()) {

			MultiKey key = iteratorKey.next();
			
			final AggregableStats stat=aggregatedStats.get(key);
			
			final double area = areaSum.get(key);
			
			// Does this --> (X/AREA) make sense???
			stat.setVariance(stat.getVariance()/Math.pow(area,2));
			stat.setMax(stat.getMax()/area);
			stat.setMin(stat.getMin()/area);
			stat.setMean(stat.getMean()/area);
			stat.setSum(stat.getSum()/area);
			// -------------------------------


			aggregatedStats.put(key, stat);
			
			if (LOGGER.isDebugEnabled())
				LOGGER.debug("postprocessed #");

		}
	}


}
