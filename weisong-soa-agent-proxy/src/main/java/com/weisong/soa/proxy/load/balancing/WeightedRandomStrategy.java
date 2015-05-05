package com.weisong.soa.proxy.load.balancing;

import java.util.Random;


public class WeightedRandomStrategy implements LoadBalancingStrategy {

	final static private int SIZE = 10000;
	
	private int index;
	private int[] slots = new int[SIZE];
	
	public WeightedRandomStrategy(float[] weights) {
		float[][] ranges = new float[weights.length][2];
		float total = 0f;
		for(int i = 0; i < weights.length; i++) {
			ranges[i][0] = total;
			total += weights[i];
			ranges[i][1] = total - Float.MIN_VALUE;
		}
		
		float f = 0f;
		float step = total / SIZE;
		for(int i = 0; i < slots.length; i++) {
			for(int r = 0; r < ranges.length; r++) {
				if(ranges[r][0] <= f && f < ranges[r][1]) {
					slots[i] = r;
				}
			}
			f += step;
		}

		// Shuffle multiple times
		randomize(slots);
		randomize(slots);
		randomize(slots);
	}
	
	private void randomize(int a[]) {
		Random random = new Random();
		for (int i = 0; i < a.length; i++) {
			// Pick a random index
			int j = random.nextInt(SIZE) % a.length;
			// Swap
			int temp = a[i];
			a[i] = a[j];
			a[j] = temp;
		}
	}

	@Override
	synchronized public int next() {
		index = ++index % slots.length;
		return slots[index];
	}

}
