package com.weisong.soa.proxy.load.balancing;

import java.util.Arrays;

public class WeightedRoundRobinStragegy implements LoadBalancingStrategy {

	static private class Ratio implements Comparable<Ratio> {
		private float ratio;
		private int index;
		private Ratio(float ratio, int index) {
			this.ratio = ratio;
			this.index = index;
		}
		@Override
		public int compareTo(Ratio o) {
			return ratio < o.ratio ? -1 :
				ratio > o.ratio ? 1 :
					0;
		}
		@Override
		public String toString() {
			return String.format("[%f, %d]", ratio, index);
		}
	}
	
	final static public int maxSamples = 10000; 
	
	private int index, level;
	private Ratio[] ratios;
	private float[] values;
	
	public WeightedRoundRobinStragegy(float[] weights) {
		index = weights.length - 1;
		values = new float[weights.length];
		float max = Float.MIN_VALUE;
		for(int i = 0; i < weights.length; i++) {
			max = Math.max(max, 1f / weights[i]);
		}
		
		ratios = new Ratio[weights.length];
		for(int i = 0; i < weights.length; i++) {
			ratios[i] = new Ratio(1f / weights[i] / max, i);
		}
		Arrays.sort(ratios);
	}
	
	private void clearValues() {
		for(int i = 0; i < values.length; i++) {
			values[i] = 0f;
		}
	}

	@Override
	synchronized public int next() {
		while(true) {
			if(index == values.length - 1) { // the first one
				if(level > maxSamples) {
					level = 0;
					clearValues();
				}
				level += ratios[index].ratio;
				values[index] += ratios[index].ratio;
				index--;
				return ratios[ratios.length - 1].index;
			}
			else {
				while(index >= 0 && values[index] + ratios[index].ratio > level) {
					--index;
				}
				if(index < 0) {
					index = values.length - 1;
					continue;
				}
				values[index] += ratios[index].ratio;
				return ratios[index].index;
 			}
		}
	}
	
}
