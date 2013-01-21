package it.geosolutions.geobatch.gaez.utils.rules;

import it.geosolutions.geobatch.gaez.utils.aggregator.AggregableStats;

import org.apache.commons.collections.keyvalue.MultiKey;

public interface LineWriter {

	public String writeLine(MultiKey key, AggregableStats results);
	
	public String writeHeader();
}
