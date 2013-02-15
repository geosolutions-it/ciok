package it.geosolutions.geobatch.gaez.utils.aggregator;

public class AggregableStats {
	double min;
	double max;
	long size;
	double variance;
	double mean;
	double sum;
	
	public double getMin() {
		return min;
	}

	public void setMin(double min) {
		this.min = min;
	}

	public double getMax() {
		return max;
	}

	public void setMax(double max) {
		this.max = max;
	}

	public long getSize() {
		return size;
	}

	public void setSize(long  size) {
		this.size = size;
	}

	public double getVariance() {
		return variance;
	}

	public void setVariance(double variance) {
		this.variance = variance;
	}

	public double getMean() {
		return mean;
	}

	public void setMean(double mean) {
		this.mean = mean;
	}
	
	public double getSum() {
		return sum;
	}

	public void setSum(double sum) {
		this.sum = sum;
	}
	
	public AggregableStats(){
		this.mean=Double.NaN;
		this.variance=Double.NaN;
		this.size=0;
		this.min=Double.NaN;
		this.max=Double.NaN;
		this.sum=Double.NaN;
//		this.mean=0;
//		this.variance=0;
//		this.size=0;
//		this.min=Double.POSITIVE_INFINITY;
//		this.max=Double.NEGATIVE_INFINITY;
//		this.sum=0;
	}
	
	public AggregableStats(final double mean, final double variance, final long size, final double max, final double min, final double sum){
		this.mean=mean;
		this.variance=variance;
		this.size=size;
		this.max=max;
		this.min=min;
		this.sum=sum;
	}
	

}