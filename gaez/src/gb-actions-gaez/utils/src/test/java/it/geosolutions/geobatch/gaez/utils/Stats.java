package it.geosolutions.geobatch.gaez.utils;

import static org.junit.Assert.*;
import it.geosolutions.geobatch.gaez.utils.aggregator.AggregableStats;
import it.geosolutions.geobatch.gaez.utils.aggregator.Aggregator;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.math.stat.descriptive.AggregateSummaryStatistics;
import org.apache.commons.math.stat.descriptive.StatisticalSummaryValues;
import org.apache.commons.math.stat.descriptive.SummaryStatistics;
import org.apache.commons.math.stat.descriptive.SynchronizedSummaryStatistics;
import org.apache.commons.math.stat.descriptive.moment.StandardDeviation;
import org.junit.Assert;
import org.junit.Test;

public class Stats {
	
	@Test
	public void test(){
		SynchronizedSummaryStatistics s=new SynchronizedSummaryStatistics();
//		SynchronizedDescriptiveStatistics sds=new SynchronizedDescriptiveStatistics();
//		sds.
		s.addValue(1.0d);
		s.addValue(1.0d);
//		s.addValue(1.1d);
		s.addValue(2.0d);
		
		double[] d=new double[]{1.0d, 1.0d, 2.0d};
		double[] d2=new double[]{9.0d, 1.0d, 5.0d};
		double[] d3=new double[]{1.0d, 1.0d, 2.0d, 9.0d, 1.0d, 5.0d};
		double[] d4=new double[]{11.0d, 13.0d, 2.0d, 19.0d, 10.0d, 7.0d};
		StandardDeviation sd=new StandardDeviation();
		sd.setData(d);
		System.out.println("sd1: "+(sd.evaluate()));
		sd.setData(d2);
		System.out.println("sd2: "+(sd.evaluate()));
		sd.setData(d3);
		System.out.println("sd3: "+(sd.evaluate()));
		sd.setData(d4);
		System.out.println("sd4: "+(sd.evaluate()));
		
		SummaryStatistics ss=new SummaryStatistics();
		for (double value : d)
			ss.addValue(value);
		AggregableStats a=new AggregableStats(ss.getMean(), ss.getVariance(), ss.getN(), ss.getMax(),ss.getMin(),ss.getSum());
		
		SummaryStatistics ss2=new SummaryStatistics();
		for (double value : d2)
			ss2.addValue(value);
		AggregableStats b=new AggregableStats(ss2.getMean(), ss2.getVariance(), ss2.getN(), ss2.getMax(),ss2.getMin(),ss2.getSum());
		
		SummaryStatistics ss3=new SummaryStatistics();
		for (double value : d4)
			ss3.addValue(value);
		AggregableStats c=new AggregableStats(ss3.getMean(), ss3.getVariance(), ss3.getN(), ss3.getMax(), ss3.getMin(), ss3.getSum());
		
		
		
		System.out.println("sdA: "+(Math.sqrt(a.getVariance())));
		System.out.println("varianceA: "+(a.getVariance()));
		
		System.out.println("sdB: "+(Math.sqrt(b.getVariance())));
		System.out.println("varianceB: "+(b.getVariance()));
		
//		sdc.add();
		AggregableStats aggrC=Aggregator.aggregate(new AggregableStats[]{a, b, c});
		System.out.println("sdC: "+(Math.sqrt(aggrC.getVariance())));
		System.out.println("varianceC: "+(aggrC.getVariance()));
		
		Collection<SummaryStatistics> sdc=new ArrayList<SummaryStatistics>(2);
		sdc.add(ss);
		sdc.add(ss2);
		sdc.add(ss3);
		
//		sdc.add();
		StatisticalSummaryValues summary;
		synchronized (sdc) {
			summary=AggregateSummaryStatistics.aggregate(sdc);	
		}
		System.out.println("sdAggr(C): "+(summary.getStandardDeviation()));
		System.out.println("sdAggrPow(C): "+(Math.pow(summary.getStandardDeviation(),2)));
		System.out.println("varianceAggr(C): "+(summary.getVariance()));

//		Assert.assertEquals(summary.getVariance(), aggrC.getVariance());
		//Assert.assertEquals(Math.sqrt(summary.getStandardDeviation()), aggrC.getVariance(),10^-10000);
//		final Mean mx=new Mean();
//		mx.setData(d);
//		final double dmx=mx.evaluate();
//		final long sizex=d.length;
//		
//		final Mean my=new Mean();
//		my.setData(d2);
//		final double dmy=my.evaluate();
//		final long sizey=d2.length;
//		
//		
//		Variance v=new Variance();
//	
//		final double sigma=dmy-dmx;
////		final double nmx=dmx+sigma*sizex/(sizex+sizey);
//		
////		final double sdc=Math.sqrt(v.evaluate(d,dmx))+Math.sqrt(v.evaluate(d2,dmy))+Math.pow(sigma, 2)*sizex*sizey/(sizex+sizey);
//		
//		System.out.println("sdC: "+(sdc));
//		
//		
//		FirstMoment fm=new FirstMoment();
//		fm.incrementAll(d);
//		System.out.println("FirstMoment: "+fm.getResult());
//		
//		SecondMoment sm=new SecondMoment();
//		sm.incrementAll(d);
//		sm.setData(d);
//		System.out.println("SecondMoment: "+sm.evaluate());
//		System.out.println("SecondMomentSqrt: "+Math.sqrt(sm.evaluate()));
//		
//		ThirdMoment tm=new ThirdMoment();
//		tm.incrementAll(d);
//		System.out.println("ThirdMoment: "+sm.getResult());
		
//		org.apache.commons.math.stat.descriptive.SummaryStatistics
	}

}
